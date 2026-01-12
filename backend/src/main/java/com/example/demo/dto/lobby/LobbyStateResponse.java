package com.example.demo.dto.lobby;

import java.time.Instant;
import java.util.List;

import com.example.demo.enums.LobbyPhase;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LobbyStateResponse {

    private String roomCode;
    private LobbyPhase phase;
    private Integer roundNumber;
    private LobbyQuestionView currentQuestion;
    private Integer currentQuestionIndex;
    private Integer totalQuestions;
    private Instant countdownEndsAt;
    private Instant questionEndsAt;
    private Instant resultsEndsAt;
    private Integer activePlayers;
    private Boolean playerAnswered;
    private Integer playerScore;
    private Integer playerRank;
    private Boolean participant;
    private List<LobbyLeaderboardEntry> leaderboard;
}
