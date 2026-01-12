package com.example.demo.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.example.demo.domain.User;
import com.example.demo.dto.presence.OnlineUserResponse;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repo.UserRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private static final Duration ONLINE_TTL = Duration.ofSeconds(60);

    private final UserRepo userRepo;
    private final Map<Long, PresenceRecord> activeUsers = new ConcurrentHashMap<>();

    public OnlineUserResponse heartbeat(Long userId) {
        User user = resolveUser(userId);
        PresenceRecord record = activeUsers.compute(user.getId(), (id, existing) -> {
            PresenceRecord next = existing != null ? existing : new PresenceRecord();
            next.userId = user.getId();
            next.username = user.getUsername();
            next.role = user.getRole() != null ? user.getRole().name() : null;
            next.lastSeen = Instant.now();
            return next;
        });
        return toResponse(record);
    }

    public void markOffline(Long userId) {
        if (userId == null) {
            return;
        }
        activeUsers.remove(userId);
    }

    public boolean isUserOnline(Long userId) {
        if (userId == null) {
            return false;
        }
        PresenceRecord record = activeUsers.get(userId);
        if (record == null) {
            return false;
        }
        boolean expired = isExpired(record, Instant.now().minus(ONLINE_TTL));
        if (expired) {
            activeUsers.remove(userId);
            return false;
        }
        return true;
    }

    public List<OnlineUserResponse> listOnline(Long excludeUserId) {
        Instant cutoff = Instant.now().minus(ONLINE_TTL);
        activeUsers.entrySet().removeIf(entry -> isExpired(entry.getValue(), cutoff));
        return activeUsers.values().stream()
                .filter(record -> !Objects.equals(record.userId, excludeUserId))
                .sorted(Comparator.comparing((PresenceRecord record) -> record.lastSeen).reversed())
            .map(this::toResponse)
            .filter(Objects::nonNull)
                .toList();
    }

    private boolean isExpired(PresenceRecord record, Instant cutoff) {
        return record == null || record.lastSeen == null || record.lastSeen.isBefore(cutoff);
    }

    private OnlineUserResponse toResponse(PresenceRecord record) {
        if (record == null) {
            return null;
        }
        return OnlineUserResponse.builder()
                .id(record.userId)
                .username(record.username)
                .role(record.role)
                .lastSeenAt(record.lastSeen)
                .build();
    }

    private User resolveUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private static final class PresenceRecord {
        private Long userId;
        private String username;
        private String role;
        private Instant lastSeen;
    }
}
