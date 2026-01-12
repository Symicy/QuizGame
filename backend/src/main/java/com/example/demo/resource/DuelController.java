package com.example.demo.resource;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.duel.DuelAnswerRequest;
import com.example.demo.dto.duel.DuelAnswerResponse;
import com.example.demo.dto.duel.DuelStateResponse;
import com.example.demo.service.DuelService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/duels")
@RequiredArgsConstructor
@Validated
public class DuelController {

    private final DuelService duelService;

    @GetMapping("/{code}")
    public ResponseEntity<DuelStateResponse> getState(@PathVariable String code,
            @RequestParam(name = "userId") Long userId) {
        return ResponseEntity.ok(duelService.getState(code, userId));
    }

    @PostMapping("/{code}/answer")
    public ResponseEntity<DuelAnswerResponse> submitAnswer(@PathVariable String code,
            @Valid @RequestBody DuelAnswerRequest request) {
        return ResponseEntity.ok(duelService.submitAnswer(code, request));
    }
}
