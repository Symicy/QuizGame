package com.example.demo.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.Submission;

@Repository
public interface SubmissionRepo extends JpaRepository<Submission, Long> {

    List<Submission> findBySessionId(Long sessionId);

    boolean existsBySessionIdAndQuestionId(Long sessionId, Long questionId);

    long countBySessionId(Long sessionId);

    Optional<Submission> findBySessionIdAndQuestionId(Long sessionId, Long questionId);
}
