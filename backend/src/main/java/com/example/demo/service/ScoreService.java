package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.Score;
import com.example.demo.domain.Session;
import com.example.demo.repo.ScoreRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ScoreService {

    private final ScoreRepo scoreRepo;

    @SuppressWarnings("null")
    public Score recordScoreFromSession(Session session) {
        if (session == null) {
            throw new IllegalArgumentException("Session is required");
        }
        Score score = Score.fromSession(session);
        return scoreRepo.save(score);
    }
}
