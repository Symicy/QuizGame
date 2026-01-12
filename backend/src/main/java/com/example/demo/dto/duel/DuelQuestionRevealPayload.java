package com.example.demo.dto.duel;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DuelQuestionRevealPayload {

    Long questionId;
    Integer correctOption;
    List<DuelPlayerState> leaderboard;
    LocalDateTime revealedAt;
    List<PlayerReveal> playerChoices;

    @Value
    @Builder
    public static class PlayerReveal {
        Long userId;
        Integer selectedOption;
        Boolean correct;
        Boolean answered;
    }
}
