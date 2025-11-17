package com.example.demo.resource;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.room.RoomCreateRequest;
import com.example.demo.dto.room.RoomPlayerRequest;
import com.example.demo.dto.room.RoomReadyRequest;
import com.example.demo.dto.room.RoomResponse;
import com.example.demo.enums.RoomStatus;
import com.example.demo.service.RoomService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Validated
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getRooms(@RequestParam(name = "status", required = false) RoomStatus status) {
        return ResponseEntity.ok(roomService.listRooms(status));
    }

    @GetMapping("/{code}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String code) {
        return ResponseEntity.ok(roomService.getRoomByCode(code));
    }

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomCreateRequest request) {
        return ResponseEntity.ok(roomService.createRoom(request));
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<RoomResponse> joinRoom(@PathVariable String code, @Valid @RequestBody RoomPlayerRequest request) {
        return ResponseEntity.ok(roomService.joinRoom(code, request));
    }

    @PostMapping("/{code}/leave")
    public ResponseEntity<RoomResponse> leaveRoom(@PathVariable String code, @Valid @RequestBody RoomPlayerRequest request) {
        return ResponseEntity.ok(roomService.leaveRoom(code, request));
    }

    @PostMapping("/{code}/ready")
    public ResponseEntity<RoomResponse> toggleReady(@PathVariable String code, @Valid @RequestBody RoomReadyRequest request) {
        return ResponseEntity.ok(roomService.toggleReady(code, request));
    }

    @PostMapping("/{code}/start")
    public ResponseEntity<RoomResponse> startRoom(@PathVariable String code, @Valid @RequestBody RoomPlayerRequest request) {
        return ResponseEntity.ok(roomService.startRoom(code, request));
    }

    @PostMapping("/{code}/finish")
    public ResponseEntity<RoomResponse> finishRoom(@PathVariable String code, @Valid @RequestBody RoomPlayerRequest request) {
        return ResponseEntity.ok(roomService.finishRoom(code, request));
    }
}
