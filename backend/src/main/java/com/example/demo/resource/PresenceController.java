package com.example.demo.resource;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.presence.OnlineUserResponse;
import com.example.demo.dto.presence.PresencePingRequest;
import com.example.demo.service.PresenceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/presence")
@RequiredArgsConstructor
@Validated
public class PresenceController {

    private final PresenceService presenceService;

    @PostMapping("/ping")
    public ResponseEntity<OnlineUserResponse> ping(@Valid @RequestBody PresencePingRequest request) {
        return ResponseEntity.ok(presenceService.heartbeat(request.getUserId()));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> offline(@PathVariable Long userId) {
        presenceService.markOffline(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/online")
    public ResponseEntity<List<OnlineUserResponse>> listOnline(
            @RequestParam(name = "excludeUserId", required = false) Long excludeUserId) {
        return ResponseEntity.ok(presenceService.listOnline(excludeUserId));
    }
}
