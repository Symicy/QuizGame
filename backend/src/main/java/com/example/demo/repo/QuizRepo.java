package com.example.demo.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.Quiz;
import com.example.demo.enums.DifficultyLevel;

@Repository
public interface QuizRepo extends JpaRepository<Quiz, Long> {

    List<Quiz> findByIsActiveTrue();

    List<Quiz> findByCategoryId(Long categoryId);

    List<Quiz> findByDifficulty(DifficultyLevel difficulty);

    List<Quiz> findByCategoryIdAndDifficulty(Long categoryId, DifficultyLevel difficulty);

    List<Quiz> findByCreatedById(Long userId);
}
