package com.example.demo.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.RoomParticipant;

@Repository
public interface RoomParticipantRepo extends JpaRepository<RoomParticipant, Long> {

    List<RoomParticipant> findByRoomId(Long roomId);

    List<RoomParticipant> findByRoomIdAndIsActiveTrue(Long roomId);

    Optional<RoomParticipant> findByRoomIdAndUserId(Long roomId, Long userId);

    boolean existsByRoomIdAndUserId(Long roomId, Long userId);

    long countByRoomIdAndIsActiveTrue(Long roomId);
}
