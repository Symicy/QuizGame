package com.example.demo.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.demo.domain.Category;
import com.example.demo.domain.Question;
import com.example.demo.dto.question.OpenTriviaCategoryResponse;
import com.example.demo.dto.question.QuestionImportRequest;
import com.example.demo.dto.question.QuestionResponse;
import com.example.demo.enums.DifficultyLevel;
import com.example.demo.enums.OpenTriviaEncoding;
import com.example.demo.enums.SourceType;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.external.opentrivia.OpenTriviaApiClient;
import com.example.demo.external.opentrivia.OpenTriviaApiClient.OpenTriviaQuestion;
import com.example.demo.repo.CategoryRepo;
import com.example.demo.repo.QuestionRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionImportService {

    private final OpenTriviaApiClient openTriviaApiClient;
    private final CategoryRepo categoryRepo;
    private final QuestionRepo questionRepo;
    private final OpenTriviaTokenService openTriviaTokenService;

    @Transactional(readOnly = true)
    public List<OpenTriviaCategoryResponse> getExternalCategories() {
        return openTriviaApiClient.fetchCategories();
    }

    @Transactional(readOnly = true)
    public Optional<String> getStoredSessionToken() {
        return openTriviaTokenService.getToken();
    }

    public String requestSessionToken() {
        String token = openTriviaApiClient.requestSessionToken();
        return openTriviaTokenService.storeToken(token);
    }

    public String resetSessionToken(String token) {
        String tokenToReset = StringUtils.hasText(token)
                ? token.trim()
                : openTriviaTokenService.getToken()
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Nu există un token salvat. Generează unul nou înainte de reset."));

        String resetToken = openTriviaApiClient.resetSessionToken(tokenToReset);
        return openTriviaTokenService.storeToken(resetToken);
    }

    public List<QuestionResponse> importFromOpenTrivia(QuestionImportRequest request) {
        Category category = resolveTargetCategory(request);

        OpenTriviaEncoding encoding = request.getEncoding() == null
            ? OpenTriviaEncoding.DEFAULT
            : request.getEncoding();

        String providedToken = request.getSessionToken();
        String effectiveToken = StringUtils.hasText(providedToken)
            ? providedToken.trim()
            : openTriviaTokenService.getToken().orElse(null);

        List<OpenTriviaQuestion> externalQuestions = openTriviaApiClient.fetchQuestions(
            request.getAmount(),
            request.getExternalCategoryId(),
            request.getDifficultyLevel(),
            encoding,
            effectiveToken);

        if (externalQuestions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Question> mappedQuestions = externalQuestions.stream()
            .map(question -> mapQuestion(question, category, encoding))
                .filter(Objects::nonNull)
                .toList();

        if (mappedQuestions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Question> saved = questionRepo.saveAll(mappedQuestions);
        return saved.stream()
                .map(QuestionResponse::fromEntity)
                .toList();
    }

    private Category resolveTargetCategory(QuestionImportRequest request) {
        Long categoryId = request.getCategoryId();
        if (categoryId != null) {
            return categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
        }

        if (request.getExternalCategoryId() == null) {
            throw new IllegalArgumentException("Selectează o categorie internă sau una OpenTrivia.");
        }

        List<OpenTriviaCategoryResponse> externalCategories = openTriviaApiClient.fetchCategories();
        OpenTriviaCategoryResponse targetCategory = externalCategories.stream()
                .filter(cat -> cat.id() == request.getExternalCategoryId())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Categoria OpenTrivia selectată nu mai există."));

        return categoryRepo.findByNameIgnoreCase(targetCategory.name())
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setName(targetCategory.name());
                    category.setDescription("OpenTrivia ID " + targetCategory.id());
                    category.setIsActive(true);
                    return categoryRepo.save(category);
                });
    }

    private Question mapQuestion(OpenTriviaQuestion externalQuestion, Category category, OpenTriviaEncoding encoding) {
        List<String> options = buildOptions(externalQuestion, encoding);
        if (options.size() != 4) {
            return null;
        }

        String correctAnswer = clean(externalQuestion.correctAnswer(), encoding);
        int correctIndex = options.indexOf(correctAnswer);
        if (correctIndex < 0) {
            return null;
        }

        DifficultyLevel difficulty = mapDifficulty(externalQuestion.difficulty());

        Question question = new Question();
        question.setCategory(category);
        question.setText(clean(externalQuestion.question(), encoding));
        question.setOptionA(options.get(0));
        question.setOptionB(options.get(1));
        question.setOptionC(options.get(2));
        question.setOptionD(options.get(3));
        question.setCorrectOption(correctIndex);
        question.setDifficultyLevel(difficulty);
        question.setSourceType(SourceType.API_IMPORT);
        question.setPoints(resolvePoints(difficulty));
        question.setIsActive(true);
        return question;
    }

    private List<String> buildOptions(OpenTriviaQuestion externalQuestion, OpenTriviaEncoding encoding) {
        List<String> answers = new ArrayList<>();
        answers.add(clean(externalQuestion.correctAnswer(), encoding));
        if (externalQuestion.incorrectAnswers() != null) {
            externalQuestion.incorrectAnswers().stream()
                    .map(answer -> clean(answer, encoding))
                    .forEach(answers::add);
        }
        Collections.shuffle(answers);
        return answers;
    }

    private DifficultyLevel mapDifficulty(String difficulty) {
        if (difficulty == null) {
            return DifficultyLevel.MEDIUM;
        }
        return switch (difficulty.toUpperCase()) {
            case "EASY" -> DifficultyLevel.EASY;
            case "HARD" -> DifficultyLevel.HARD;
            default -> DifficultyLevel.MEDIUM;
        };
    }

    private int resolvePoints(DifficultyLevel difficultyLevel) {
        return switch (difficultyLevel) {
            case EASY -> 10;
            case MEDIUM -> 20;
            case HARD -> 30;
        };
    }

    private String clean(String value, OpenTriviaEncoding encoding) {
        OpenTriviaEncoding activeEncoding = encoding == null ? OpenTriviaEncoding.DEFAULT : encoding;
        return activeEncoding.decode(value);
    }
}
