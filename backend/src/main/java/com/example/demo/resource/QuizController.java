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

import com.example.demo.dto.quiz.QuizRequest;
import com.example.demo.dto.quiz.QuizResponse;
import com.example.demo.enums.DifficultyLevel;
import com.example.demo.service.QuizService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/quizzes")
@Validated
@RequiredArgsConstructor
public class QuizController {

	private final QuizService quizService;

	@GetMapping
	public ResponseEntity<List<QuizResponse>> getQuizzes(
			@RequestParam(name = "categoryId", required = false) Long categoryId,
			@RequestParam(name = "difficulty", required = false) DifficultyLevel difficultyLevel,
			@RequestParam(name = "activeOnly", required = false) Boolean activeOnly) {
		return ResponseEntity.ok(quizService.getQuizzes(categoryId, difficultyLevel, activeOnly));
	}

	@GetMapping("/{id}")
	public ResponseEntity<QuizResponse> getQuiz(@PathVariable Long id) {
		return ResponseEntity.ok(quizService.getQuiz(id));
	}

	@PostMapping
	public ResponseEntity<QuizResponse> createQuiz(@Valid @RequestBody QuizRequest request) {
		return ResponseEntity.ok(quizService.createQuiz(request));
	}

	@PutMapping("/{id}")
	public ResponseEntity<QuizResponse> updateQuiz(
			@PathVariable Long id,
			@Valid @RequestBody QuizRequest request) {
		return ResponseEntity.ok(quizService.updateQuiz(id, request));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteQuiz(@PathVariable Long id) {
		quizService.deleteQuiz(id);
		return ResponseEntity.noContent().build();
	}
}
