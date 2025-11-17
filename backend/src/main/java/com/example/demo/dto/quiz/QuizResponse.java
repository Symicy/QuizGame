package com.example.demo.dto.quiz;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.example.demo.domain.Quiz;
import com.example.demo.enums.DifficultyLevel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuizResponse {

	private Long id;
	private String title;
	private String description;
	private DifficultyLevel difficulty;
	private Integer totalQuestions;
	private Integer timeLimitSeconds;
	private Boolean isActive;
	private Long categoryId;
	private String categoryName;
	private Long createdById;
	private String createdByUsername;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private List<Long> questionIds;

	public static QuizResponse fromEntity(Quiz quiz) {
		if (quiz == null) {
			return null;
		}

		return QuizResponse.builder()
				.id(quiz.getId())
				.title(quiz.getTitle())
				.description(quiz.getDescription())
				.difficulty(quiz.getDifficulty())
				.totalQuestions(quiz.getTotalQuestions())
				.timeLimitSeconds(quiz.getTimeLimitSeconds())
				.isActive(quiz.getIsActive())
				.categoryId(quiz.getCategory() != null ? quiz.getCategory().getId() : null)
				.categoryName(quiz.getCategory() != null ? quiz.getCategory().getName() : null)
				.createdById(quiz.getCreatedBy() != null ? quiz.getCreatedBy().getId() : null)
				.createdByUsername(quiz.getCreatedBy() != null ? quiz.getCreatedBy().getUsername() : null)
				.createdAt(quiz.getCreatedAt())
				.updatedAt(quiz.getUpdatedAt())
				.questionIds(quiz.getQuestions().stream()
						.map(question -> question.getId())
						.collect(Collectors.toList()))
				.build();
	}
}
