package com.example.demo.dto.duel;

import com.example.demo.enums.SessionStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DuelPlayerState {

    Long userId;
    String username;
    Integer score;
    Integer correctAnswers;
    Integer answeredQuestions;
    Integer totalQuestions;
    Boolean active;
    Boolean ready;
    SessionStatus sessionStatus;
    Long sessionId;
    Integer finalRank;
}
