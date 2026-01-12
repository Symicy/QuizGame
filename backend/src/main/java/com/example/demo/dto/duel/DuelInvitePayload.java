package com.example.demo.dto.duel;

import java.time.Instant;

import com.example.demo.enums.DifficultyLevel;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DuelInvitePayload {

    private String inviteId;
    private String roomCode;
    private Long inviterId;
    private String inviterUsername;
    private Integer questionCount;
    private Integer timePerQuestion;
    private DifficultyLevel difficulty;
    private Instant sentAt;
}
