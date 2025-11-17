package com.example.demo.resource;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.session.SessionResponse;
import com.example.demo.dto.session.SessionStartRequest;
import com.example.demo.dto.session.SubmissionRequest;
import com.example.demo.dto.session.SubmissionResponse;
import com.example.demo.service.SessionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Validated
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/solo")
    public ResponseEntity<SessionResponse> startSoloSession(@Valid @RequestBody SessionStartRequest request) {
        return ResponseEntity.ok(sessionService.startSoloSession(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSession(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getSession(id));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<SubmissionResponse> submitAnswer(@PathVariable Long id, @Valid @RequestBody SubmissionRequest request) {
        return ResponseEntity.ok(sessionService.submitAnswer(id, request));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<SessionResponse> completeSession(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.completeSession(id));
    }

    @GetMapping("/users/{userId}/recent")
    public ResponseEntity<List<SessionResponse>> getRecentSessions(@PathVariable Long userId) {
        return ResponseEntity.ok(sessionService.getRecentSessions(userId));
    }
}
