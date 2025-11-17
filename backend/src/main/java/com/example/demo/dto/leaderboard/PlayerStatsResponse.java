package com.example.demo.dto.leaderboard;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerStatsResponse {

    private Long userId;
    private String username;
    private Integer totalSessions;
    private Integer bestScore;
    private Double averageScore;
    private LocalDateTime lastPlayedAt;
    private List<LeaderboardEntryResponse> recentScores;
}
