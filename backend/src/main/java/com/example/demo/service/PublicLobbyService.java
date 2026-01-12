package com.example.demo.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.Question;
import com.example.demo.domain.Room;
import com.example.demo.domain.RoomParticipant;
import com.example.demo.dto.lobby.LobbyAnswerRequest;
import com.example.demo.dto.lobby.LobbyAnswerResponse;
import com.example.demo.dto.lobby.LobbyCountdownPayload;
import com.example.demo.dto.lobby.LobbyLeaderboardEntry;
import com.example.demo.dto.lobby.LobbyQuestionPayload;
import com.example.demo.dto.lobby.LobbyQuestionResultPayload;
import com.example.demo.dto.lobby.LobbyQuestionView;
import com.example.demo.dto.lobby.LobbyRoundSummaryPayload;
import com.example.demo.dto.lobby.LobbyStateResponse;
import com.example.demo.dto.room.RoomCreateRequest;
import com.example.demo.dto.room.RoomPlayerRequest;
import com.example.demo.dto.room.RoomResponse;
import com.example.demo.enums.DifficultyLevel;
import com.example.demo.enums.LobbyPhase;
import com.example.demo.enums.RoomStatus;
import com.example.demo.enums.RoomType;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repo.QuestionRepo;
import com.example.demo.repo.RoomParticipantRepo;
import com.example.demo.repo.RoomRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicLobbyService {

    private final RoomService roomService;
    private final RoomRepo roomRepo;
    private final RoomParticipantRepo participantRepo;
    private final QuestionRepo questionRepo;
    private final GameEventPublisher eventPublisher;

    @Value("${app.lobby.max-players:30}")
    private int lobbyMaxPlayers;

    @Value("${app.lobby.min-players:2}")
    private int lobbyMinPlayers;

    @Value("${app.lobby.question-count:10}")
    private int lobbyQuestionCount;

    @Value("${app.lobby.question-duration-sec:25}")
    private int questionDurationSeconds;

    @Value("${app.lobby.countdown-sec:5}")
    private int countdownSeconds;

    @Value("${app.lobby.results-sec:10}")
    private int resultsSeconds;

    private final SecureRandom random = new SecureRandom();
    private final Object stateMonitor = new Object();
    private final LobbyRoundState state = new LobbyRoundState();

    @Transactional
    public RoomResponse joinLobby(RoomPlayerRequest request) {
        Room room = ensureLobbyRoom(request.getUserId());
        RoomResponse response = roomService.joinRoom(room.getCode(), request);
        synchronized (stateMonitor) {
            state.roomCode = response.getCode();
        }
        return response;
    }

    @Transactional
    public RoomResponse leaveLobby(RoomPlayerRequest request) {
        Room room = getLobbyRoomOrThrow();
        RoomResponse response = roomService.leaveRoom(room.getCode(), request);
        return response;
    }

    @Transactional(readOnly = true)
    public LobbyStateResponse getLobbyState(Long userId) {
        Room room = roomRepo.findFirstByRoomTypeOrderByIdAsc(RoomType.LOBBY).orElse(null);
        synchronized (stateMonitor) {
            if (room == null) {
                return LobbyStateResponse.builder()
                        .phase(LobbyPhase.WAITING)
                        .roundNumber(state.roundNumber)
                        .leaderboard(state.lastLeaderboard)
                        .build();
            }
            state.roomCode = room.getCode();
            List<RoomParticipant> activeParticipants = getActiveParticipants(room.getId());
                    LobbyQuestionView currentQuestion = resolveCurrentQuestionView();
            boolean playerAnswered = userId != null && hasUserAnsweredCurrentQuestion(userId);
            var leaderboard = buildLeaderboard(activeParticipants);
                var playerStats = resolvePlayerStats(room.getId(), userId, leaderboard);

            return LobbyStateResponse.builder()
                    .roomCode(room.getCode())
                    .phase(state.phase)
                    .roundNumber(state.roundNumber)
                    .currentQuestion(currentQuestion)
                    .currentQuestionIndex(state.getCurrentQuestionIndex())
                    .totalQuestions(state.questions.size())
                    .countdownEndsAt(state.countdownEndsAt)
                    .questionEndsAt(state.questionEndsAt)
                    .resultsEndsAt(state.resultsEndsAt)
                    .activePlayers(activeParticipants.size())
                    .playerAnswered(playerAnswered)
                    .playerScore(playerStats.score())
                    .playerRank(playerStats.rank())
                    .participant(playerStats.participant())
                    .leaderboard(leaderboard)
                    .build();
        }
    }

    @Transactional
    public LobbyAnswerResponse submitAnswer(LobbyAnswerRequest request) {
        Room room = getLobbyRoomOrThrow();
        RoomParticipant participant = participantRepo.findByRoomIdAndUserId(room.getId(), request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User is not part of the lobby"));

        synchronized (stateMonitor) {
            Integer currentCorrectOption = getCurrentQuestion()
                    .map(Question::getCorrectOption)
                    .orElse(null);
            if (!LobbyPhase.QUESTION.equals(state.phase)) {
                return LobbyAnswerResponse.builder()
                        .accepted(false)
                        .questionEndsAt(state.questionEndsAt)
                        .correctOption(currentCorrectOption)
                        .build();
            }
            Question currentQuestion = getCurrentQuestion().orElseThrow(() -> new IllegalStateException("No active question"));
            if (!currentQuestion.getId().equals(request.getQuestionId())) {
                throw new IllegalArgumentException("Question mismatch");
            }
            if (state.currentQuestionAnswers.containsKey(request.getUserId())) {
                return LobbyAnswerResponse.builder()
                        .accepted(false)
                        .questionEndsAt(state.questionEndsAt)
                        .correctOption(currentQuestion.getCorrectOption())
                        .build();
            }

            boolean correct = currentQuestion.getCorrectOption().equals(request.getAnswerIndex());
            int points = correct ? resolvePoints(currentQuestion) : 0;

            state.currentQuestionAnswers.put(request.getUserId(), request.getAnswerIndex());

            if (points > 0) {
                participant.addPoints(points);
                participant.incrementCorrectAnswers();
            } else {
                participant.addPoints(0);
            }
            participantRepo.save(participant);

            var leaderboard = buildLeaderboard(room.getId());
            state.lastLeaderboard = leaderboard;
            publishScoreUpdate(room.getCode(), leaderboard);

            return LobbyAnswerResponse.builder()
                    .accepted(true)
                    .correct(correct)
                    .pointsEarned(points)
                    .playerScore(participant.getCurrentScore())
                    .playerRank(resolvePlayerRank(leaderboard, participant.getUser().getId()))
                    .questionEndsAt(state.questionEndsAt)
                    .correctOption(currentQuestion.getCorrectOption())
                    .build();
        }
    }

    @Scheduled(fixedDelayString = "${app.lobby.tick-ms:1000}")
    @Transactional
    public void handleLobbyLoop() {
        Room room = roomRepo.findFirstByRoomTypeOrderByIdAsc(RoomType.LOBBY).orElse(null);
        if (room == null) {
            return;
        }
        synchronized (stateMonitor) {
            state.roomCode = room.getCode();
            List<RoomParticipant> activeParticipants = getActiveParticipants(room.getId());
            long activePlayers = activeParticipants.size();
            Instant now = Instant.now();

            switch (state.phase) {
                case WAITING -> {
                    if (activePlayers >= lobbyMinPlayers) {
                        startCountdown(room, activePlayers, now);
                    }
                }
                case COUNTDOWN -> {
                    if (activePlayers == 0) {
                        resetToWaiting(room);
                    } else if (state.countdownEndsAt != null && now.isAfter(state.countdownEndsAt)) {
                        startRound(room, now);
                    }
                }
                case QUESTION -> {
                    if (state.questionEndsAt != null && now.isAfter(state.questionEndsAt)) {
                        finishCurrentQuestion(room, now);
                    }
                }
                case RESULTS -> {
                    if (state.resultsEndsAt != null && now.isAfter(state.resultsEndsAt)) {
                        resetToWaiting(room);
                    }
                }
                default -> {
                }
            }
        }
    }

    private Room ensureLobbyRoom(Long ownerId) {
        Optional<Room> existing = roomRepo.findFirstByRoomTypeOrderByIdAsc(RoomType.LOBBY);
        if (existing.isPresent()) {
            return existing.get();
        }
        if (ownerId == null) {
            throw new IllegalArgumentException("Owner id required to create lobby");
        }
        RoomCreateRequest request = new RoomCreateRequest();
        request.setOwnerId(ownerId);
        request.setRoomType(RoomType.LOBBY);
        request.setMaxPlayers(lobbyMaxPlayers);
        request.setQuestionCount(lobbyQuestionCount);
        request.setTimePerQuestion(questionDurationSeconds);
        RoomResponse created = roomService.createRoom(request);
        Room room = roomRepo.findById(created.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Room", created.getId()));
        state.roomCode = room.getCode();
        return room;
    }

    private Room getLobbyRoomOrThrow() {
        return roomRepo.findFirstByRoomTypeOrderByIdAsc(RoomType.LOBBY)
                .orElseThrow(() -> new ResourceNotFoundException("Lobby room not available"));
    }

    private void startCountdown(Room room, long activePlayers, Instant now) {
        state.phase = LobbyPhase.COUNTDOWN;
        state.countdownEndsAt = now.plusSeconds(countdownSeconds);
        state.resultsEndsAt = null;
        state.questionEndsAt = null;
        publishCountdown(room.getCode(), activePlayers);
        log.debug("Lobby countdown started for room {}", room.getCode());
    }

    private void startRound(Room room, Instant now) {
        List<Question> questions = pickRandomQuestions(lobbyQuestionCount);
        state.phase = LobbyPhase.QUESTION;
        state.roundNumber += 1;
        state.questions = questions;
        state.currentQuestionIndex = -1;
        state.countdownEndsAt = null;
        state.resultsEndsAt = null;
        state.answersByQuestion.clear();
        state.currentQuestionAnswers.clear();
        state.lastLeaderboard = List.of();

        resetParticipantStats(room.getId());

        room.setStatus(RoomStatus.IN_PROGRESS);
        room.setQuestionCount(questions.size());
        room.setQuestions(new ArrayList<>(questions));
        room.setCurrentQuestionIndex(0);
        room.setRoundStartedAt(LocalDateTime.now());
        roomRepo.save(room);
        eventPublisher.publishRoomUpdate(room, participantRepo.findByRoomId(room.getId()));

        log.debug("Lobby round {} started with {} questions", state.roundNumber, questions.size());
        try {
            advanceToNextQuestion(room, now);
        } catch (Exception ex) {
            log.error("Failed to advance to first question", ex);
            resetToWaiting(room);
        }
    }

    private void advanceToNextQuestion(Room room, Instant now) {
        state.currentQuestionIndex += 1;
        if (state.currentQuestionIndex >= state.questions.size()) {
            concludeRound(room, now);
            return;
        }
        Question question = state.questions.get(state.currentQuestionIndex);
        state.currentQuestionAnswers.clear();
        state.questionEndsAt = now.plusSeconds(questionDurationSeconds);

        LobbyQuestionPayload payload = LobbyQuestionPayload.builder()
                .roundNumber(state.roundNumber)
                .questionIndex(state.currentQuestionIndex + 1)
                .totalQuestions(state.questions.size())
                .question(toQuestionView(question))
                .endsAt(state.questionEndsAt)
                .build();
        eventPublisher.publishCustomRoomEvent(room.getCode(), "LOBBY_QUESTION", payload);
    }

    private void finishCurrentQuestion(Room room, Instant now) {
        Question question = getCurrentQuestion().orElse(null);
        if (question != null) {
            state.answersByQuestion.computeIfAbsent(question.getId(), id -> new HashMap<>())
                    .putAll(state.currentQuestionAnswers);
            List<LobbyLeaderboardEntry> leaderboard = buildLeaderboard(room.getId());
            state.lastLeaderboard = leaderboard;

            LobbyQuestionResultPayload payload = LobbyQuestionResultPayload.builder()
                    .roundNumber(state.roundNumber)
                    .questionId(question.getId())
                    .correctOption(question.getCorrectOption())
                    .leaderboard(leaderboard)
                    .build();
            eventPublisher.publishCustomRoomEvent(room.getCode(), "LOBBY_QUESTION_RESULT", payload);
        }
        advanceToNextQuestion(room, now);
    }

    private void concludeRound(Room room, Instant now) {
        List<LobbyLeaderboardEntry> leaderboard = buildLeaderboard(room.getId());
        state.lastLeaderboard = leaderboard;
        List<LobbyLeaderboardEntry> topPlayers = leaderboard.stream().limit(3).toList();
        assignFinalRanks(room.getId(), topPlayers);

        LobbyRoundSummaryPayload summary = LobbyRoundSummaryPayload.builder()
                .roundNumber(state.roundNumber)
                .topPlayers(topPlayers)
                .build();
        eventPublisher.publishCustomRoomEvent(room.getCode(), "LOBBY_ROUND_ENDED", summary);

        room.setStatus(RoomStatus.WAITING);
        room.setRoundStartedAt(null);
        room.setCurrentQuestionIndex(0);
        roomRepo.save(room);
        state.phase = LobbyPhase.RESULTS;
        state.resultsEndsAt = now.plusSeconds(resultsSeconds);
        state.questionEndsAt = null;
        state.countdownEndsAt = null;
    }

    private void resetToWaiting(Room room) {
        state.phase = LobbyPhase.WAITING;
        state.countdownEndsAt = null;
        state.questionEndsAt = null;
        state.resultsEndsAt = null;
        state.currentQuestionIndex = -1;
        state.questions = List.of();
        state.currentQuestionAnswers.clear();
        room.setStatus(RoomStatus.WAITING);
        room.setRoundStartedAt(null);
        roomRepo.save(room);
    }

    private List<Question> pickRandomQuestions(int requestedCount) {
        List<Question> selected = new ArrayList<>();
        Set<Long> usedIds = new HashSet<>();
        DifficultyLevel[] difficulties = DifficultyLevel.values();
        int attempts = 0;
        while (selected.size() < requestedCount && attempts < requestedCount * 10) {
            DifficultyLevel difficulty = difficulties[random.nextInt(difficulties.length)];
            List<Question> batch = questionRepo.findRandomActiveQuestions(null, difficulty, PageRequest.of(0, 1));
            attempts++;
            if (batch.isEmpty()) {
                continue;
            }
            Question candidate = batch.get(0);
            if (usedIds.add(candidate.getId())) {
                selected.add(candidate);
            }
        }
        if (selected.size() < requestedCount) {
            log.warn("Only {} questions available for lobby round", selected.size());
        }
        return selected;
    }

    private void publishCountdown(String roomCode, long activePlayers) {
        LobbyCountdownPayload payload = LobbyCountdownPayload.builder()
                .roundNumber(state.roundNumber + 1)
                .countdownEndsAt(state.countdownEndsAt)
                .activePlayers((int) activePlayers)
                .build();
        eventPublisher.publishCustomRoomEvent(roomCode, "LOBBY_COUNTDOWN", payload);
    }

    private void publishScoreUpdate(String roomCode, List<LobbyLeaderboardEntry> leaderboard) {
        eventPublisher.publishCustomRoomEvent(roomCode, "LOBBY_SCORE_UPDATE", leaderboard);
    }

    private void resetParticipantStats(Long roomId) {
        List<RoomParticipant> participants = participantRepo.findByRoomId(roomId);
        participants.forEach(participant -> {
            participant.setCurrentScore(0);
            participant.setCorrectAnswers(0);
            participant.setFinalRank(null);
            participant.setIsReady(false);
        });
        participantRepo.saveAll(participants);
    }

    private void assignFinalRanks(Long roomId, List<LobbyLeaderboardEntry> topPlayers) {
        Map<Long, Integer> rankByUser = topPlayers.stream()
                .collect(Collectors.toMap(LobbyLeaderboardEntry::getUserId, LobbyLeaderboardEntry::getRank));
        List<RoomParticipant> participants = participantRepo.findByRoomId(roomId);
        participants.forEach(participant -> {
            Integer rank = rankByUser.get(participant.getUser().getId());
            participant.setFinalRank(rank);
        });
        participantRepo.saveAll(participants);
    }

    private List<LobbyLeaderboardEntry> buildLeaderboard(Long roomId) {
        return buildLeaderboard(getActiveParticipants(roomId));
    }

    private List<LobbyLeaderboardEntry> buildLeaderboard(List<RoomParticipant> participants) {
        participants.sort(Comparator
            .comparing((RoomParticipant participant) -> Optional.ofNullable(participant.getCurrentScore()).orElse(0), Comparator.reverseOrder())
            .thenComparing(participant -> Optional.ofNullable(participant.getCorrectAnswers()).orElse(0), Comparator.reverseOrder())
            .thenComparing(RoomParticipant::getJoinedAt));
        List<LobbyLeaderboardEntry> entries = new ArrayList<>();
        int rank = 1;
        for (RoomParticipant participant : participants) {
            entries.add(LobbyLeaderboardEntry.builder()
                    .userId(participant.getUser().getId())
                    .username(participant.getUser().getUsername())
                    .score(Optional.ofNullable(participant.getCurrentScore()).orElse(0))
                    .correctAnswers(Optional.ofNullable(participant.getCorrectAnswers()).orElse(0))
                    .rank(rank++)
                    .build());
        }
        return entries;
    }

    private List<RoomParticipant> getActiveParticipants(Long roomId) {
        return participantRepo.findByRoomId(roomId).stream()
                .filter(participant -> Boolean.TRUE.equals(participant.getIsActive()))
                .collect(Collectors.toList());
    }

    private LobbyQuestionView resolveCurrentQuestionView() {
        return getCurrentQuestion().map(this::toQuestionView).orElse(null);
    }

    private Optional<Question> getCurrentQuestion() {
        if (state.currentQuestionIndex < 0 || state.currentQuestionIndex >= state.questions.size()) {
            return Optional.empty();
        }
        return Optional.of(state.questions.get(state.currentQuestionIndex));
    }

    private LobbyQuestionView toQuestionView(Question question) {
        return LobbyQuestionView.builder()
                .id(question.getId())
                .text(question.getText())
                .optionA(question.getOptionA())
                .optionB(question.getOptionB())
                .optionC(question.getOptionC())
                .optionD(question.getOptionD())
                .difficulty(question.getDifficultyLevel())
                .build();
    }

    private boolean hasUserAnsweredCurrentQuestion(Long userId) {
        if (userId == null) {
            return false;
        }
        return state.currentQuestionAnswers.containsKey(userId);
    }

    private record PlayerStats(Integer score, Integer rank, boolean participant) {}

    private PlayerStats resolvePlayerStats(Long roomId, Long userId, List<LobbyLeaderboardEntry> leaderboard) {
        if (userId == null) {
            return new PlayerStats(null, null, false);
        }
        Integer rank = resolvePlayerRank(leaderboard, userId);
        Optional<RoomParticipant> participantOpt = participantRepo.findByRoomIdAndUserId(roomId, userId);
        Integer score = participantOpt
                .map(RoomParticipant::getCurrentScore)
                .orElse(null);
        boolean participant = participantOpt.map(RoomParticipant::getIsActive).orElse(false);
        return new PlayerStats(score, rank, participant);
    }

    private Integer resolvePlayerRank(List<LobbyLeaderboardEntry> leaderboard, Long userId) {
        if (userId == null) {
            return null;
        }
        return leaderboard.stream()
                .filter(entry -> userId.equals(entry.getUserId()))
                .map(LobbyLeaderboardEntry::getRank)
                .findFirst()
                .orElse(null);
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

    private static final class LobbyRoundState {
        private String roomCode;
        private LobbyPhase phase = LobbyPhase.WAITING;
        private int roundNumber = 0;
        private List<Question> questions = List.of();
        private int currentQuestionIndex = -1;
        private Instant countdownEndsAt;
        private Instant questionEndsAt;
        private Instant resultsEndsAt;
        private final Map<Long, Map<Long, Integer>> answersByQuestion = new HashMap<>();
        private final Map<Long, Integer> currentQuestionAnswers = new HashMap<>();
        private List<LobbyLeaderboardEntry> lastLeaderboard = List.of();

        public LobbyPhase getPhase() {
            return phase;
        }

        public int getCurrentQuestionIndex() {
            return currentQuestionIndex;
        }
    }
}
