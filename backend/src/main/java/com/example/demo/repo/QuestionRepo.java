package com.example.demo.repo;

import java.util.Optional;

import com.example.demo.domain.Question;
import com.example.demo.enums.DifficultyLevel;

public interface QuestionRepo {
    
    Optional<Question> findById(Long id);
    Optional<Question> findByDifficulty(DifficultyLevel difficulty);
    Optional<Question> findByCategoryAndDifficulty(String category, DifficultyLevel difficulty);
    Optional<Question> findRandomQuestions(Long categoryId, DifficultyLevel difficultyLevel, int count);
    Optional<Long> countByCategoryId(Long categoryId);
    
}
