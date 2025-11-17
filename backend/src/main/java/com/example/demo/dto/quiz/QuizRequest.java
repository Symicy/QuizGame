package com.example.demo.dto.quiz;

import java.util.List;

import com.example.demo.enums.DifficultyLevel;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class QuizRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150)
    private String title;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Difficulty level is required")
    private DifficultyLevel difficulty;

    @NotNull(message = "Category id is required")
    private Long categoryId;

    @NotNull(message = "Creator id is required")
    private Long createdBy;

    @Min(value = 1, message = "Quiz must contain at least one question")
    private Integer totalQuestions;

    private Integer timeLimitSeconds;

    private Boolean isActive;

    private List<Long> questionIds;
}
