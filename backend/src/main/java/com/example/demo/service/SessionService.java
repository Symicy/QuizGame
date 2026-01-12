package com.example.demo.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.Category;
import com.example.demo.domain.Question;
import com.example.demo.domain.Quiz;
import com.example.demo.domain.Room;
import com.example.demo.domain.Session;
import com.example.demo.domain.Submission;
import com.example.demo.domain.User;
import com.example.demo.dto.session.SessionResponse;
import com.example.demo.dto.session.SessionStartRequest;
import com.example.demo.dto.session.SubmissionRequest;
import com.example.demo.dto.session.SubmissionResponse;
import com.example.demo.enums.DifficultyLevel;
import com.example.demo.enums.SessionStatus;
import com.example.demo.enums.SessionType;
import com.example.demo.enums.RoomType;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repo.CategoryRepo;
import com.example.demo.repo.QuestionRepo;
import com.example.demo.repo.QuizRepo;
import com.example.demo.repo.SessionRepo;
import com.example.demo.repo.SubmissionRepo;
import com.example.demo.repo.UserRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SessionService {

    private static final int DEFAULT_QUESTION_COUNT = 10;

    private final SessionRepo sessionRepo;
    private final SubmissionRepo submissionRepo;
    private final UserRepo userRepo;
    private final QuizRepo quizRepo;
    private final CategoryRepo categoryRepo;
    private final QuestionRepo questionRepo;
    private final ScoreService scoreService;

    public SessionResponse startSoloSession(SessionStartRequest request) {
        User user = getUser(request.getUserId());
        Quiz quiz = resolveQuiz(request.getQuizId());
        Category category = resolveCategory(request.getCategoryId(), quiz);
        DifficultyLevel difficulty = resolveDifficulty(request.getDifficulty(), quiz);

        int requestedQuestions = request.getQuestionCount() != null
                ? request.getQuestionCount()
                : determineDefaultCount(quiz);

        List<Question> questions = resolveQuestions(quiz, category, difficulty, requestedQuestions);

        Session session = new Session();
        session.setSessionType(SessionType.SINGLE_PLAYER);
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setUser(user);
        session.setQuiz(quiz);
        session.setCategory(category);
        session.setDifficulty(difficulty);
        session.setQuestions(new ArrayList<>(questions));
        session.setTotalQuestions(questions.size());
        session.setFinalScore(0);
        session.setCorrectAnswers(0);
        session.setDurationSeconds(0);

        Session saved = sessionRepo.save(session);
        return SessionResponse.fromEntity(saved, List.of());
    }

    @Transactional(readOnly = true)
    public SessionResponse getSession(Long id) {
        Session session = getSessionEntity(id);
        List<Submission> submissions = submissionRepo.findBySessionId(session.getId());
        return SessionResponse.fromEntity(session, submissions);
    }

    public SubmissionResponse submitAnswer(Long sessionId, SubmissionRequest request) {
        Session session = getSessionEntity(sessionId);
        ensureSessionInProgress(session);

        Question question = getQuestionFromSession(session, request.getQuestionId());
        if (submissionRepo.existsBySessionIdAndQuestionId(sessionId, question.getId())) {
            throw new IllegalArgumentException("Question already answered");
        }

        boolean correct = question.getCorrectOption().equals(request.getAnswerIndex());
        int pointsEarned = correct ? resolvePoints(question) : 0;

        Submission submission = new Submission();
        submission.setSession(session);
        submission.setQuestion(question);
        submission.setUser(session.getUser());
        submission.setAnswerIndex(request.getAnswerIndex());
        submission.setIsCorrect(correct);
        submission.setPointsEarned(pointsEarned);
        submission.setResponseTimeMs(request.getResponseTimeMs());
        submissionRepo.save(submission);
        submissionRepo.flush();

        session.setFinalScore((session.getFinalScore() != null ? session.getFinalScore() : 0) + pointsEarned);
        if (correct) {
            session.setCorrectAnswers((session.getCorrectAnswers() != null ? session.getCorrectAnswers() : 0) + 1);
        }

        long answered = submissionRepo.countBySessionId(sessionId);
        if (answered >= session.getTotalQuestions()) {
            completeSessionInternal(session);
        }

        sessionRepo.save(session);
        sessionRepo.flush();

        return SubmissionResponse.builder()
                .sessionId(session.getId())
                .questionId(question.getId())
                .correct(correct)
                .pointsEarned(pointsEarned)
                .totalScore(session.getFinalScore())
                .correctAnswers(session.getCorrectAnswers())
                .answeredQuestions((int) answered)
                .totalQuestions(session.getTotalQuestions())
                .build();
    }

    public SessionResponse completeSession(Long sessionId) {
        Session session = getSessionEntity(sessionId);
        completeSessionInternal(session);
        Session saved = sessionRepo.save(session);
        List<Submission> submissions = submissionRepo.findBySessionId(session.getId());
        return SessionResponse.fromEntity(saved, submissions);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getRecentSessions(Long userId) {
        List<Session> sessions = sessionRepo.findTop5ByUserIdOrderByCreatedAtDesc(userId);
        return sessions.stream()
                .map(session -> SessionResponse.fromEntity(session, submissionRepo.findBySessionId(session.getId())))
                .toList();
    }

    public Session createRoomSession(Room room, User user) {
        if (room == null) {
            throw new IllegalArgumentException("Room is required");
        }
        if (user == null) {
            throw new IllegalArgumentException("User is required");
        }
        List<Question> questions = room.getQuestions();
        if (questions == null || questions.isEmpty()) {
            throw new IllegalStateException("Room does not have questions configured");
        }

        Session session = new Session();
        SessionType type = RoomType.DUEL.equals(room.getRoomType()) ? SessionType.DUEL : SessionType.LOBBY;
        session.setSessionType(type);
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setUser(user);
        session.setRoom(room);
        session.setQuiz(room.getQuiz());
        session.setCategory(room.getCategory());
        session.setDifficulty(room.getDifficulty());
        session.setQuestions(new ArrayList<>(questions));
        session.setTotalQuestions(questions.size());
        session.setFinalScore(0);
        session.setCorrectAnswers(0);
        session.setDurationSeconds(0);
        return sessionRepo.save(session);
    }

    private void completeSessionInternal(Session session) {
        if (SessionStatus.COMPLETED.equals(session.getStatus())) {
            return;
        }
        session.setStatus(SessionStatus.COMPLETED);
        session.setDurationSeconds(calculateDurationSeconds(session.getCreatedAt(), LocalDateTime.now()));
        scoreService.recordScoreFromSession(session);
    }

    private int calculateDurationSeconds(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return (int) Duration.between(start, end).toSeconds();
    }

    private Session getSessionEntity(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Session id is required");
        }
        return sessionRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session", id));
    }

    private void ensureSessionInProgress(Session session) {
        if (!SessionStatus.IN_PROGRESS.equals(session.getStatus())) {
            throw new IllegalStateException("Session is not active");
        }
    }

    private Question getQuestionFromSession(Session session, Long questionId) {
        return session.getQuestions().stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Question does not belong to session"));
    }

    private int resolvePoints(Question question) {
        if (question.getPoints() != null) {
            return question.getPoints();
        }
        return switch (question.getDifficultyLevel()) {
            case EASY -> 10;
            case MEDIUM -> 20;
            case HARD -> 30;
        };
    }

    private int determineDefaultCount(Quiz quiz) {
        if (quiz != null && quiz.getQuestions() != null && !quiz.getQuestions().isEmpty()) {
            return quiz.getQuestions().size();
        }
        return DEFAULT_QUESTION_COUNT;
    }

    private List<Question> resolveQuestions(Quiz quiz, Category category, DifficultyLevel difficulty, int requestedCount) {
        if (quiz != null) {
            List<Question> quizQuestions = quiz.getQuestions();
            if (quizQuestions == null || quizQuestions.isEmpty()) {
                throw new IllegalArgumentException("Quiz has no questions");
            }
            if (requestedCount != quizQuestions.size()) {
                throw new IllegalArgumentException("Requested question count must match quiz size (%d)".formatted(quizQuestions.size()));
            }
            return new ArrayList<>(quizQuestions);
        }

        Pageable pageable = PageRequest.of(0, requestedCount);
        Long categoryId = category != null ? category.getId() : null;
        List<Question> questions = questionRepo.findRandomActiveQuestions(categoryId, difficulty, pageable);
        if (questions.size() < requestedCount) {
            throw new IllegalArgumentException("Not enough questions available");
        }
        return questions;
    }

    private Quiz resolveQuiz(Long quizId) {
        if (quizId == null) {
            return null;
        }
        return quizRepo.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", quizId));
    }

    private Category resolveCategory(Long categoryId, Quiz quiz) {
        if (quiz != null) {
            return quiz.getCategory();
        }
        if (categoryId == null) {
            return null;
        }
        return categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }

    private DifficultyLevel resolveDifficulty(DifficultyLevel requested, Quiz quiz) {
        if (requested != null) {
            return requested;
        }
        if (quiz != null && quiz.getDifficulty() != null) {
            return quiz.getDifficulty();
        }
        return DifficultyLevel.MEDIUM;
    }

    private User getUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id is required");
        }
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
