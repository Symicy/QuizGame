package com.example.demo.dto.lobby;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LobbyQuestionPayload {

    private int roundNumber;
    private int questionIndex;
    private int totalQuestions;
    private LobbyQuestionView question;
    private Instant endsAt;
}
