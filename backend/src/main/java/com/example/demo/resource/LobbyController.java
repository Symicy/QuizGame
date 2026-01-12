package com.example.demo.resource;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.lobby.LobbyAnswerRequest;
import com.example.demo.dto.lobby.LobbyAnswerResponse;
import com.example.demo.dto.lobby.LobbyStateResponse;
import com.example.demo.dto.room.RoomPlayerRequest;
import com.example.demo.dto.room.RoomResponse;
import com.example.demo.service.PublicLobbyService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/lobby")
@RequiredArgsConstructor
@Validated
public class LobbyController {

    private final PublicLobbyService lobbyService;

    @GetMapping
    public ResponseEntity<LobbyStateResponse> getLobbyState(@RequestParam(name = "userId", required = false) Long userId) {
        return ResponseEntity.ok(lobbyService.getLobbyState(userId));
    }

    @PostMapping("/join")
    public ResponseEntity<RoomResponse> joinLobby(@Valid @RequestBody RoomPlayerRequest request) {
        return ResponseEntity.ok(lobbyService.joinLobby(request));
    }

    @PostMapping("/leave")
    public ResponseEntity<RoomResponse> leaveLobby(@Valid @RequestBody RoomPlayerRequest request) {
        return ResponseEntity.ok(lobbyService.leaveLobby(request));
    }

    @PostMapping("/answer")
    public ResponseEntity<LobbyAnswerResponse> submitAnswer(@Valid @RequestBody LobbyAnswerRequest request) {
        return ResponseEntity.ok(lobbyService.submitAnswer(request));
    }
}
