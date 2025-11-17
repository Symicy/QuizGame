package com.example.demo.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.Session;
import com.example.demo.enums.SessionStatus;

@Repository
public interface SessionRepo extends JpaRepository<Session, Long> {

    List<Session> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Session> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);

    List<Session> findByStatus(SessionStatus status);
}
