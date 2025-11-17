package com.example.demo.dto.question;

import com.example.demo.enums.DifficultyLevel;
import com.example.demo.enums.OpenTriviaEncoding;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class QuestionImportRequest {

    private Long categoryId;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least 1")
    @Max(value = 50, message = "Amount cannot exceed 50")
    private Integer amount = 10;

    private DifficultyLevel difficultyLevel;

    @Positive(message = "External category id must be positive")
    private Integer externalCategoryId;

    private OpenTriviaEncoding encoding = OpenTriviaEncoding.DEFAULT;

    private String sessionToken;
}
