package com.example.demo.resource;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.leaderboard.LeaderboardResponse;
import com.example.demo.dto.leaderboard.PlayerStatsResponse;
import com.example.demo.service.LeaderboardService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/leaderboards")
@RequiredArgsConstructor
@Validated
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<LeaderboardResponse> getLeaderboard(
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "quizId", required = false) Long quizId) {
        return ResponseEntity.ok(leaderboardService.getLeaderboard(categoryId, quizId));
    }

    @GetMapping("/players/{userId}")
    public ResponseEntity<PlayerStatsResponse> getPlayerStats(@PathVariable Long userId) {
        return ResponseEntity.ok(leaderboardService.getPlayerStats(userId));
    }
}
