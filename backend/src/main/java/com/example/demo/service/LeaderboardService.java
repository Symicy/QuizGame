package com.example.demo.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.Score;
import com.example.demo.domain.User;
import com.example.demo.dto.leaderboard.LeaderboardEntryResponse;
import com.example.demo.dto.leaderboard.LeaderboardResponse;
import com.example.demo.dto.leaderboard.PlayerStatsResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repo.ScoreRepo;
import com.example.demo.repo.UserRepo;
import com.example.demo.repo.projection.UserScoreAggregate;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LeaderboardService {

    private final ScoreRepo scoreRepo;
    private final UserRepo userRepo;

    public LeaderboardResponse getLeaderboard(Long categoryId, Long quizId) {
        List<LeaderboardEntryResponse> global = mapAggregatesWithRank(
                scoreRepo.findTopUsersByTotalPoints(PageRequest.of(0, 10)));
        List<LeaderboardEntryResponse> category = categoryId != null
                ? mapScoresWithRank(scoreRepo.findTop10ByCategoryIdOrderByTotalPointsDesc(categoryId))
                : List.of();
        List<LeaderboardEntryResponse> quiz = quizId != null
                ? mapScoresWithRank(scoreRepo.findTop10ByQuizIdOrderByTotalPointsDesc(quizId))
                : List.of();

        return LeaderboardResponse.builder()
                .global(global)
                .category(category)
                .quiz(quiz)
                .build();
    }

    public PlayerStatsResponse getPlayerStats(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        List<Score> scores = scoreRepo.findByUserIdOrderByCreatedAtDesc(userId);
        if (scores.isEmpty()) {
            return PlayerStatsResponse.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .totalSessions(0)
                    .bestScore(0)
                    .averageScore(0.0)
                    .recentScores(List.of())
                    .build();
        }

        int totalSessions = scores.size();
        int bestScore = scores.stream().mapToInt(Score::getTotalPoints).max().orElse(0);
        double averageScore = scores.stream().mapToInt(Score::getTotalPoints).average().orElse(0.0);

        return PlayerStatsResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .totalSessions(totalSessions)
                .bestScore(bestScore)
                .averageScore(averageScore)
                .lastPlayedAt(scores.get(0).getCreatedAt())
                                .recentScores(mapScoresWithRank(scores.stream().limit(5).toList()))
                .build();
    }

        private List<LeaderboardEntryResponse> mapScoresWithRank(List<Score> scores) {
        AtomicInteger counter = new AtomicInteger(1);
        return scores.stream()
                .map(score -> LeaderboardEntryResponse.fromEntity(score, counter.getAndIncrement()))
                .toList();
    }

        private List<LeaderboardEntryResponse> mapAggregatesWithRank(List<UserScoreAggregate> aggregates) {
                AtomicInteger counter = new AtomicInteger(1);
                return aggregates.stream()
                                .map(entry -> LeaderboardEntryResponse.fromAggregate(entry, counter.getAndIncrement()))
                                .toList();
        }
}
