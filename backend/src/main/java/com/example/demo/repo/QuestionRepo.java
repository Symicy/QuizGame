package com.example.demo.repo;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.Question;
import com.example.demo.enums.DifficultyLevel;
import com.example.demo.enums.SourceType;

@Repository
public interface QuestionRepo extends JpaRepository<Question, Long> {

    List<Question> findByCategoryId(Long categoryId);

    List<Question> findByDifficultyLevel(DifficultyLevel difficulty);

    List<Question> findByCategoryIdAndDifficultyLevel(Long categoryId, DifficultyLevel difficultyLevel);

    List<Question> findByCategoryIdAndDifficultyLevelAndIsActiveTrue(Long categoryId, DifficultyLevel difficultyLevel);

    long countByCategoryId(Long categoryId);

    @Query("""
            SELECT q FROM Question q
            WHERE (:categoryId IS NULL OR q.category.id = :categoryId)
              AND (:difficulty IS NULL OR q.difficultyLevel = :difficulty)
              AND q.isActive = true
            ORDER BY function('RAND')
            """)
    List<Question> findRandomActiveQuestions(
            @Param("categoryId") Long categoryId,
            @Param("difficulty") DifficultyLevel difficulty,
            Pageable pageable);

    @Query(value = """
            SELECT q FROM Question q
            WHERE (:categoryId IS NULL OR q.category.id = :categoryId)
              AND (:difficulty IS NULL OR q.difficultyLevel = :difficulty)
              AND (:sourceType IS NULL OR q.sourceType = :sourceType)
              AND (:activeOnly = false OR q.isActive = true)
            """,
           countQuery = """
            SELECT COUNT(q) FROM Question q
            WHERE (:categoryId IS NULL OR q.category.id = :categoryId)
              AND (:difficulty IS NULL OR q.difficultyLevel = :difficulty)
              AND (:sourceType IS NULL OR q.sourceType = :sourceType)
              AND (:activeOnly = false OR q.isActive = true)
            """)
    Page<Question> findByFilters(
            @Param("categoryId") Long categoryId,
            @Param("difficulty") DifficultyLevel difficulty,
            @Param("sourceType") SourceType sourceType,
            @Param("activeOnly") boolean activeOnly,
            Pageable pageable);
}
