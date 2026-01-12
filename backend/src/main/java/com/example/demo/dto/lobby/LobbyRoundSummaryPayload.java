package com.example.demo.dto.lobby;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LobbyRoundSummaryPayload {

    private int roundNumber;
    private List<LobbyLeaderboardEntry> topPlayers;
}
