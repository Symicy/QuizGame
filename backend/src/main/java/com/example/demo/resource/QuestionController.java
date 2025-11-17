package com.example.demo.resource;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.common.PagedResponse;
import com.example.demo.dto.question.OpenTriviaCategoryResponse;
import com.example.demo.dto.question.OpenTriviaTokenResetRequest;
import com.example.demo.dto.question.QuestionImportRequest;
import com.example.demo.dto.question.QuestionRequest;
import com.example.demo.dto.question.QuestionResponse;
import com.example.demo.enums.DifficultyLevel;
import com.example.demo.enums.SourceType;
import com.example.demo.service.QuestionImportService;
import com.example.demo.service.QuestionService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/questions")
@Validated
@RequiredArgsConstructor
public class QuestionController {

	private final QuestionService questionService;
	private final QuestionImportService questionImportService;

	@GetMapping
	public ResponseEntity<List<QuestionResponse>> getQuestions(
			@RequestParam(name = "categoryId", required = false) Long categoryId,
			@RequestParam(name = "difficulty", required = false) DifficultyLevel difficultyLevel,
			@RequestParam(name = "sourceType", required = false) SourceType sourceType,
			@RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly,
			@RequestParam(name = "random", defaultValue = "false") boolean random,
			@RequestParam(name = "count", defaultValue = "10") @Min(1) @Max(50) int count) {

		if (random) {
			return ResponseEntity.ok(
					questionService.getRandomQuestions(categoryId, difficultyLevel, count));
		}

		return ResponseEntity.ok(questionService.getQuestions(categoryId, difficultyLevel, sourceType, activeOnly));
	}

	@GetMapping("/search")
	public ResponseEntity<PagedResponse<QuestionResponse>> searchQuestions(
			@RequestParam(name = "categoryId", required = false) Long categoryId,
			@RequestParam(name = "difficulty", required = false) DifficultyLevel difficultyLevel,
			@RequestParam(name = "sourceType", required = false) SourceType sourceType,
			@RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly,
			@RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
			@RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
		return ResponseEntity.ok(
				questionService.getQuestionsPage(categoryId, difficultyLevel, sourceType, activeOnly, page, size));
	}

	@GetMapping("/{id}")
	public ResponseEntity<QuestionResponse> getQuestion(@PathVariable Long id) {
		return ResponseEntity.ok(questionService.getQuestion(id));
	}

	@PostMapping
	public ResponseEntity<QuestionResponse> createQuestion(@Valid @RequestBody QuestionRequest request) {
		return ResponseEntity.ok(questionService.createQuestion(request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<QuestionResponse> updateQuestion(
			@PathVariable Long id,
			@Valid @RequestBody QuestionRequest request) {
		return ResponseEntity.ok(questionService.updateQuestion(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
		questionService.deleteQuestion(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/import/opentdb")
	public ResponseEntity<List<QuestionResponse>> importFromOpenTrivia(
			@Valid @RequestBody QuestionImportRequest request) {
		return ResponseEntity.ok(questionImportService.importFromOpenTrivia(request));
	}

	@GetMapping("/opentdb/categories")
	public ResponseEntity<List<OpenTriviaCategoryResponse>> getOpenTriviaCategories() {
		return ResponseEntity.ok(questionImportService.getExternalCategories());
	}

	@GetMapping("/opentdb/token")
	public ResponseEntity<String> getStoredOpenTriviaToken() {
		return questionImportService.getStoredSessionToken()
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@PostMapping("/opentdb/token/request")
	public ResponseEntity<String> requestOpenTriviaToken() {
		return ResponseEntity.ok(questionImportService.requestSessionToken());
	}

	@PostMapping("/opentdb/token/reset")
	public ResponseEntity<String> resetOpenTriviaToken(
			@Valid @RequestBody OpenTriviaTokenResetRequest request) {
		return ResponseEntity.ok(questionImportService.resetSessionToken(request.getToken()));
	}
}
