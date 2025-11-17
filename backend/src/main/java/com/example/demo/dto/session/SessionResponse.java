package com.example.demo.dto.session;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.demo.domain.Question;
import com.example.demo.domain.Session;
import com.example.demo.domain.Submission;
import com.example.demo.enums.DifficultyLevel;
import com.example.demo.enums.SessionStatus;
import com.example.demo.enums.SessionType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionResponse {

    private Long id;
    private SessionType sessionType;
    private SessionStatus status;
    private Integer finalScore;
    private Integer correctAnswers;
    private Integer totalQuestions;
    private Integer durationSeconds;
    private DifficultyLevel difficulty;
    private Long userId;
    private Long quizId;
    private String quizTitle;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
    private List<SessionQuestionResponse> questions;
    private Set<Long> answeredQuestionIds;

    public static SessionResponse fromEntity(Session session, List<Submission> submissions) {
        if (session == null) {
            return null;
        }

        Map<Long, Submission> submissionByQuestion = submissions != null
                ? submissions.stream().collect(Collectors.toMap(s -> s.getQuestion().getId(), s -> s, (a, b) -> a))
                : Map.of();

        List<SessionQuestionResponse> questionResponses = session.getQuestions() != null
                ? session.getQuestions().stream()
                        .map(question -> SessionQuestionResponse.fromEntity(question, submissionByQuestion.containsKey(question.getId())))
                        .toList()
                : List.of();

        Set<Long> answered = submissionByQuestion.keySet();

        return SessionResponse.builder()
                .id(session.getId())
                .sessionType(session.getSessionType())
                .status(session.getStatus())
                .finalScore(session.getFinalScore())
                .correctAnswers(session.getCorrectAnswers())
                .totalQuestions(session.getTotalQuestions())
                .durationSeconds(session.getDurationSeconds())
                .difficulty(session.getDifficulty())
                .userId(session.getUser() != null ? session.getUser().getId() : null)
                .quizId(session.getQuiz() != null ? session.getQuiz().getId() : null)
                .quizTitle(session.getQuiz() != null ? session.getQuiz().getTitle() : null)
                .categoryId(session.getCategory() != null ? session.getCategory().getId() : null)
                .categoryName(session.getCategory() != null ? session.getCategory().getName() : null)
                .createdAt(session.getCreatedAt())
                .questions(questionResponses)
                .answeredQuestionIds(answered)
                .build();
    }
}
