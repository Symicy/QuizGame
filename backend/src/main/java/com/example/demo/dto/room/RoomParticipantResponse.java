package com.example.demo.dto.room;

import java.time.LocalDateTime;

import com.example.demo.domain.RoomParticipant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomParticipantResponse {

    private Long id;
    private Long userId;
    private String username;
    private Boolean ready;
    private Boolean active;
    private Integer currentScore;
    private Integer correctAnswers;
    private Integer finalRank;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;

    public static RoomParticipantResponse fromEntity(RoomParticipant participant) {
        if (participant == null) {
            return null;
        }
        return RoomParticipantResponse.builder()
                .id(participant.getId())
                .userId(participant.getUser() != null ? participant.getUser().getId() : null)
                .username(participant.getUser() != null ? participant.getUser().getUsername() : null)
                .ready(participant.getIsReady())
                .active(participant.getIsActive())
                .currentScore(participant.getCurrentScore())
                .correctAnswers(participant.getCorrectAnswers())
                .finalRank(participant.getFinalRank())
                .joinedAt(participant.getJoinedAt())
                .leftAt(participant.getLeftAt())
                .build();
    }
}
