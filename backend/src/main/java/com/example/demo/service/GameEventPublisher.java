package com.example.demo.service;

import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.domain.Room;
import com.example.demo.domain.RoomParticipant;
import com.example.demo.dto.game.GameEventPayload;
import com.example.demo.dto.room.RoomResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GameEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishRoomUpdate(Room room, List<RoomParticipant> participants) {
        if (room == null) {
            return;
        }
        RoomResponse response = RoomResponse.fromEntity(room, participants);
        GameEventPayload<RoomResponse> payload = GameEventPayload.<RoomResponse>builder()
                .type("ROOM_UPDATED")
                .roomCode(room.getCode())
                .payload(response)
                .build();
        messagingTemplate.convertAndSend(topicForRoom(room.getCode()), Objects.requireNonNull(payload));
    }

    public <T> void publishCustomRoomEvent(String roomCode, String eventType, T payload) {
        GameEventPayload<T> event = GameEventPayload.<T>builder()
                .type(eventType)
                .roomCode(roomCode)
                .payload(payload)
                .build();
        messagingTemplate.convertAndSend(topicForRoom(roomCode), Objects.requireNonNull(event));
    }

    public <T> void publishUserEvent(Long userId, String eventType, T payload) {
        if (userId == null) {
            return;
        }
        GameEventPayload<T> event = GameEventPayload.<T>builder()
                .type(eventType)
                .payload(payload)
                .build();
        messagingTemplate.convertAndSend(topicForUser(userId), Objects.requireNonNull(event));
    }

    private @NonNull String topicForRoom(String roomCode) {
        return "/topic/rooms/" + Objects.requireNonNull(roomCode, "roomCode");
    }

    private @NonNull String topicForUser(Long userId) {
        return "/topic/users/" + Objects.requireNonNull(userId, "userId");
    }
}
