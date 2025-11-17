package com.example.demo.external.opentrivia;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.demo.dto.question.OpenTriviaCategoryResponse;
import com.example.demo.enums.DifficultyLevel;
import com.example.demo.enums.OpenTriviaEncoding;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenTriviaApiClient {

    private final RestTemplate restTemplate;

    @Value("${opentdb.base-url:https://opentdb.com}")
    private String baseUrl;

    public List<OpenTriviaCategoryResponse> fetchCategories() {
        String url = baseUrl + "/api_category.php";
        try {
            ResponseEntity<TriviaCategoryListResponse> response = restTemplate.getForEntity(url,
                    TriviaCategoryListResponse.class);
            TriviaCategoryListResponse body = response.getBody();
            if (body == null || body.triviaCategories() == null) {
                return Collections.emptyList();
            }
            return body.triviaCategories();
        } catch (RestClientException ex) {
            log.warn("Failed to fetch OpenTrivia categories", ex);
            return Collections.emptyList();
        }
    }

    public List<OpenTriviaQuestion> fetchQuestions(
            int amount,
            Integer categoryId,
            DifficultyLevel difficultyLevel,
            OpenTriviaEncoding encoding,
            String sessionToken) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl + "/api.php")
                .queryParam("amount", amount)
                .queryParam("type", "multiple");

        if (categoryId != null) {
            builder.queryParam("category", categoryId);
        }

        if (difficultyLevel != null) {
            builder.queryParam("difficulty", difficultyLevel.name().toLowerCase(Locale.ROOT));
        }

        if (encoding != null && encoding.getQueryValue() != null) {
            builder.queryParam("encode", encoding.getQueryValue());
        }

        if (StringUtils.hasText(sessionToken)) {
            builder.queryParam("token", sessionToken);
        }

        String url = builder.toUriString();

        try {
            ResponseEntity<OpenTriviaApiResponse> response = restTemplate.getForEntity(url, OpenTriviaApiResponse.class);
            OpenTriviaApiResponse body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Empty response from OpenTrivia API");
            }
            handleResponseCode(body.responseCode());
            return Optional.ofNullable(body.results()).orElse(Collections.emptyList());
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to fetch questions from OpenTrivia", ex);
        }
    }

    public String requestSessionToken() {
        String url = UriComponentsBuilder.fromUriString(baseUrl + "/api_token.php")
                .queryParam("command", "request")
                .toUriString();
        return executeTokenCommand(url, "request");
    }

    public String resetSessionToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Token is required to reset the OpenTrivia session");
        }
        String url = UriComponentsBuilder.fromUriString(baseUrl + "/api_token.php")
                .queryParam("command", "reset")
                .queryParam("token", token)
                .toUriString();
        return executeTokenCommand(url, "reset");
    }

    @SuppressWarnings("null")
    private String executeTokenCommand(String url, String commandName) {
        try {
            ResponseEntity<OpenTriviaTokenApiResponse> response = restTemplate.getForEntity(url,
                    OpenTriviaTokenApiResponse.class);
            OpenTriviaTokenApiResponse body = response.getBody();
            if (body == null) {
                throw new IllegalStateException("Empty response when trying to " + commandName + " session token");
            }
            handleResponseCode(body.responseCode());
            if (!StringUtils.hasText(body.token())) {
                throw new IllegalStateException("OpenTrivia did not return a valid session token");
            }
            return body.token();
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to " + commandName + " OpenTrivia session token", ex);
        }
    }

    private void handleResponseCode(int responseCode) {
        if (responseCode == 0) {
            return;
        }
        switch (responseCode) {
            case 1 -> throw new IllegalStateException(
                    "OpenTrivia did not return any questions for the chosen filters. Try loosening the filters.");
            case 2 -> throw new IllegalArgumentException("Invalid parameters were sent to the OpenTrivia API.");
            case 3 -> throw new IllegalStateException(
                    "Session token not found. Request a new token before performing another import.");
            case 4 -> throw new IllegalStateException(
                    "Session token exhausted. Reset the token to clear the question history.");
            case 5 -> throw new IllegalStateException(
                    "OpenTrivia rate limit reached. Please wait a few seconds before retrying.");
            default -> throw new IllegalStateException("OpenTrivia API responded with unknown code " + responseCode);
        }
    }

    public record TriviaCategoryListResponse(
            @JsonProperty("trivia_categories") List<OpenTriviaCategoryResponse> triviaCategories) {
    }

    public record OpenTriviaApiResponse(
            @JsonProperty("response_code") int responseCode,
            List<OpenTriviaQuestion> results) {
    }

    public record OpenTriviaQuestion(
            String category,
            String type,
            String difficulty,
            String question,
            @JsonProperty("correct_answer") String correctAnswer,
            @JsonProperty("incorrect_answers") List<String> incorrectAnswers) {
    }

    public record OpenTriviaTokenApiResponse(
            @JsonProperty("response_code") int responseCode,
            String token) {
    }
}
