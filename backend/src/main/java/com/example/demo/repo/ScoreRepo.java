package com.example.demo.repo;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.Score;
import com.example.demo.repo.projection.UserScoreAggregate;

@Repository
public interface ScoreRepo extends JpaRepository<Score, Long> {

    List<Score> findTop10ByOrderByTotalPointsDesc();

    List<Score> findTop10ByCategoryIdOrderByTotalPointsDesc(Long categoryId);

    List<Score> findTop10ByQuizIdOrderByTotalPointsDesc(Long quizId);

    List<Score> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
            SELECT s.user.id AS userId,
                   s.user.username AS username,
                   SUM(s.totalPoints) AS totalPoints,
                   SUM(s.correctAnswers) AS totalCorrectAnswers,
                   SUM(s.totalQuestions) AS totalQuestions,
                   AVG(s.accuracy) AS averageAccuracy,
                   COUNT(s.id) AS totalSessions,
                   MAX(s.createdAt) AS lastPlayedAt
            FROM Score s
            GROUP BY s.user.id, s.user.username
            ORDER BY SUM(s.totalPoints) DESC
            """)
    List<UserScoreAggregate> findTopUsersByTotalPoints(Pageable pageable);
}
