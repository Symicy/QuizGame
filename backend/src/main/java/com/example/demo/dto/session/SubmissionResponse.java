package com.example.demo.dto.session;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubmissionResponse {

    private Long sessionId;
    private Long questionId;
    private Boolean correct;
    private Integer pointsEarned;
    private Integer totalScore;
    private Integer correctAnswers;
    private Integer answeredQuestions;
    private Integer totalQuestions;
}
