package com.example.demo.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.domain.OpenTriviaToken;

public interface OpenTriviaTokenRepo extends JpaRepository<OpenTriviaToken, Long> {
}
