package com.example.demo.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.Category;
import com.example.demo.domain.Question;
import com.example.demo.dto.common.PagedResponse;
import com.example.demo.dto.question.QuestionRequest;
import com.example.demo.dto.question.QuestionResponse;
import com.example.demo.enums.DifficultyLevel;
import com.example.demo.enums.SourceType;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repo.CategoryRepo;
import com.example.demo.repo.QuestionRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class QuestionService {

	private final QuestionRepo questionRepo;
	private final CategoryRepo categoryRepo;

	@Transactional(readOnly = true)
	public QuestionResponse getQuestion(Long id) {
		return QuestionResponse.fromEntity(getQuestionEntity(id));
	}

	@Transactional(readOnly = true)
	public List<QuestionResponse> getQuestions(
			Long categoryId,
			DifficultyLevel difficultyLevel,
			SourceType sourceType,
			boolean activeOnly) {
		Page<Question> questions = questionRepo.findByFilters(categoryId, difficultyLevel, sourceType, activeOnly,
				Pageable.unpaged());
		return questions.getContent().stream()
				.map(QuestionResponse::fromEntity)
				.toList();
	}

	@Transactional(readOnly = true)
	public PagedResponse<QuestionResponse> getQuestionsPage(
			Long categoryId,
			DifficultyLevel difficultyLevel,
			SourceType sourceType,
			boolean activeOnly,
			int page,
			int size) {
		Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<QuestionResponse> responsePage = questionRepo
				.findByFilters(categoryId, difficultyLevel, sourceType, activeOnly, pageable)
				.map(QuestionResponse::fromEntity);
		return PagedResponse.fromPage(responsePage);
	}

	public QuestionResponse createQuestion(QuestionRequest request) {
		Category category = getCategory(request.getCategoryId());

		Question question = new Question();
		mapRequestToEntity(request, question);
		question.setCategory(category);

		Question saved = questionRepo.save(question);
		return QuestionResponse.fromEntity(saved);
	}

	public QuestionResponse updateQuestion(Long id, QuestionRequest request) {
		Question question = getQuestionEntity(id);

		if (!question.getCategory().getId().equals(request.getCategoryId())) {
			    question.setCategory(getCategory(request.getCategoryId()));
		}

		mapRequestToEntity(request, question);
		return QuestionResponse.fromEntity(question);
	}

	public void deleteQuestion(Long id) {
		Question question = getQuestionEntity(id);
		question.setIsActive(false);
	}

	@Transactional(readOnly = true)
	public List<QuestionResponse> getRandomQuestions(Long categoryId, DifficultyLevel difficultyLevel, int count) {
		if (count <= 0) {
			return List.of();
		}
		Pageable pageable = PageRequest.of(0, count);
		List<Question> randomQuestions = questionRepo.findRandomActiveQuestions(categoryId, difficultyLevel, pageable);
		return randomQuestions.stream()
				.map(QuestionResponse::fromEntity)
				.toList();
	}

	private void mapRequestToEntity(QuestionRequest request, Question question) {
		question.setText(request.getText());
		question.setOptionA(request.getOptionA());
		question.setOptionB(request.getOptionB());
		question.setOptionC(request.getOptionC());
		question.setOptionD(request.getOptionD());
		question.setCorrectOption(request.getCorrectOption());
		question.setDifficultyLevel(request.getDifficultyLevel());
		question.setSourceType(request.getSourceType() != null ? request.getSourceType() : SourceType.ADMIN);
		question.setExplanation(request.getExplanation());
		question.setPoints(resolvePoints(request.getDifficultyLevel(), request.getPoints()));
		if (request.getIsActive() != null) {
			question.setIsActive(request.getIsActive());
		}
	}

	private int resolvePoints(DifficultyLevel difficultyLevel, Integer customPoints) {
		if (customPoints != null && customPoints > 0) {
			return customPoints;
		}

		return switch (difficultyLevel) {
			case EASY -> 10;
			case MEDIUM -> 20;
			case HARD -> 30;
		};
	}

	private Category getCategory(Long categoryId) {
		if (categoryId == null) {
			throw new IllegalArgumentException("Category id is required");
		}
		return categoryRepo.findById(categoryId)
				.orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
	}

	public Question getQuestionEntity(Long id) {
		if (id == null) {
			throw new IllegalArgumentException("Question id is required");
		}
		return questionRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Question", id));
	}
}
