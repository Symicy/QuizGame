package com.example.demo.dto.lobby;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LobbyLeaderboardEntry {

    private Long userId;
    private String username;
    private Integer score;
    private Integer correctAnswers;
    private Integer rank;
}
