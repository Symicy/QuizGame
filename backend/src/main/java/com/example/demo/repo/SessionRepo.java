package com.example.demo.repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.Session;
import com.example.demo.enums.SessionStatus;

@Repository
public interface SessionRepo extends JpaRepository<Session, Long> {

    List<Session> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Session> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);

    List<Session> findByStatus(SessionStatus status);

    List<Session> findByRoomId(Long roomId);

    List<Session> findByRoomIdAndStatus(Long roomId, SessionStatus status);

    List<Session> findByRoomIdAndCreatedAtAfter(Long roomId, LocalDateTime createdAfter);

    Optional<Session> findTopByRoomIdAndUserIdOrderByCreatedAtDesc(Long roomId, Long userId);

    Optional<Session> findTopByRoomIdAndUserIdAndStatusOrderByCreatedAtDesc(Long roomId, Long userId, SessionStatus status);
}
