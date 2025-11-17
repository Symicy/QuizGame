package com.example.demo.dto.question;

import com.example.demo.enums.DifficultyLevel;
import com.example.demo.enums.SourceType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuestionRequest {

	@NotBlank(message = "Question text is required")
	@Size(max = 1000)
	private String text;

	@NotBlank(message = "Option A is required")
	private String optionA;

	@NotBlank(message = "Option B is required")
	private String optionB;

	@NotBlank(message = "Option C is required")
	private String optionC;

	@NotBlank(message = "Option D is required")
	private String optionD;

	@NotNull(message = "Correct option is required")
	@Min(value = 0, message = "Correct option must be between 0 and 3")
	@Max(value = 3, message = "Correct option must be between 0 and 3")
	private Integer correctOption;

	@NotNull(message = "Difficulty level is required")
	private DifficultyLevel difficultyLevel;

	private SourceType sourceType = SourceType.ADMIN;

	@Size(max = 2000)
	private String explanation;

	private Integer points;

	@NotNull(message = "Category id is required")
	private Long categoryId;

	private Boolean isActive;
}
