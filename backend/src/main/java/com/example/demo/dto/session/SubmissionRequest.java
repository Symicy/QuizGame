package com.example.demo.dto.session;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmissionRequest {

    @NotNull(message = "Question id is required")
    private Long questionId;

    @NotNull(message = "Answer index is required")
    @Min(value = 0, message = "Answer index must be between 0 and 3")
    @Max(value = 3, message = "Answer index must be between 0 and 3")
    private Integer answerIndex;

    private Integer responseTimeMs;
}
