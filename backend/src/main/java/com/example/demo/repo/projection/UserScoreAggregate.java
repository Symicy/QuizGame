package com.example.demo.repo.projection;

import java.time.LocalDateTime;

public interface UserScoreAggregate {

    Long getUserId();

    String getUsername();

    Long getTotalPoints();

    Long getTotalCorrectAnswers();

    Long getTotalQuestions();

    Double getAverageAccuracy();

    Long getTotalSessions();

    LocalDateTime getLastPlayedAt();
}
