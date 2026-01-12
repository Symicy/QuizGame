package com.example.demo.dto.lobby;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LobbyQuestionResultPayload {

    private int roundNumber;
    private Long questionId;
    private Integer correctOption;
    private List<LobbyLeaderboardEntry> leaderboard;
}
