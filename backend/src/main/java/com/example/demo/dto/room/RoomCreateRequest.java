package com.example.demo.dto.room;

import com.example.demo.enums.DifficultyLevel;
import com.example.demo.enums.RoomType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoomCreateRequest {

    @NotNull(message = "Owner id is required")
    private Long ownerId;

    @NotNull(message = "Room type is required")
    private RoomType roomType;

    @NotNull(message = "Max players is required")
    @Min(value = 2, message = "Room must allow at least 2 players")
    @Max(value = 10, message = "Room cannot exceed 10 players")
    private Integer maxPlayers;

    @NotNull(message = "Question count is required")
    @Min(value = 5, message = "Need at least 5 questions")
    @Max(value = 50, message = "Too many questions requested")
    private Integer questionCount;

    @NotNull(message = "Time per question is required")
    @Min(value = 10, message = "Need at least 10 seconds per question")
    @Max(value = 120, message = "Maximum 120 seconds per question")
    private Integer timePerQuestion;

    private DifficultyLevel difficulty;

    private Long categoryId;

    private Long quizId;
}
