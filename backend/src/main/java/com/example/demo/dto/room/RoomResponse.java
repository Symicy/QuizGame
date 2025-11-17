package com.example.demo.dto.room;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.example.demo.domain.Question;
import com.example.demo.domain.Room;
import com.example.demo.domain.RoomParticipant;
import com.example.demo.enums.DifficultyLevel;
import com.example.demo.enums.RoomStatus;
import com.example.demo.enums.RoomType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomResponse {

    private Long id;
    private String code;
    private RoomType roomType;
    private RoomStatus status;
    private Integer maxPlayers;
    private Integer currentPlayers;
    private DifficultyLevel difficulty;
    private Integer questionCount;
    private Integer timePerQuestion;
    private Integer currentQuestionIndex;
    private LocalDateTime roundStartedAt;
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
    private Long ownerId;
    private String ownerUsername;
    private Long categoryId;
    private String categoryName;
    private Long quizId;
    private String quizTitle;
    private List<Long> questionIds;
    private List<RoomParticipantResponse> participants;

    public static RoomResponse fromEntity(Room room, List<RoomParticipant> participants) {
        if (room == null) {
            return null;
        }

        List<Long> questionIds = room.getQuestions() != null
                ? room.getQuestions().stream().map(Question::getId).collect(Collectors.toList())
                : List.of();

        List<RoomParticipantResponse> participantResponses = participants != null
                ? participants.stream().map(RoomParticipantResponse::fromEntity).toList()
                : List.of();

        return RoomResponse.builder()
                .id(room.getId())
                .code(room.getCode())
                .roomType(room.getRoomType())
                .status(room.getStatus())
                .maxPlayers(room.getMaxPlayers())
                .currentPlayers(room.getCurrentPlayers())
                .difficulty(room.getDifficulty())
                .questionCount(room.getQuestionCount())
                .timePerQuestion(room.getTimePerQuestion())
                .currentQuestionIndex(room.getCurrentQuestionIndex())
                .roundStartedAt(room.getRoundStartedAt())
                .createdAt(room.getCreatedAt())
                .closedAt(room.getClosedAt())
                .ownerId(room.getOwner() != null ? room.getOwner().getId() : null)
                .ownerUsername(room.getOwner() != null ? room.getOwner().getUsername() : null)
                .categoryId(room.getCategory() != null ? room.getCategory().getId() : null)
                .categoryName(room.getCategory() != null ? room.getCategory().getName() : null)
                .quizId(room.getQuiz() != null ? room.getQuiz().getId() : null)
                .quizTitle(room.getQuiz() != null ? room.getQuiz().getTitle() : null)
                .questionIds(questionIds)
                .participants(participantResponses)
                .build();
    }
}
