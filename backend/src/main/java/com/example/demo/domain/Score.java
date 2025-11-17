package com.example.demo.domain;

import com.example.demo.enums.DifficultyLevel;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Table(
    name = "scores",
    indexes = {
        @Index(name = "idx_total_points", columnList = "total_points DESC"),
        @Index(name = "idx_user_created", columnList = "user_id, created_at DESC"),
        @Index(name = "idx_quiz_score", columnList = "quiz_id, total_points DESC"),
        @Index(name = "idx_category_score", columnList = "category_id, total_points DESC")
    }
)
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints;

    @Column(name = "correct_answers", nullable = false)
    private Integer correctAnswers;

    @Column(name = "total_questions", nullable = false)
    private Integer totalQuestions;

    @Column(name = "accuracy")
    private Double accuracy;

    @Column(name = "time_spent")
    private Integer timeSpent;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    private DifficultyLevel difficulty;

    @Column(name = "score_rank")
    private Integer rank;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", unique = true, nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        calculateAccuracy();
    }

    @PreUpdate
    protected void onUpdate() {
        calculateAccuracy();
    }

    private void calculateAccuracy() {
        if (totalQuestions != null && totalQuestions > 0) {
            this.accuracy = (correctAnswers * 100.0) / totalQuestions;
        } else {
            this.accuracy = 0.0;
        }
    }

    public static Score fromSession(Session session) {
        Score score = new Score();
        score.setSession(session);
        score.setUser(session.getUser());
        score.setQuiz(session.getQuiz());
        score.setCategory(session.getCategory());
        score.setDifficulty(session.getDifficulty());
        score.setTotalPoints(session.getFinalScore());
        score.setCorrectAnswers(session.getCorrectAnswers());
        score.setTotalQuestions(session.getTotalQuestions());
        score.setTimeSpent(session.getDurationSeconds());
        return score;
    }
}
