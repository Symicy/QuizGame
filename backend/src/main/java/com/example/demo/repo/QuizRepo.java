package com.example.demo.repo;

import java.util.Optional;

import com.example.demo.domain.Quiz;
import com.example.demo.enums.DifficultyLevel;

public interface QuizRepo {
    Optional<Quiz> findById(Long id);
    Optional<Quiz> findByAll();
    Optional<Quiz> findByCategoryId(Long categoryId);
    Optional<Quiz> findByCreatedBy(Long userId);
    Optional<Quiz> findByDifficultyLevel(DifficultyLevel difficultyLevel);
    Optional<Quiz> findByCategoryAndDifficulty(Long categoryId, DifficultyLevel difficultyLevel);
}
