package com.example.demo.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.Score;

@Repository
public interface ScoreRepo extends JpaRepository<Score, Long> {

    List<Score> findTop10ByOrderByTotalPointsDesc();

    List<Score> findTop10ByCategoryIdOrderByTotalPointsDesc(Long categoryId);

    List<Score> findTop10ByQuizIdOrderByTotalPointsDesc(Long quizId);

    List<Score> findByUserIdOrderByCreatedAtDesc(Long userId);
}
