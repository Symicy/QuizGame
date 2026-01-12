package com.example.demo.dto.duel;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.dto.session.SessionResponse;
import com.example.demo.enums.RoomStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DuelStateResponse {

    String roomCode;
    RoomStatus roomStatus;
    Integer questionCount;
    Integer timePerQuestion;
    Integer currentQuestionIndex;
    LocalDateTime startedAt;
    LocalDateTime finishedAt;
    SessionResponse session;
    List<DuelPlayerState> leaderboard;
    DuelPlayerState player;
    DuelPlayerState opponent;
    boolean duelActive;
    boolean duelCompleted;
    Long winnerUserId;
}
