package com.example.demo.dto.session;

import com.example.demo.enums.DifficultyLevel;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SessionStartRequest {

    @NotNull(message = "User id is required")
    private Long userId;

    private Long quizId;

    private Long categoryId;

    private DifficultyLevel difficulty;

    @Min(value = 5, message = "Need at least 5 questions")
    @Max(value = 50, message = "Too many questions requested")
    private Integer questionCount;

    @Min(value = 10, message = "Need at least 10 seconds per question")
    @Max(value = 120, message = "Maximum 120 seconds per question")
    private Integer timePerQuestion;
}
