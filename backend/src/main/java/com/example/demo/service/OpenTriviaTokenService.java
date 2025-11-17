package com.example.demo.service;

import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.demo.domain.OpenTriviaToken;
import com.example.demo.repo.OpenTriviaTokenRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OpenTriviaTokenService {

    private final OpenTriviaTokenRepo tokenRepo;

    @Transactional(readOnly = true)
    public Optional<String> getToken() {
        return tokenRepo.findById(OpenTriviaToken.SINGLETON_ID)
                .map(OpenTriviaToken::getToken);
    }

    public String storeToken(String tokenValue) {
        if (!StringUtils.hasText(tokenValue)) {
            throw new IllegalArgumentException("Tokenul OpenTrivia nu poate fi gol");
        }

        OpenTriviaToken entity = tokenRepo.findById(OpenTriviaToken.SINGLETON_ID)
                .orElseGet(OpenTriviaToken::new);
        entity.setId(OpenTriviaToken.SINGLETON_ID);
        entity.setToken(tokenValue.trim());
        entity.setUpdatedAt(Instant.now());
        tokenRepo.save(entity);
        return entity.getToken();
    }

    public void clearToken() {
        tokenRepo.deleteById(OpenTriviaToken.SINGLETON_ID);
    }
}
