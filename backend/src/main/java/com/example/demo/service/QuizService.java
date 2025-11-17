package com.example.demo.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.Category;
import com.example.demo.domain.Question;
import com.example.demo.domain.Quiz;
import com.example.demo.domain.User;
import com.example.demo.dto.quiz.QuizRequest;
import com.example.demo.dto.quiz.QuizResponse;
import com.example.demo.enums.DifficultyLevel;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repo.CategoryRepo;
import com.example.demo.repo.QuestionRepo;
import com.example.demo.repo.QuizRepo;
import com.example.demo.repo.UserRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizService {

	private final QuizRepo quizRepo;
	private final CategoryRepo categoryRepo;
	private final QuestionRepo questionRepo;
	private final UserRepo userRepo;

	@Transactional(readOnly = true)
	public List<QuizResponse> getQuizzes(Long categoryId, DifficultyLevel difficultyLevel, Boolean activeOnly) {
		List<Quiz> quizzes;

		if (categoryId != null && difficultyLevel != null) {
			quizzes = quizRepo.findByCategoryIdAndDifficulty(categoryId, difficultyLevel);
		} else if (categoryId != null) {
			quizzes = quizRepo.findByCategoryId(categoryId);
		} else if (difficultyLevel != null) {
			quizzes = quizRepo.findByDifficulty(difficultyLevel);
		} else {
			quizzes = quizRepo.findAll();
		}

		if (Boolean.TRUE.equals(activeOnly)) {
			quizzes = quizzes.stream()
					.filter(Quiz::getIsActive)
					.toList();
		}

		return quizzes.stream()
				.map(QuizResponse::fromEntity)
				.toList();
	}

	@Transactional(readOnly = true)
	public QuizResponse getQuiz(Long id) {
		return QuizResponse.fromEntity(getQuizEntity(id));
	}

	public QuizResponse createQuiz(QuizRequest request) {
		Quiz quiz = new Quiz();
		applyRequestToQuiz(quiz, request);
		Quiz saved = quizRepo.save(quiz);
		return QuizResponse.fromEntity(saved);
	}

	public QuizResponse updateQuiz(Long id, QuizRequest request) {
		Quiz quiz = getQuizEntity(id);
		applyRequestToQuiz(quiz, request);
		return QuizResponse.fromEntity(quiz);
	}

	public void deleteQuiz(Long id) {
		Quiz quiz = getQuizEntity(id);
		quiz.setIsActive(false);
	}

	public Quiz getQuizEntity(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Quiz id is required");
		}
		return quizRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Quiz", id));
	}

	private void applyRequestToQuiz(Quiz quiz, QuizRequest request) {
		Long categoryId = request.getCategoryId();
		Long creatorId = request.getCreatedBy();

		if (categoryId == null) {
		    throw new IllegalArgumentException("Category id is required");
		}
		if (creatorId == null) {
		    throw new IllegalArgumentException("Creator id is required");
		}

		Category category = categoryRepo.findById(categoryId)
			.orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
		User creator = userRepo.findById(creatorId)
			.orElseThrow(() -> new ResourceNotFoundException("User", creatorId));

		quiz.setTitle(request.getTitle());
		quiz.setDescription(request.getDescription());
		quiz.setDifficulty(request.getDifficulty());
		quiz.setCategory(category);
		quiz.setCreatedBy(creator);
		quiz.setTimeLimitSeconds(request.getTimeLimitSeconds());
		quiz.setIsActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE);

		List<Question> questions = resolveQuestions(request.getQuestionIds());
		if (quiz.getQuestions() == null) {
			quiz.setQuestions(new ArrayList<>());
		}
		quiz.getQuestions().clear();
		quiz.getQuestions().addAll(questions);

		int totalQuestions = request.getTotalQuestions() != null
				? request.getTotalQuestions()
				: questions.size();

		if (totalQuestions <= 0) {
			throw new IllegalArgumentException("Quiz must contain at least one question");
		}
		if (!questions.isEmpty() && totalQuestions != questions.size()) {
			throw new IllegalArgumentException("totalQuestions must match the number of provided questions");
		}

		quiz.setTotalQuestions(totalQuestions);
	}

	private List<Question> resolveQuestions(List<Long> questionIds) {
		if (questionIds == null || questionIds.isEmpty()) {
			return Collections.emptyList();
		}

		List<Question> questions = questionRepo.findAllById(questionIds);
		if (questions.size() != questionIds.size()) {
			throw new ResourceNotFoundException("One or more questions were not found");
		}
		return questions;
	}
}
