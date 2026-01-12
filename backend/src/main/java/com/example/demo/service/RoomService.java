package com.example.demo.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.domain.Category;
import com.example.demo.domain.Question;
import com.example.demo.domain.Quiz;
import com.example.demo.domain.Room;
import com.example.demo.domain.RoomParticipant;
import com.example.demo.domain.User;
import com.example.demo.dto.room.RoomCreateRequest;
import com.example.demo.dto.room.RoomInviteRequest;
import com.example.demo.dto.room.RoomPlayerRequest;
import com.example.demo.dto.room.RoomReadyRequest;
import com.example.demo.dto.room.RoomResponse;
import com.example.demo.enums.DifficultyLevel;
import com.example.demo.enums.RoomStatus;
import com.example.demo.enums.RoomType;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repo.CategoryRepo;
import com.example.demo.repo.QuestionRepo;
import com.example.demo.repo.QuizRepo;
import com.example.demo.repo.RoomParticipantRepo;
import com.example.demo.repo.RoomRepo;
import com.example.demo.repo.UserRepo;
import com.example.demo.dto.duel.DuelInvitePayload;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    private final RoomRepo roomRepo;
    private final RoomParticipantRepo participantRepo;
    private final UserRepo userRepo;
    private final QuizRepo quizRepo;
    private final CategoryRepo categoryRepo;
    private final QuestionRepo questionRepo;
    private final GameEventPublisher eventPublisher;
    private final DuelService duelService;
    private final PresenceService presenceService;

    public RoomResponse createRoom(RoomCreateRequest request) {
        User owner = getUser(request.getOwnerId());
        Quiz quiz = resolveQuiz(request.getQuizId());
        Category category = resolveCategory(request.getCategoryId(), quiz);
        DifficultyLevel difficulty = resolveDifficulty(request.getDifficulty(), quiz);

        int maxPlayers = RoomType.DUEL.equals(request.getRoomType()) ? 2 : request.getMaxPlayers();
        List<Question> questions = resolveQuestions(request, quiz, difficulty, category);

        Room room = new Room();
        room.setOwner(owner);
        room.setRoomType(request.getRoomType());
        room.setMaxPlayers(maxPlayers);
        room.setQuestionCount(questions.size());
        room.setTimePerQuestion(request.getTimePerQuestion());
        room.setDifficulty(difficulty);
        room.setCategory(category);
        room.setQuiz(quiz);
        room.setStatus(RoomStatus.WAITING);
        room.setCode(generateUniqueCode());
        room.setQuestions(new ArrayList<>(questions));

        Room savedRoom = roomRepo.save(room);

        RoomParticipant ownerParticipant = new RoomParticipant();
        ownerParticipant.setRoom(savedRoom);
        ownerParticipant.setUser(owner);
        ownerParticipant.setIsActive(true);
        ownerParticipant.setIsReady(false);
        participantRepo.save(ownerParticipant);

        syncPlayerCount(savedRoom);
        RoomResponse response = buildAndPublish(savedRoom);
        return response;
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> listRooms(RoomStatus status) {
        List<Room> rooms = status != null ? roomRepo.findByStatus(status) : roomRepo.findAll();
        return rooms.stream()
                .map(room -> RoomResponse.fromEntity(room, participantRepo.findByRoomId(room.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public RoomResponse getRoomByCode(String code) {
        Room room = getRoomOrThrow(code);
        return RoomResponse.fromEntity(room, participantRepo.findByRoomId(room.getId()));
    }

    public RoomResponse joinRoom(String code, RoomPlayerRequest request) {
        Room room = getRoomOrThrow(code);
        ensureRoomJoinable(room);
        User user = getUser(request.getUserId());

        RoomParticipant participant = participantRepo.findByRoomIdAndUserId(room.getId(), user.getId())
                .orElseGet(() -> new RoomParticipant());
        participant.setRoom(room);
        participant.setUser(user);
        participant.setIsActive(true);
        participant.setIsReady(false);
        participantRepo.save(participant);

        syncPlayerCount(room);
        return buildAndPublish(room);
    }

    public RoomResponse leaveRoom(String code, RoomPlayerRequest request) {
        Room room = getRoomOrThrow(code);
        RoomParticipant participant = participantRepo.findByRoomIdAndUserId(room.getId(), request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User is not part of this room"));
        boolean duelRoom = RoomType.DUEL.equals(room.getRoomType());
        boolean duelWaitingRoom = duelRoom && RoomStatus.WAITING.equals(room.getStatus());

        participant.leave();
        if (duelWaitingRoom) {
            participantRepo.delete(participant);
        } else {
            participantRepo.save(participant);
        }

        syncPlayerCount(room);
        if (duelRoom) {
            duelService.handlePlayerLeave(room, participant);
        }
        if (room.getCurrentPlayers() <= 0) {
            if (RoomType.LOBBY.equals(room.getRoomType())) {
                room.setStatus(RoomStatus.WAITING);
                room.setClosedAt(null);
                room.setRoundStartedAt(null);
                room.setCurrentQuestionIndex(0);
            } else if (!RoomType.DUEL.equals(room.getRoomType())) {
                room.setStatus(RoomStatus.FINISHED);
                room.setClosedAt(LocalDateTime.now());
            }
        }
        return buildAndPublish(room);
    }

    public RoomResponse toggleReady(String code, RoomReadyRequest request) {
        Room room = getRoomOrThrow(code);
        RoomParticipant participant = participantRepo.findByRoomIdAndUserId(room.getId(), request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User is not part of this room"));
        participant.setIsReady(Boolean.TRUE.equals(request.getReady()));
        participantRepo.save(participant);
        return buildAndPublish(room);
    }

    public RoomResponse startRoom(String code, RoomPlayerRequest request) {
        Room room = getRoomOrThrow(code);
        ensureOwner(room, request.getUserId());
        if (RoomStatus.IN_PROGRESS.equals(room.getStatus())) {
            throw new IllegalStateException("Room already started");
        }
        List<RoomParticipant> activeParticipants = participantRepo.findByRoomIdAndIsActiveTrue(room.getId());
        if (activeParticipants.size() < 2) {
            throw new IllegalStateException("Need at least two players to start");
        }
        boolean everyoneReady = activeParticipants.stream().allMatch(RoomParticipant::getIsReady);
        if (!everyoneReady) {
            throw new IllegalStateException("All players must be ready");
        }
        room.setStatus(RoomStatus.IN_PROGRESS);
        room.setCurrentQuestionIndex(0);
        room.setRoundStartedAt(LocalDateTime.now());
        if (RoomType.DUEL.equals(room.getRoomType())) {
            duelService.handleRoomStarted(room, activeParticipants);
        }
        roomRepo.save(room);
        return buildAndPublish(room);
    }

    public RoomResponse finishRoom(String code, RoomPlayerRequest request) {
        Room room = getRoomOrThrow(code);
        ensureOwner(room, request.getUserId());
        if (RoomType.DUEL.equals(room.getRoomType())) {
            duelService.forceFinish(room);
            room = getRoomOrThrow(code);
        } else {
            room.setStatus(RoomStatus.FINISHED);
            room.setClosedAt(LocalDateTime.now());
            roomRepo.save(room);
        }
        return buildAndPublish(room);
    }

    public void invitePlayer(String code, RoomInviteRequest request) {
        Room room = getRoomOrThrow(code);
        if (!RoomType.DUEL.equals(room.getRoomType())) {
            throw new IllegalStateException("Invites are available only for duel rooms");
        }
        if (!RoomStatus.WAITING.equals(room.getStatus())) {
            throw new IllegalStateException("You can only invite players before the duel starts");
        }
        if (room.isFull()) {
            throw new IllegalStateException("Room already has the maximum number of players");
        }

        User inviter = getUser(request.getInviterId());
        ensureInvitePermission(room, inviter.getId());

        User target = getUser(request.getTargetUserId());
        if (Objects.equals(inviter.getId(), target.getId())) {
            throw new IllegalArgumentException("You cannot invite yourself");
        }
        boolean alreadyParticipant = participantRepo.findByRoomIdAndUserId(room.getId(), target.getId()).isPresent();
        if (alreadyParticipant) {
            throw new IllegalStateException("Player is already part of this room");
        }
        if (!presenceService.isUserOnline(target.getId())) {
            throw new IllegalStateException("Player is no longer online");
        }

        DuelInvitePayload payload = DuelInvitePayload.builder()
                .inviteId(UUID.randomUUID().toString())
                .roomCode(room.getCode())
                .inviterId(inviter.getId())
                .inviterUsername(inviter.getUsername())
                .questionCount(room.getQuestionCount())
                .timePerQuestion(room.getTimePerQuestion())
                .difficulty(room.getDifficulty())
                .sentAt(Instant.now())
                .build();
        eventPublisher.publishUserEvent(target.getId(), "DUEL_INVITE", payload);
    }

    private RoomResponse buildAndPublish(Room room) {
        Room saved = roomRepo.save(room);
        List<RoomParticipant> participants = participantRepo.findByRoomId(room.getId());
        eventPublisher.publishRoomUpdate(saved, participants);
        return RoomResponse.fromEntity(saved, participants);
    }

    private void ensureRoomJoinable(Room room) {
        if (RoomType.LOBBY.equals(room.getRoomType())) {
            if (room.isFull()) {
                throw new IllegalStateException("Room is already full");
            }
            return;
        }
        if (!RoomStatus.WAITING.equals(room.getStatus())) {
            throw new IllegalStateException("Room is not joinable");
        }
        if (room.isFull()) {
            throw new IllegalStateException("Room is already full");
        }
    }

    private void ensureOwner(Room room, Long userId) {
        if (room.getOwner() == null || !room.getOwner().getId().equals(userId)) {
            throw new IllegalArgumentException("Only room owner can perform this action");
        }
    }

    private void ensureInvitePermission(Room room, Long userId) {
        boolean isOwner = room.getOwner() != null && room.getOwner().getId().equals(userId);
        if (isOwner) {
            return;
        }
        boolean isActiveParticipant = participantRepo.findByRoomIdAndUserId(room.getId(), userId)
                .map(participant -> Boolean.TRUE.equals(participant.getIsActive()))
                .orElse(false);
        if (!isActiveParticipant) {
            throw new IllegalArgumentException("Only active players can invite others");
        }
    }

    private void syncPlayerCount(Room room) {
        participantRepo.flush();
        long activePlayers = participantRepo.findByRoomId(room.getId()).stream()
                .filter(participant -> Boolean.TRUE.equals(participant.getIsActive()))
                .count();
        room.setCurrentPlayers(Math.toIntExact(activePlayers));
        roomRepo.save(room);
    }

    private Room getRoomOrThrow(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Room code is required");
        }
        return roomRepo.findByCode(code.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Room with code %s not found".formatted(code)));
    }

    private User getUser(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User id is required");
        }
        return userRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
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

    private List<Question> resolveQuestions(RoomCreateRequest request, Quiz quiz, DifficultyLevel difficulty, Category category) {
        int requestedCount = request.getQuestionCount();
        if (quiz != null) {
            List<Question> quizQuestions = quiz.getQuestions();
            if (quizQuestions == null || quizQuestions.isEmpty()) {
                throw new IllegalArgumentException("Quiz does not have any questions");
            }
            if (requestedCount != quizQuestions.size()) {
                throw new IllegalArgumentException("Question count must match quiz questions (%d)".formatted(quizQuestions.size()));
            }
            return new ArrayList<>(quizQuestions);
        }

        Pageable pageable = PageRequest.of(0, requestedCount);
        Long categoryId = category != null ? category.getId() : null;
        List<Question> questions = questionRepo.findRandomActiveQuestions(categoryId, difficulty, pageable);
        if (questions.size() < requestedCount) {
            throw new IllegalArgumentException("Not enough questions available for the selected criteria");
        }
        return questions;
    }

    private String generateUniqueCode() {
        String code;
        int attempts = 0;
        do {
            code = randomCode();
            attempts++;
            if (attempts > 10) {
                throw new IllegalStateException("Unable to generate unique room code");
            }
        } while (roomRepo.existsByCode(code));
        return code;
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = RANDOM.nextInt(CODE_ALPHABET.length());
            builder.append(CODE_ALPHABET.charAt(index));
        }
        return builder.toString();
    }
}
