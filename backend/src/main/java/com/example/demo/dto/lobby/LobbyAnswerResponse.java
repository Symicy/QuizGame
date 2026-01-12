package com.example.demo.dto.lobby;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LobbyAnswerResponse {

    private boolean accepted;
    private boolean correct;
    private Integer pointsEarned;
    private Integer playerScore;
    private Integer playerRank;
    private Instant questionEndsAt;
    private Integer correctOption;
}
