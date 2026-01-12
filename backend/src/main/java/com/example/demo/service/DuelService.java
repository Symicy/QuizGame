package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.demo.domain.Question;
import com.example.demo.domain.Room;
import com.example.demo.domain.RoomParticipant;
import com.example.demo.domain.Session;
import com.example.demo.domain.Submission;
import com.example.demo.dto.duel.DuelAnswerRequest;
import com.example.demo.dto.duel.DuelAnswerResponse;
import com.example.demo.dto.duel.DuelQuestionRevealPayload;
import com.example.demo.dto.duel.DuelQuestionStartPayload;
import com.example.demo.dto.duel.DuelPlayerState;
import com.example.demo.dto.duel.DuelResultPayload;
import com.example.demo.dto.duel.DuelStateResponse;
import com.example.demo.dto.session.SessionResponse;
import com.example.demo.dto.session.SubmissionRequest;
import com.example.demo.dto.session.SubmissionResponse;
import com.example.demo.enums.RoomStatus;
import com.example.demo.enums.RoomType;
import com.example.demo.enums.SessionStatus;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repo.RoomParticipantRepo;
import com.example.demo.repo.RoomRepo;
import com.example.demo.repo.SessionRepo;
import com.example.demo.repo.SubmissionRepo;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DuelService {

    private final RoomRepo roomRepo;
    private final RoomParticipantRepo participantRepo;
    private final SessionRepo sessionRepo;
    private final SubmissionRepo submissionRepo;
    private final SessionService sessionService;
    private final GameEventPublisher eventPublisher;
    private final PlatformTransactionManager transactionManager;

    private static final long QUESTION_REVEAL_DELAY_MS = 2000L;
    private final ScheduledExecutorService questionScheduler = Executors.newScheduledThreadPool(1);
    private final Map<String, ScheduledFuture<?>> questionAdvanceTasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> questionTimeoutTasks = new ConcurrentHashMap<>();
    private final Map<String, Long> resolvedQuestionLocks = new ConcurrentHashMap<>();

    @PreDestroy
    public void shutdownScheduler() {
        questionScheduler.shutdownNow();
    }

    public void handleRoomStarted(Room room, List<RoomParticipant> participants) {
        if (room == null || !RoomType.DUEL.equals(room.getRoomType())) {
            return;
        }
        if (participants == null || participants.size() != 2) {
            throw new IllegalStateException("Duel rooms require exactly two active participants");
        }
        if (room.getQuestions() == null || room.getQuestions().isEmpty()) {
            throw new IllegalStateException("Duel rooms must have questions configured");
        }
        cancelScheduledAdvance(room.getCode());
        cancelQuestionTimeout(room.getCode());
        resolvedQuestionLocks.remove(room.getCode());
        participants.forEach(participant -> {
            participant.setCurrentScore(0);
            participant.setCorrectAnswers(0);
            participant.setFinalRank(null);
            participant.setIsReady(false);
            participantRepo.save(participant);
            sessionService.createRoomSession(room, participant.getUser());
        });
        publishScoreboard(room);
        int startIndex = room.getCurrentQuestionIndex() != null ? room.getCurrentQuestionIndex() : 0;
        publishQuestionStart(room, startIndex);
    }

    public DuelStateResponse getState(String code, Long userId) {
        Room room = getRoomByCode(code);
        return buildState(room, userId);
    }

    public DuelAnswerResponse submitAnswer(String code, DuelAnswerRequest request) {
        Objects.requireNonNull(request, "request");
        Room room = getRoomByCode(code);
        if (!RoomStatus.IN_PROGRESS.equals(room.getStatus())) {
            throw new IllegalStateException("Duel is not active");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("User id is required");
        }
        RoomParticipant participant = participantRepo.findByRoomIdAndUserId(room.getId(), request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User is not part of this duel"));
        Session session = findLatestSession(room, participant.getUser().getId())
                .orElseThrow(() -> new IllegalStateException("Active duel session not found"));

        Long lockedQuestionId = resolvedQuestionLocks.get(room.getCode());
        if (lockedQuestionId != null && lockedQuestionId.equals(request.getQuestionId())) {
            throw new IllegalStateException("Question already closed");
        }

        SubmissionRequest submissionRequest = new SubmissionRequest();
        submissionRequest.setQuestionId(request.getQuestionId());
        submissionRequest.setAnswerIndex(request.getAnswerIndex());
        SubmissionResponse submission = sessionService.submitAnswer(session.getId(), submissionRequest);

        participant.setCurrentScore(submission.getTotalScore());
        participant.setCorrectAnswers(submission.getCorrectAnswers());
        participantRepo.save(participant);

        publishScoreboard(room);
        handleQuestionProgress(room, findRoomQuestion(room, request.getQuestionId()));
        maybeFinalizeMatch(room);

        SessionResponse sessionResponse = sessionService.getSession(session.getId());
        List<DuelPlayerState> leaderboard = buildLeaderboard(room);
        boolean duelCompleted = RoomStatus.FINISHED.equals(room.getStatus());
        Long winnerUserId = duelCompleted ? resolveWinnerId(participantRepo.findByRoomId(room.getId())) : null;

        return DuelAnswerResponse.builder()
                .accepted(true)
                .submission(submission)
                .session(sessionResponse)
                .leaderboard(leaderboard)
                .duelCompleted(duelCompleted)
                .winnerUserId(winnerUserId)
                .build();
    }

    public void handlePlayerLeave(Room room, RoomParticipant participant) {
        if (room == null || !RoomType.DUEL.equals(room.getRoomType())) {
            return;
        }
        if (participant == null) {
            return;
        }

        if (RoomStatus.IN_PROGRESS.equals(room.getStatus())) {
            Long opponentUserId = participantRepo.findByRoomId(room.getId()).stream()
                    .filter(p -> p.getUser() != null && !p.getUser().getId().equals(participant.getUser().getId()))
                    .filter(p -> Boolean.TRUE.equals(p.getIsActive()))
                    .map(p -> p.getUser().getId())
                    .findFirst()
                    .orElse(null);
            finishMatch(room, participantRepo.findByRoomId(room.getId()), opponentUserId);
            return;
        }
        publishScoreboard(room);
    }

    public void forceFinish(Room room) {
        if (room == null || !RoomType.DUEL.equals(room.getRoomType())) {
            return;
        }
        finishMatch(room, participantRepo.findByRoomId(room.getId()), null);
    }

    private DuelStateResponse buildState(Room room, Long userId) {
        List<DuelPlayerState> leaderboard = buildLeaderboard(room);
        DuelPlayerState player = findPlayer(leaderboard, userId);
        DuelPlayerState opponent = leaderboard.stream()
                .filter(entry -> player == null || !entry.getUserId().equals(player.getUserId()))
                .findFirst()
                .orElse(null);

        SessionResponse sessionResponse = null;
        if (player != null && player.getSessionId() != null) {
            sessionResponse = sessionService.getSession(player.getSessionId());
        }

        boolean duelCompleted = RoomStatus.FINISHED.equals(room.getStatus())
                || leaderboard.stream().allMatch(entry -> SessionStatus.COMPLETED.equals(entry.getSessionStatus()));
        Long winnerUserId = resolveWinnerId(participantRepo.findByRoomId(room.getId()));

        return DuelStateResponse.builder()
                .roomCode(room.getCode())
                .roomStatus(room.getStatus())
                .questionCount(room.getQuestionCount())
                .timePerQuestion(room.getTimePerQuestion())
            .currentQuestionIndex(room.getCurrentQuestionIndex())
                .startedAt(room.getRoundStartedAt())
                .finishedAt(room.getClosedAt())
                .session(sessionResponse)
                .leaderboard(leaderboard)
                .player(player)
                .opponent(opponent)
                .duelActive(RoomStatus.IN_PROGRESS.equals(room.getStatus()))
                .duelCompleted(duelCompleted)
                .winnerUserId(winnerUserId)
                .build();
    }

    private List<DuelPlayerState> buildLeaderboard(Room room) {
        return participantRepo.findByRoomId(room.getId()).stream()
                .map(participant -> toPlayerState(participant, room))
                .sorted(Comparator.comparingInt((DuelPlayerState state) -> state.getScore() != null ? state.getScore() : 0)
                        .reversed())
                .toList();
    }

    private DuelPlayerState toPlayerState(RoomParticipant participant, Room room) {
        if (participant.getUser() == null) {
            return DuelPlayerState.builder()
                    .active(participant.getIsActive())
                    .ready(participant.getIsReady())
                    .finalRank(participant.getFinalRank())
                    .score(participant.getCurrentScore())
                    .correctAnswers(participant.getCorrectAnswers())
                    .build();
        }
        Optional<Session> sessionOpt = findLatestSession(room, participant.getUser().getId());
        Session session = sessionOpt.orElse(null);
        Integer totalQuestions = session != null ? session.getTotalQuestions() : room.getQuestionCount();
        int answered = session != null ? Math.toIntExact(submissionRepo.countBySessionId(session.getId())) : 0;

        return DuelPlayerState.builder()
                .userId(participant.getUser().getId())
                .username(participant.getUser().getUsername())
                .score(participant.getCurrentScore() != null ? participant.getCurrentScore() : 0)
                .correctAnswers(participant.getCorrectAnswers() != null ? participant.getCorrectAnswers() : 0)
                .answeredQuestions(answered)
                .totalQuestions(totalQuestions)
                .active(Boolean.TRUE.equals(participant.getIsActive()))
                .ready(Boolean.TRUE.equals(participant.getIsReady()))
                .sessionStatus(session != null ? session.getStatus() : null)
                .sessionId(session != null ? session.getId() : null)
                .finalRank(participant.getFinalRank())
                .build();
    }

    private Optional<Session> findLatestSession(Room room, Long userId) {
        if (room == null || userId == null) {
            return Optional.empty();
        }
        Optional<Session> active = sessionRepo
                .findTopByRoomIdAndUserIdAndStatusOrderByCreatedAtDesc(room.getId(), userId, SessionStatus.IN_PROGRESS);
        if (active.isPresent()) {
            return active;
        }
        return sessionRepo.findTopByRoomIdAndUserIdOrderByCreatedAtDesc(room.getId(), userId);
    }

    private void publishScoreboard(Room room) {
        eventPublisher.publishCustomRoomEvent(room.getCode(), "DUEL_SCORE_UPDATE", buildLeaderboard(room));
    }

    private DuelPlayerState findPlayer(List<DuelPlayerState> leaderboard, Long userId) {
        if (userId == null || leaderboard == null) {
            return null;
        }
        return leaderboard.stream()
                .filter(entry -> userId.equals(entry.getUserId()))
                .findFirst()
                .orElse(null);
    }

    private void maybeFinalizeMatch(Room room) {
        if (!RoomStatus.IN_PROGRESS.equals(room.getStatus())) {
            return;
        }
        if (questionAdvanceTasks.containsKey(room.getCode())) {
            return;
        }
        List<RoomParticipant> activeParticipants = participantRepo.findByRoomIdAndIsActiveTrue(room.getId());
        if (activeParticipants.size() != 2) {
            return;
        }
        boolean allCompleted = activeParticipants.stream()
                .map(participant -> findLatestSession(room, participant.getUser().getId()).orElse(null))
                .allMatch(session -> session != null && SessionStatus.COMPLETED.equals(session.getStatus()));
        if (allCompleted) {
            finishMatch(room, participantRepo.findByRoomId(room.getId()), null);
        }
    }

    private void finishMatch(Room room, List<RoomParticipant> participants, Long forcedWinnerUserId) {
        if (room == null || !RoomType.DUEL.equals(room.getRoomType())) {
            return;
        }
        if (participants == null || participants.isEmpty()) {
            return;
        }
        if (RoomStatus.FINISHED.equals(room.getStatus())) {
            return;
        }

        cancelScheduledAdvance(room.getCode());

        sessionRepo.findByRoomIdAndStatus(room.getId(), SessionStatus.IN_PROGRESS)
                .forEach(activeSession -> sessionService.completeSession(activeSession.getId()));

        RoomParticipant winner = determineWinner(participants, room, forcedWinnerUserId);
        participants.forEach(participant -> {
            if (participant.getUser() == null) {
                participant.setFinalRank(1);
            } else if (winner == null) {
                participant.setFinalRank(1);
            } else if (participant.getUser().getId().equals(winner.getUser().getId())) {
                participant.setFinalRank(1);
            } else {
                participant.setFinalRank(2);
            }
            participant.setIsReady(false);
            participantRepo.save(participant);
        });

        room.setStatus(RoomStatus.FINISHED);
        room.setClosedAt(LocalDateTime.now());
        room.setRoundStartedAt(null);
        room.setCurrentQuestionIndex(0);
        roomRepo.save(room);

        List<DuelPlayerState> leaderboard = buildLeaderboard(room);
        DuelResultPayload payload = DuelResultPayload.builder()
                .winnerUserId(winner != null && winner.getUser() != null ? winner.getUser().getId() : null)
                .finishedAt(room.getClosedAt())
                .leaderboard(leaderboard)
                .build();
        eventPublisher.publishCustomRoomEvent(room.getCode(), "DUEL_FINISHED", payload);
        eventPublisher.publishRoomUpdate(room, participantRepo.findByRoomId(room.getId()));
    }

    private RoomParticipant determineWinner(List<RoomParticipant> participants, Room room, Long forcedWinnerUserId) {
        if (forcedWinnerUserId != null) {
            return participants.stream()
                    .filter(participant -> participant.getUser() != null && forcedWinnerUserId.equals(participant.getUser().getId()))
                    .findFirst()
                    .orElse(null);
        }
        return participants.stream()
                .filter(participant -> participant.getUser() != null)
                .max((left, right) -> {
                    int leftScore = left.getCurrentScore() != null ? left.getCurrentScore() : 0;
                    int rightScore = right.getCurrentScore() != null ? right.getCurrentScore() : 0;
                    if (leftScore != rightScore) {
                        return Integer.compare(leftScore, rightScore);
                    }
                    Optional<Session> leftSession = findLatestSession(room, left.getUser().getId());
                    Optional<Session> rightSession = findLatestSession(room, right.getUser().getId());
                    int leftTime = leftSession.isPresent() && leftSession.get().getDurationSeconds() != null
                            ? leftSession.get().getDurationSeconds()
                            : Integer.MAX_VALUE;
                    int rightTime = rightSession.isPresent() && rightSession.get().getDurationSeconds() != null
                            ? rightSession.get().getDurationSeconds()
                            : Integer.MAX_VALUE;
                    return Integer.compare(-leftTime, -rightTime);
                })
                .orElse(null);
    }

    private Long resolveWinnerId(List<RoomParticipant> participants) {
        return participants.stream()
                .filter(participant -> participant.getFinalRank() != null && participant.getFinalRank() == 1)
                .filter(participant -> participant.getUser() != null)
                .map(participant -> participant.getUser().getId())
                .findFirst()
                .orElse(null);
    }

    private void handleQuestionProgress(Room room, Question question) {
        if (room == null || question == null || !RoomStatus.IN_PROGRESS.equals(room.getStatus())) {
            return;
        }
        if (!everyoneAnsweredQuestion(room, question)) {
            return;
        }
        cancelQuestionTimeout(room.getCode());
        List<DuelQuestionRevealPayload.PlayerReveal> playerStates = buildPlayerReveal(room, question);
        boolean everyoneSubmitted = playerStates.stream()
                .allMatch(state -> Boolean.TRUE.equals(state.getAnswered()));
        if (!everyoneSubmitted) {
            return;
        }
        resolvedQuestionLocks.put(room.getCode(), question.getId());
        publishQuestionReveal(room, question, playerStates);
        int questionIndex = resolveQuestionIndex(room, question);
        room.setCurrentQuestionIndex(questionIndex);
        roomRepo.save(room);
        schedulePostRevealAdvance(room.getCode(), questionIndex + 1);
    }

    private Question findRoomQuestion(Room room, Long questionId) {
        if (room == null || questionId == null || room.getQuestions() == null) {
            return null;
        }
        return room.getQuestions().stream()
                .filter(candidate -> questionId.equals(candidate.getId()))
                .findFirst()
                .orElse(null);
    }

    private boolean everyoneAnsweredQuestion(Room room, Question question) {
        if (room == null || question == null) {
            return false;
        }
        List<RoomParticipant> activeParticipants = participantRepo.findByRoomIdAndIsActiveTrue(room.getId());
        if (activeParticipants.size() < 2) {
            return false;
        }
        return activeParticipants.stream()
                .filter(participant -> participant.getUser() != null)
                .allMatch(participant -> {
                    Optional<Session> sessionOpt = findLatestSession(room, participant.getUser().getId());
                    return sessionOpt.isPresent()
                            && submissionRepo.existsBySessionIdAndQuestionId(sessionOpt.get().getId(), question.getId());
                });
    }

    private int resolveQuestionIndex(Room room, Question question) {
        if (room == null || question == null || room.getQuestions() == null) {
            return room != null && room.getCurrentQuestionIndex() != null ? room.getCurrentQuestionIndex() : 0;
        }
        for (int index = 0; index < room.getQuestions().size(); index++) {
            if (Objects.equals(room.getQuestions().get(index).getId(), question.getId())) {
                return index;
            }
        }
        return room.getCurrentQuestionIndex() != null ? room.getCurrentQuestionIndex() : 0;
    }

    private int resolveTotalQuestions(Room room) {
        if (room == null) {
            return 0;
        }
        if (room.getQuestionCount() != null) {
            return room.getQuestionCount();
        }
        return room.getQuestions() != null ? room.getQuestions().size() : 0;
    }

    private void publishQuestionReveal(Room room, Question question, List<DuelQuestionRevealPayload.PlayerReveal> playerStates) {
        DuelQuestionRevealPayload payload = DuelQuestionRevealPayload.builder()
                .questionId(question.getId())
                .correctOption(question.getCorrectOption())
                .leaderboard(buildLeaderboard(room))
                .revealedAt(LocalDateTime.now())
                .playerChoices(playerStates)
                .build();
        eventPublisher.publishCustomRoomEvent(room.getCode(), "DUEL_QUESTION_REVEAL", payload);
    }

    private List<DuelQuestionRevealPayload.PlayerReveal> buildPlayerReveal(Room room, Question question) {
        if (room == null || question == null) {
            return List.of();
        }
        return participantRepo.findByRoomId(room.getId()).stream()
                .filter(participant -> participant.getUser() != null)
                .map(participant -> {
                    Long userId = participant.getUser().getId();
                    Optional<Session> sessionOpt = findLatestSession(room, userId);
                    if (sessionOpt.isEmpty()) {
                        return DuelQuestionRevealPayload.PlayerReveal.builder()
                                .userId(userId)
                                .answered(false)
                                .build();
                    }
                    Session session = sessionOpt.get();
                        Optional<Submission> submissionOpt = submissionRepo.findBySessionIdAndQuestionId(session.getId(), question.getId());
                        if (submissionOpt.isEmpty()) {
                        return DuelQuestionRevealPayload.PlayerReveal.builder()
                                .userId(userId)
                                .answered(false)
                                .build();
                    }
                    Submission submission = submissionOpt.get();
                    return DuelQuestionRevealPayload.PlayerReveal.builder()
                            .userId(userId)
                            .selectedOption(submission.getAnswerIndex())
                            .correct(Boolean.TRUE.equals(submission.getIsCorrect()))
                            .answered(true)
                            .build();
                })
                .toList();
    }

    private void schedulePostRevealAdvance(String roomCode, int nextQuestionIndex) {
        if (roomCode == null) {
            return;
        }
        cancelScheduledAdvance(roomCode);
        ScheduledFuture<?> future = questionScheduler.schedule(
                () -> runInTransaction(() -> advanceAfterReveal(roomCode, nextQuestionIndex)),
                QUESTION_REVEAL_DELAY_MS,
                TimeUnit.MILLISECONDS);
        questionAdvanceTasks.put(roomCode, future);
    }

    private void advanceAfterReveal(String roomCode, int nextQuestionIndex) {
        Room latestRoom = getRoomByCode(roomCode);
        if (!RoomStatus.IN_PROGRESS.equals(latestRoom.getStatus())) {
            questionAdvanceTasks.remove(roomCode);
            return;
        }
        List<RoomParticipant> participants = participantRepo.findByRoomId(latestRoom.getId());
        int totalQuestions = resolveTotalQuestions(latestRoom);

        if (nextQuestionIndex >= totalQuestions) {
            questionAdvanceTasks.remove(roomCode);
            finishMatch(latestRoom, participants, null);
            return;
        }

        latestRoom.setCurrentQuestionIndex(nextQuestionIndex);
        roomRepo.save(latestRoom);

        publishQuestionStart(latestRoom, nextQuestionIndex);
        eventPublisher.publishRoomUpdate(latestRoom, participants);
        questionAdvanceTasks.remove(roomCode);
    }

    private void scheduleQuestionTimeout(String roomCode, int questionIndex, Integer timePerQuestion) {
        if (roomCode == null) {
            return;
        }
        cancelQuestionTimeout(roomCode);
        if (timePerQuestion == null || timePerQuestion <= 0) {
            return;
        }
        ScheduledFuture<?> future = questionScheduler.schedule(
                () -> runInTransaction(() -> handleQuestionTimeout(roomCode, questionIndex)),
            Math.max(1, timePerQuestion.longValue()),
            TimeUnit.SECONDS);
        questionTimeoutTasks.put(roomCode, future);
    }

    private void cancelQuestionTimeout(String roomCode) {
        if (roomCode == null) {
            return;
        }
        ScheduledFuture<?> future = questionTimeoutTasks.remove(roomCode);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void handleQuestionTimeout(String roomCode, int questionIndex) {
        questionTimeoutTasks.remove(roomCode);
        Room room = getRoomByCode(roomCode);
        if (!RoomStatus.IN_PROGRESS.equals(room.getStatus())) {
            return;
        }
        int currentIndex = room.getCurrentQuestionIndex() != null ? room.getCurrentQuestionIndex() : 0;
        if (currentIndex != questionIndex) {
            return;
        }
        Question question = getQuestionByIndex(room, questionIndex);
        if (question == null) {
            schedulePostRevealAdvance(roomCode, questionIndex + 1);
            return;
        }
        Long lockedQuestionId = resolvedQuestionLocks.get(roomCode);
        if (lockedQuestionId != null && lockedQuestionId.equals(question.getId())) {
            return;
        }
        List<DuelQuestionRevealPayload.PlayerReveal> playerStates = buildPlayerReveal(room, question);
        resolvedQuestionLocks.put(roomCode, question.getId());
        publishQuestionReveal(room, question, playerStates);
        schedulePostRevealAdvance(roomCode, questionIndex + 1);
    }

    private void publishQuestionStart(Room room, int questionIndex) {
        if (room == null) {
            return;
        }
        int safeIndex = Math.max(0, questionIndex);
        Question question = getQuestionByIndex(room, safeIndex);
        DuelQuestionStartPayload payload = DuelQuestionStartPayload.builder()
                .questionId(question != null ? question.getId() : null)
                .questionIndex(safeIndex)
                .totalQuestions(resolveTotalQuestions(room))
                .startsAt(LocalDateTime.now())
                .build();
        eventPublisher.publishCustomRoomEvent(room.getCode(), "DUEL_QUESTION_START", payload);
        resolvedQuestionLocks.remove(room.getCode());
        scheduleQuestionTimeout(room.getCode(), safeIndex, room.getTimePerQuestion());
    }

    private Question getQuestionByIndex(Room room, int index) {
        if (room == null || room.getQuestions() == null) {
            return null;
        }
        if (index < 0 || index >= room.getQuestions().size()) {
            return null;
        }
        return room.getQuestions().get(index);
    }

    private void runInTransaction(Runnable action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> action.run());
    }

    private void cancelScheduledAdvance(String roomCode) {
        if (roomCode == null) {
            return;
        }
        ScheduledFuture<?> future = questionAdvanceTasks.remove(roomCode);
        if (future != null) {
            future.cancel(false);
        }
    }

    private Room getRoomByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Room code is required");
        }
        return roomRepo.findByCode(code.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Room with code %s not found".formatted(code)));
    }
}
