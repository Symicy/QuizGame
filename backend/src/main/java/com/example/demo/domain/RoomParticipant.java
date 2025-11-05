package com.example.demo.domain;

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
    name = "room_participants",
    uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"})
)
public class RoomParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    @Column(name = "is_ready", nullable = false)
    private Boolean isReady;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "current_score")
    private Integer currentScore;

    @Column(name = "correct_answers")
    private Integer correctAnswers;

    @Column(name = "final_rank")
    private Integer finalRank;

    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "left_at")
    private LocalDateTime leftAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        joinedAt = LocalDateTime.now();
        if (isReady == null) {
            isReady = false;
        }
        if (isActive == null) {
            isActive = true;
        }
        if (currentScore == null) {
            currentScore = 0;
        }
        if (correctAnswers == null) {
            correctAnswers = 0;
        }
    }

    public void addPoints(int points) {
        this.currentScore += points;
    }

    public void incrementCorrectAnswers() {
        this.correctAnswers++;
    }

    public void leave() {
        this.isActive = false;
        this.leftAt = LocalDateTime.now();
    }
}
