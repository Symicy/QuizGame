package com.example.demo.domain;

import com.example.demo.enums.DifficultyLevel;
import com.example.demo.enums.RoomStatus;
import com.example.demo.enums.RoomType;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Long id;

    // 6-character room code (e.g., "ABC123")
    @Column(name = "code", unique = true, nullable = false, length = 6)
    private String code;

    // Room type
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false)
    private RoomType roomType;

    // Current status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RoomStatus status;

    // Max players allowed
    @Column(name = "max_players", nullable = false)
    private Integer maxPlayers;

    // Current number of players
    @Column(name = "current_players", nullable = false)
    private Integer currentPlayers;

    // Game settings
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty")
    private DifficultyLevel difficulty;

    @Column(name = "question_count")
    private Integer questionCount;

    @Column(name = "time_per_question")
    private Integer timePerQuestion; // seconds

    // Game state (for syncing)
    @Column(name = "current_question_index")
    private Integer currentQuestionIndex;

    @Column(name = "round_started_at")
    private LocalDateTime roundStartedAt;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id")
    private Quiz quiz;

    // Questions for this room
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "room_questions",
        joinColumns = @JoinColumn(name = "room_id"),
        inverseJoinColumns = @JoinColumn(name = "question_id")
    )
    @OrderColumn(name = "question_order")
    private List<Question> questions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = RoomStatus.WAITING;
        }
        if (currentPlayers == null) {
            currentPlayers = 0;
        }
        if (currentQuestionIndex == null) {
            currentQuestionIndex = 0;
        }
    }

    // Helper methods
    public boolean isFull() {
        return currentPlayers >= maxPlayers;
    }

    public boolean canStart() {
        return currentPlayers >= 2 && status == RoomStatus.WAITING;
    }

    public void incrementPlayers() {
        this.currentPlayers++;
    }

    public void decrementPlayers() {
        if (this.currentPlayers > 0) {
            this.currentPlayers--;
        }
    }
}
