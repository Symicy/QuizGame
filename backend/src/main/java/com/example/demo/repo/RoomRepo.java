package com.example.demo.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.domain.Room;
import com.example.demo.enums.RoomStatus;

@Repository
public interface RoomRepo extends JpaRepository<Room, Long> {

    Optional<Room> findByCode(String code);

    boolean existsByCode(String code);

    List<Room> findByStatus(RoomStatus status);

    List<Room> findByOwnerId(Long ownerId);
}
