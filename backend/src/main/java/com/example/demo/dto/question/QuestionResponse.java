package com.example.demo.dto.question;

import com.example.demo.domain.Question;
import com.example.demo.enums.DifficultyLevel;
import com.example.demo.enums.SourceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

	private Long id;
	private String text;
	private String optionA;
	private String optionB;
	private String optionC;
	private String optionD;
	private Integer correctOption;
	private DifficultyLevel difficultyLevel;
	private SourceType sourceType;
	private String explanation;
	private Integer points;
	private Boolean isActive;
	private Long categoryId;
	private String categoryName;

	public static QuestionResponse fromEntity(Question question) {
		if (question == null) {
			return null;
		}
		return QuestionResponse.builder()
				.id(question.getId())
				.text(question.getText())
				.optionA(question.getOptionA())
				.optionB(question.getOptionB())
				.optionC(question.getOptionC())
				.optionD(question.getOptionD())
				.correctOption(question.getCorrectOption())
				.difficultyLevel(question.getDifficultyLevel())
				.sourceType(question.getSourceType())
				.explanation(question.getExplanation())
				.points(question.getPoints())
				.isActive(question.getIsActive())
				.categoryId(question.getCategory() != null ? question.getCategory().getId() : null)
				.categoryName(question.getCategory() != null ? question.getCategory().getName() : null)
				.build();
	}
}
