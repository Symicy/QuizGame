package com.example.demo.dto.leaderboard;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaderboardResponse {

    private List<LeaderboardEntryResponse> global;
    private List<LeaderboardEntryResponse> category;
    private List<LeaderboardEntryResponse> quiz;
}
