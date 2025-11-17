package com.example.demo.dto.leaderboard;

import java.time.LocalDateTime;

import com.example.demo.domain.Score;
import com.example.demo.enums.DifficultyLevel;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaderboardEntryResponse {

    private Long scoreId;
    private Long userId;
    private String username;
    private Integer totalPoints;
    private Integer correctAnswers;
    private Integer totalQuestions;
    private Double accuracy;
    private DifficultyLevel difficulty;
    private Integer rank;
    private LocalDateTime createdAt;
    private Long quizId;
    private String quizTitle;
    private Long categoryId;
    private String categoryName;

    public static LeaderboardEntryResponse fromEntity(Score score) {
        return fromEntity(score, null);
    }

    public static LeaderboardEntryResponse fromEntity(Score score, Integer rankOverride) {
        if (score == null) {
            return null;
        }
        return LeaderboardEntryResponse.builder()
                .scoreId(score.getId())
                .userId(score.getUser() != null ? score.getUser().getId() : null)
                .username(score.getUser() != null ? score.getUser().getUsername() : null)
                .totalPoints(score.getTotalPoints())
                .correctAnswers(score.getCorrectAnswers())
                .totalQuestions(score.getTotalQuestions())
                .accuracy(score.getAccuracy())
                .difficulty(score.getDifficulty())
                .rank(rankOverride != null ? rankOverride : score.getRank())
                .createdAt(score.getCreatedAt())
                .quizId(score.getQuiz() != null ? score.getQuiz().getId() : null)
                .quizTitle(score.getQuiz() != null ? score.getQuiz().getTitle() : null)
                .categoryId(score.getCategory() != null ? score.getCategory().getId() : null)
                .categoryName(score.getCategory() != null ? score.getCategory().getName() : null)
                .build();
    }
}
