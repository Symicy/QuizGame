import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import duelService from '../../services/duelService';
import presenceService from '../../services/presenceService';
import useLobbyEvents from '../../hooks/useLobbyEvents';

const difficultyOptions = [
  { label: 'Mediu (implicit)', value: 'MEDIUM' },
  { label: 'Ușor', value: 'EASY' },
  { label: 'Dificil', value: 'HARD' },
];

const answerLabels = ['A', 'B', 'C', 'D'];

const DuelRoom = () => {
  const { user, pendingInvites, dismissInvite } = useAuth();
  const navigate = useNavigate();
  const [room, setRoom] = useState(null);
  const [duelState, setDuelState] = useState(null);
  const [status, setStatus] = useState({ type: '', message: '' });
  const [loading, setLoading] = useState(false);
  const [answering, setAnswering] = useState(false);
  const [selectedAnswer, setSelectedAnswer] = useState(null);
  const [lastSubmission, setLastSubmission] = useState(null);
  const [answerReveal, setAnswerReveal] = useState(null);
  const [questionTimeLeft, setQuestionTimeLeft] = useState(null);
  const [createForm, setCreateForm] = useState({
    questionCount: '10',
    timePerQuestion: '30',
    difficulty: 'MEDIUM',
  });
  const [joinCode, setJoinCode] = useState('');
  const [onlinePlayers, setOnlinePlayers] = useState([]);
  const [presenceStatus, setPresenceStatus] = useState({ loading: false, error: '' });
  const [inviteSendingTo, setInviteSendingTo] = useState(null);
  const [acceptingInviteId, setAcceptingInviteId] = useState(null);
  const timerIntervalRef = useRef(null);

  const playerParticipant = useMemo(
    () => room?.participants?.find((participant) => participant.userId === user?.id),
    [room, user?.id]
  );
  const isOwner = room?.ownerId === user?.id;
  const duelInProgress = room?.status === 'IN_PROGRESS';
  const canInvitePlayers = Boolean(room?.code && room?.status === 'WAITING' && isOwner);
  const activeRoomUserIds = useMemo(() => new Set(
    (room?.participants || [])
      .map((participant) => participant.userId)
      .filter(Boolean)
  ), [room?.participants]);

  const leaderboard = useMemo(() => {
    if (duelState?.leaderboard?.length) {
      return duelState.leaderboard;
    }
    return (room?.participants || []).map((participant) => ({
      userId: participant.userId,
      username: participant.username,
      score: participant.currentScore || 0,
      correctAnswers: participant.correctAnswers || 0,
      ready: participant.ready,
      active: participant.active,
      finalRank: participant.finalRank,
    }));
  }, [duelState?.leaderboard, room?.participants]);

  const availableOnlinePlayers = useMemo(() => {
    return onlinePlayers.filter((player) => !activeRoomUserIds.has(player.id));
  }, [onlinePlayers, activeRoomUserIds]);

  const roomStatusMeta = useMemo(() => {
    const defaults = { label: 'Fără cameră activă', badge: 'text-bg-secondary' };
    if (!room?.status) {
      return defaults;
    }
    const mapping = {
      WAITING: { label: 'Se adună jucători', badge: 'text-bg-warning' },
      IN_PROGRESS: { label: 'Duel în desfășurare', badge: 'text-bg-primary' },
      FINISHED: { label: 'Duel încheiat', badge: 'text-bg-success' },
    };
    return mapping[room.status] || defaults;
  }, [room?.status]);

  const duelSummary = useMemo(() => ({
    questionCount: room?.questionCount || duelState?.session?.questions?.length || 0,
    difficulty: room?.difficulty || duelState?.difficulty || 'Variat',
    timePerQuestion: room?.timePerQuestion || duelState?.timePerQuestion || 30,
  }), [room?.questionCount, room?.difficulty, room?.timePerQuestion, duelState?.session?.questions?.length, duelState?.difficulty, duelState?.timePerQuestion]);

  const session = duelState?.session;
  const questions = session?.questions || [];
  const fallbackIndex = useMemo(() => questions.findIndex((question) => question.answered === false), [questions]);
  const activeQuestionIndex = typeof duelState?.currentQuestionIndex === 'number'
    ? duelState.currentQuestionIndex
    : fallbackIndex;
  const currentQuestion = activeQuestionIndex >= 0 ? questions[activeQuestionIndex] : null;
  const displayQuestionNumber = activeQuestionIndex >= 0 ? activeQuestionIndex + 1 : 0;
  const allQuestionsAnswered = useMemo(() => questions.every((question) => question.answered), [questions]);
  const timePerQuestionSetting = duelState?.timePerQuestion || room?.timePerQuestion || 30;
  const currentReveal = currentQuestion && answerReveal?.questionId === currentQuestion.id ? answerReveal : null;
  const questionProgress = useMemo(() => {
    if (questionTimeLeft == null || !timePerQuestionSetting || timePerQuestionSetting <= 0) {
      return null;
    }
    return Math.max(0, Math.min(100, (questionTimeLeft / timePerQuestionSetting) * 100));
  }, [questionTimeLeft, timePerQuestionSetting]);

  const updateStatus = (type, message) => setStatus({ type, message });

  const triggerFeedback = (question, submission) => {
    if (question) {
      setAnswerReveal({
        questionId: question.id,
        correctOption: typeof question.correctOption === 'number' ? question.correctOption : null,
        playerOption: typeof question.selectedOption === 'number' ? question.selectedOption : null,
        correct: question.correct,
      });
    } else {
      setAnswerReveal(null);
    }
    setLastSubmission(submission || null);
  };

  const resetMatchState = useCallback(() => {
    setDuelState(null);
    setSelectedAnswer(null);
    setLastSubmission(null);
    setAnswerReveal(null);
    setQuestionTimeLeft(null);
    if (timerIntervalRef.current) {
      clearInterval(timerIntervalRef.current);
      timerIntervalRef.current = null;
    }
  }, []);

  const loadDuelState = useCallback(async () => {
    if (!room?.code || !user?.id) {
      return;
    }
    try {
      const data = await duelService.getState(room.code, user.id);
      setDuelState(data);
    } catch (error) {
      updateStatus('warning', error.message || 'Nu am putut încărca starea duelului.');
    }
  }, [room?.code, user?.id]);

  useEffect(() => {
    if (room?.code && user?.id && room.status === 'IN_PROGRESS') {
      loadDuelState();
    }
  }, [room?.code, room?.status, user?.id, loadDuelState]);

  useEffect(() => {
    if (!room?.code || room.status !== 'IN_PROGRESS' || !user?.id) {
      return undefined;
    }
    const interval = setInterval(() => {
      loadDuelState();
    }, 2500);
    return () => clearInterval(interval);
  }, [room?.code, room?.status, user?.id, loadDuelState]);

  const handleSocketMessage = useCallback((message) => {
    if (message.type === 'ROOM_UPDATED') {
      setRoom(message.payload);
      return;
    }
    if (message.type === 'DUEL_SCORE_UPDATE') {
      setDuelState((prev) => ({
        ...prev,
        leaderboard: message.payload,
      }));
      return;
    }
    if (message.type === 'DUEL_QUESTION_REVEAL') {
      const playerChoice = message.payload?.playerChoices?.find((choice) => choice.userId === user?.id);
      setDuelState((prev) => ({
        ...prev,
        leaderboard: message.payload?.leaderboard || prev?.leaderboard,
      }));
      setQuestionTimeLeft(null);
      if (typeof playerChoice?.selectedOption === 'number') {
        setSelectedAnswer(playerChoice.selectedOption);
      }
      setAnswerReveal((prev) => {
        if (!message.payload) {
          return prev;
        }
        const base = prev?.questionId === message.payload.questionId ? prev : {};
        return {
          questionId: message.payload.questionId ?? base.questionId ?? null,
          correctOption: message.payload.correctOption ?? base.correctOption ?? null,
          playerOption:
            typeof playerChoice?.selectedOption === 'number'
              ? playerChoice.selectedOption
              : base.playerOption ?? null,
          correct:
            typeof playerChoice?.correct === 'boolean'
              ? playerChoice.correct
              : base.correct ?? null,
        };
      });
      loadDuelState();
      return;
    }
    if (message.type === 'DUEL_QUESTION_START') {
      setAnswerReveal(null);
      setSelectedAnswer(null);
      setLastSubmission(null);
      setQuestionTimeLeft(timePerQuestionSetting);
      setDuelState((prev) => ({
        ...prev,
        currentQuestionIndex:
          typeof message.payload?.questionIndex === 'number'
            ? message.payload.questionIndex
            : prev?.currentQuestionIndex,
      }));
      loadDuelState();
      return;
    }
    if (message.type === 'DUEL_FINISHED') {
      setDuelState((prev) => ({
        ...prev,
        duelActive: false,
        duelCompleted: true,
        winnerUserId: message.payload?.winnerUserId || prev?.winnerUserId,
        leaderboard: message.payload?.leaderboard || prev?.leaderboard,
      }));
      setQuestionTimeLeft(null);
      setAnswerReveal(null);
      setSelectedAnswer(null);
      setLastSubmission(null);
    }
  }, [loadDuelState, timePerQuestionSetting, user?.id]);

  useLobbyEvents({ roomCode: room?.code, onMessage: handleSocketMessage });

  useEffect(() => {
    if (!canInvitePlayers || !user?.id) {
      setOnlinePlayers([]);
      setPresenceStatus({ loading: false, error: '' });
      return undefined;
    }
    let cancelled = false;
    const fetchOnline = async () => {
      setPresenceStatus((prev) => ({ ...prev, loading: true, error: '' }));
      try {
        const data = await presenceService.getOnlineUsers(user.id);
        if (!cancelled) {
          setOnlinePlayers(data || []);
          setPresenceStatus({ loading: false, error: '' });
        }
      } catch (error) {
        if (!cancelled) {
          setPresenceStatus({
            loading: false,
            error: error.message || 'Nu am putut încărca lista de jucători online.',
          });
        }
      }
    };

    fetchOnline();
    const interval = setInterval(fetchOnline, 15000);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [canInvitePlayers, user?.id]);

  useEffect(() => {
    if (currentQuestion) {
      setSelectedAnswer(null);
      setAnswerReveal(null);
      setLastSubmission(null);
    }
  }, [currentQuestion?.id]);

  useEffect(() => {
    if (!currentQuestion || !duelState?.session?.questions) {
      return;
    }
    const sessionQuestion = duelState.session.questions.find((question) => question.id === currentQuestion.id);
    if (!sessionQuestion || !sessionQuestion.answered) {
      return;
    }
    setAnswerReveal((prev) => {
      const alreadyAccurate = prev?.questionId === sessionQuestion.id
        && typeof prev.playerOption === 'number'
        && typeof prev.correct === 'boolean';
      if (alreadyAccurate) {
        return prev;
      }
      return {
        questionId: sessionQuestion.id,
        correctOption: typeof sessionQuestion.correctOption === 'number'
          ? sessionQuestion.correctOption
          : prev?.correctOption ?? null,
        playerOption: typeof sessionQuestion.selectedOption === 'number'
          ? sessionQuestion.selectedOption
          : prev?.playerOption ?? null,
        correct: typeof sessionQuestion.correct === 'boolean'
          ? sessionQuestion.correct
          : prev?.correct ?? null,
      };
    });
  }, [currentQuestion?.id, duelState?.session?.questions]);

  useEffect(() => {
    if (timerIntervalRef.current) {
      clearInterval(timerIntervalRef.current);
      timerIntervalRef.current = null;
    }
    if (!currentQuestion || currentQuestion.answered || duelState?.duelCompleted || selectedAnswer !== null) {
      if (!currentQuestion || currentQuestion.answered || duelState?.duelCompleted) {
        setQuestionTimeLeft(null);
      }
      return undefined;
    }
    setQuestionTimeLeft(timePerQuestionSetting);
    timerIntervalRef.current = setInterval(() => {
      setQuestionTimeLeft((prev) => {
        if (prev === null) {
          return prev;
        }
        if (prev <= 1) {
          if (timerIntervalRef.current) {
            clearInterval(timerIntervalRef.current);
            timerIntervalRef.current = null;
          }
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => {
      if (timerIntervalRef.current) {
        clearInterval(timerIntervalRef.current);
        timerIntervalRef.current = null;
      }
    };
  }, [currentQuestion?.id, currentQuestion?.answered, duelState?.duelCompleted, selectedAnswer, timePerQuestionSetting]);

  useEffect(() => {
    return () => {
      if (room?.code && user?.id) {
        duelService.leaveRoom(room.code, user.id).catch(() => {});
      }
    };
  }, [room?.code, user?.id]);

  useEffect(() => () => {
    if (timerIntervalRef.current) {
      clearInterval(timerIntervalRef.current);
      timerIntervalRef.current = null;
    }
  }, []);

  const handleCreateRoom = async (event) => {
    event.preventDefault();
    if (!user?.id) {
      updateStatus('danger', 'Trebuie să fii autentificat pentru a crea un duel.');
      return;
    }
    const questionCount = Number(createForm.questionCount);
    const timePerQuestion = Number(createForm.timePerQuestion);
    if (Number.isNaN(questionCount) || questionCount < 5 || questionCount > 30) {
      updateStatus('danger', 'Numărul de întrebări trebuie să fie între 5 și 30.');
      return;
    }
    if (Number.isNaN(timePerQuestion) || timePerQuestion < 10 || timePerQuestion > 90) {
      updateStatus('danger', 'Timpul pe întrebare trebuie să fie între 10 și 90 de secunde.');
      return;
    }
    setLoading(true);
    try {
      const payload = {
        ownerId: user.id,
        questionCount,
        timePerQuestion,
        difficulty: createForm.difficulty || null,
      };
      const createdRoom = await duelService.createRoom(payload);
      setRoom(createdRoom);
      resetMatchState();
      updateStatus('success', 'Camera a fost creată. Trimite codul coechipierului!');
    } catch (error) {
      updateStatus('danger', error.message || 'Nu am putut crea camera.');
    } finally {
      setLoading(false);
    }
  };

  const joinRoomByCode = useCallback(async (code) => {
    const sanitized = (code || '').trim().toUpperCase();
    if (!sanitized) {
      updateStatus('danger', 'Introdu codul camerei.');
      return false;
    }
    if (!user?.id) {
      updateStatus('danger', 'Trebuie să fii autentificat pentru a intra într-o cameră.');
      return false;
    }
    setLoading(true);
    try {
      const joinedRoom = await duelService.joinRoom(sanitized, user.id);
      setRoom(joinedRoom);
      resetMatchState();
      setJoinCode('');
      updateStatus('success', 'Ai intrat în cameră.');
      return true;
    } catch (error) {
      updateStatus('danger', error.message || 'Nu am putut intra în cameră.');
      return false;
    } finally {
      setLoading(false);
    }
  }, [user?.id, resetMatchState]);

  const handleJoinRoom = async (event) => {
    event.preventDefault();
    await joinRoomByCode(joinCode);
  };

  const handleInvitePlayer = async (targetUserId) => {
    if (!room?.code || !user?.id) {
      updateStatus('danger', 'Creează o cameră înainte să trimiți invitații.');
      return;
    }
    setInviteSendingTo(targetUserId);
    try {
      await duelService.invitePlayer(room.code, user.id, targetUserId);
      updateStatus('success', 'Invitația a fost trimisă.');
    } catch (error) {
      updateStatus('danger', error.message || 'Nu am putut trimite invitația.');
    } finally {
      setInviteSendingTo(null);
    }
  };

  const handleAcceptInvite = async (invite) => {
    if (!invite?.roomCode) {
      return;
    }
    if (room?.code) {
      updateStatus('warning', 'Părăsește camera curentă pentru a accepta o nouă invitație.');
      return;
    }
    setAcceptingInviteId(invite.inviteId);
    const joined = await joinRoomByCode(invite.roomCode);
    if (joined) {
      dismissInvite(invite.inviteId);
    }
    setAcceptingInviteId(null);
  };

  const handleDeclineInvite = (inviteId) => {
    dismissInvite(inviteId);
  };

  const handleLeaveRoom = async () => {
    if (!room?.code || !user?.id) {
      setRoom(null);
      resetMatchState();
      return;
    }
    setLoading(true);
    try {
      await duelService.leaveRoom(room.code, user.id);
      setRoom(null);
      resetMatchState();
      updateStatus('info', 'Ai părăsit camera.');
    } catch (error) {
      updateStatus('danger', error.message || 'Nu am putut părăsi camera.');
    } finally {
      setLoading(false);
    }
  };

  const handleCopyCode = () => {
    if (!room?.code) {
      return;
    }
    if (navigator?.clipboard?.writeText) {
      navigator.clipboard
        .writeText(room.code)
        .then(() => updateStatus('success', 'Codul a fost copiat în clipboard.'))
        .catch(() => updateStatus('warning', 'Copierea codului nu este disponibilă în acest browser.'));
    } else {
      updateStatus('warning', 'Copierea codului nu este disponibilă în acest browser.');
    }
  };

  const handleReadyToggle = async () => {
    if (!room?.code || !user?.id) {
      return;
    }
    try {
      const nextReady = !playerParticipant?.ready;
      const updatedRoom = await duelService.setReady(room.code, user.id, nextReady);
      setRoom(updatedRoom);
      updateStatus('', '');
    } catch (error) {
      updateStatus('danger', error.message || 'Nu am putut actualiza starea de pregătire.');
    }
  };

  const handleStartMatch = async () => {
    if (!room?.code || !user?.id) {
      return;
    }
    setLoading(true);
    try {
      const updatedRoom = await duelService.startRoom(room.code, user.id);
      setRoom(updatedRoom);
      resetMatchState();
      updateStatus('success', 'Duelul a pornit!');
    } catch (error) {
      updateStatus('danger', error.message || 'Nu am putut porni duelul.');
    } finally {
      setLoading(false);
    }
  };

  const handleAnswerSelect = (index) => {
    if (
      !currentQuestion ||
      currentQuestion.answered ||
      answering ||
      duelState?.duelCompleted ||
      selectedAnswer !== null
    ) {
      return;
    }
    setSelectedAnswer(index);
    sendAnswer(currentQuestion.id, index);
  };

  const sendAnswer = async (questionId, answerIndex) => {
    if (!questionId || answerIndex == null || !room?.code || !user?.id) {
      return;
    }
    setAnswering(true);
    let questionForReveal = null;
    try {
      const payload = {
        userId: user.id,
        questionId,
        answerIndex,
      };
      const response = await duelService.submitAnswer(room.code, payload);
      setDuelState((prev) => {
        const nextSession = response.session || prev?.session;
        questionForReveal = nextSession?.questions?.find((question) => question.id === questionId);
        return {
          ...prev,
          session: nextSession,
          leaderboard: response.leaderboard || prev?.leaderboard,
          duelCompleted: response.duelCompleted || prev?.duelCompleted,
          winnerUserId: response.winnerUserId || prev?.winnerUserId,
        };
      });
      if (questionForReveal || response.submission) {
        triggerFeedback(questionForReveal, response.submission);
      }
    } catch (error) {
      updateStatus('danger', error.message || 'Nu am putut trimite răspunsul.');
    } finally {
      setAnswering(false);
    }
  };

  const renderInvitesPanel = () => {
    if (!pendingInvites?.length) {
      return null;
    }
    return (
      <div className="alert alert-info" role="alert">
        <h2 className="h6 mb-3">Invitații primite</h2>
        <div className="d-flex flex-column gap-3">
          {pendingInvites.map((invite) => (
            <div
              key={invite.inviteId}
              className="d-flex flex-column flex-lg-row align-items-lg-center justify-content-between gap-2"
            >
              <div>
                <strong>{invite.inviterUsername}</strong> te invită la un duel ({invite.questionCount || '-'} întrebări,
                {` ${invite.timePerQuestion || 30}s`}/întrebare, dificultate {invite.difficulty || 'MEDIUM'}).
              </div>
              <div className="d-flex gap-2">
                <button
                  type="button"
                  className="btn btn-sm btn-success"
                  disabled={!!room?.code || acceptingInviteId === invite.inviteId || loading}
                  onClick={() => handleAcceptInvite(invite)}
                >
                  {acceptingInviteId === invite.inviteId ? 'Se alătură...' : 'Acceptă' }
                </button>
                <button
                  type="button"
                  className="btn btn-sm btn-outline-secondary"
                  onClick={() => handleDeclineInvite(invite.inviteId)}
                >
                  Respinge
                </button>
              </div>
            </div>
          ))}
          {room && (
            <p className="mb-0 text-secondary">
              Părăsește camera curentă pentru a răspunde unei alte invitații.
            </p>
          )}
        </div>
      </div>
    );
  };

  const renderOnlinePlayersCard = () => {
    if (!canInvitePlayers) {
      return null;
    }
    return (
      <div className="card shadow-sm border-0 mt-4">
        <div className="card-body">
          <h2 className="h5 mb-3">Jucători online</h2>
          {presenceStatus.error && (
            <div className="alert alert-warning" role="alert">
              {presenceStatus.error}
            </div>
          )}
          {presenceStatus.loading && !presenceStatus.error && (
            <p className="text-secondary mb-0">Se caută jucători disponibili...</p>
          )}
          {!presenceStatus.loading && availableOnlinePlayers.length === 0 && (
            <p className="text-secondary mb-0">
              Momentan nu există jucători online sau toți sunt deja ocupați.
            </p>
          )}
          {!presenceStatus.loading && availableOnlinePlayers.length > 0 && (
            <div className="list-group list-group-flush">
              {availableOnlinePlayers.map((player) => (
                <div
                  key={player.id}
                  className="list-group-item px-0 d-flex justify-content-between align-items-center"
                >
                  <div>
                    <p className="mb-0 fw-semibold">{player.username}</p>
                    <small className="text-secondary">activ acum</small>
                  </div>
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-primary"
                    onClick={() => handleInvitePlayer(player.id)}
                    disabled={inviteSendingTo === player.id}
                  >
                    {inviteSendingTo === player.id ? 'Se trimite...' : 'Invită'}
                  </button>
                </div>
              ))}
            </div>
          )}
          <p className="text-secondary small mb-0 mt-3">
            Lista se actualizează automat la fiecare câteva secunde.
          </p>
        </div>
      </div>
    );
  };

  const renderCreateOrJoin = () => (
    <div className="container py-5">
      <div className="rounded-4 border bg-white shadow-sm p-4 p-lg-5 mb-4 text-center">
        <p className="text-uppercase text-secondary small fw-semibold mb-1">Mod duel</p>
        <h1 className="h4 mb-2">Înfruntă un prieten într-un quiz rapid</h1>
        <p className="text-secondary mb-0">Creezi o cameră în câteva secunde sau introduci un cod primit pentru a intra direct.</p>
      </div>
      {renderInvitesPanel()}
      <div className="row g-4">
        <div className="col-12 col-lg-6">
          <div className="card shadow-sm border-0 h-100">
            <div className="card-body">
              <h1 className="h4 mb-3">Creează un duel</h1>
              <p className="text-secondary mb-4">
                Trimite codul camerei prietenului tău și pregătiți-vă pentru o rundă rapidă 1v1.
              </p>
              <form className="d-flex flex-column gap-3" onSubmit={handleCreateRoom}>
                <div>
                  <label className="form-label" htmlFor="questionCount">Întrebări</label>
                  <input
                    type="number"
                    id="questionCount"
                    name="questionCount"
                    className="form-control"
                    min="5"
                    max="30"
                    value={createForm.questionCount}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, questionCount: event.target.value }))}
                  />
                  <div className="form-text">Recomandat 10-15 întrebări pentru meciuri scurte.</div>
                </div>
                <div>
                  <label className="form-label" htmlFor="timePerQuestion">Timp pe întrebare (secunde)</label>
                  <input
                    type="number"
                    id="timePerQuestion"
                    name="timePerQuestion"
                    className="form-control"
                    min="10"
                    max="90"
                    value={createForm.timePerQuestion}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, timePerQuestion: event.target.value }))}
                  />
                </div>
                <div>
                  <label className="form-label" htmlFor="difficulty">Dificultate</label>
                  <select
                    id="difficulty"
                    className="form-select"
                    value={createForm.difficulty}
                    onChange={(event) => setCreateForm((prev) => ({ ...prev, difficulty: event.target.value }))}
                  >
                    {difficultyOptions.map((option) => (
                      <option key={option.value} value={option.value}>{option.label}</option>
                    ))}
                  </select>
                </div>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                  {loading ? 'Se pregătește...' : 'Creează și intră'}
                </button>
              </form>
            </div>
          </div>
        </div>
        <div className="col-12 col-lg-6">
          <div className="card shadow-sm border-0 h-100">
            <div className="card-body">
              <h2 className="h4 mb-3">Intră după cod</h2>
              <p className="text-secondary mb-4">Primești codul de la prieten și te alături instant.</p>
              <form className="d-flex flex-column gap-3" onSubmit={handleJoinRoom}>
                <div>
                  <label className="form-label" htmlFor="joinCode">Cod cameră</label>
                  <input
                    type="text"
                    id="joinCode"
                    name="joinCode"
                    className="form-control text-uppercase"
                    value={joinCode}
                    onChange={(event) => setJoinCode(event.target.value.toUpperCase())}
                    maxLength={6}
                    placeholder="ABC123"
                  />
                </div>
                <button type="submit" className="btn btn-dark" disabled={loading}>
                  {loading ? 'Se alătură...' : 'Intră în cameră'}
                </button>
                <button type="button" className="btn btn-outline-secondary" onClick={() => navigate('/home')}>
                  Înapoi la home
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  const renderParticipants = () => (
    <div className="card shadow-sm border-0">
      <div className="card-body">
        <div className="d-flex justify-content-between align-items-center mb-3">
          <h2 className="h5 mb-0">Jucători</h2>
          <span className="badge text-bg-light text-secondary">{room?.participants?.length || 0}/2</span>
        </div>
        <div className="list-group list-group-flush">
          {(room?.participants || []).map((participant) => {
            const statusLabel = duelInProgress
              ? participant.active === false
                ? 'Deconectat'
                : 'În duel'
              : participant.ready
                ? 'Pregătit'
                : 'Ne-pregătit';
            const statusVariant = duelInProgress
              ? participant.active === false
                ? 'danger'
                : 'primary'
              : participant.ready
                ? 'success'
                : 'secondary';
            const highlight = participant.userId === user?.id ? 'bg-primary-subtle' : '';
            const ownerBadge = participant.userId === room?.ownerId ? (
              <span className="badge text-bg-dark ms-2">Host</span>
            ) : null;
            const initial = (participant.username || '?').charAt(0).toUpperCase();
            return (
              <div
                key={participant.id || participant.userId || participant.username}
                className={`list-group-item px-0 d-flex justify-content-between align-items-center ${highlight}`}
              >
                <div className="d-flex align-items-center gap-3">
                  <div
                    className="rounded-circle bg-primary-subtle text-primary fw-semibold d-flex align-items-center justify-content-center"
                    style={{ width: '40px', height: '40px' }}
                  >
                    {initial}
                  </div>
                  <div>
                    <p className="mb-0 fw-semibold d-flex align-items-center">
                      {participant.username || 'Anonim'}
                      {ownerBadge}
                    </p>
                    <small className="text-secondary">{statusLabel}</small>
                  </div>
                </div>
                <div className="text-end">
                  <span className={`badge text-bg-${statusVariant}`}>{statusLabel}</span>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );

  const renderScoreboard = () => (
    <div className="card shadow-sm border-0">
      <div className="card-body">
        <div className="d-flex justify-content-between align-items-center mb-3">
          <div>
            <h2 className="h5 mb-0">Scor live</h2>
            <small className="text-secondary">Actualizat automat la fiecare câteva secunde</small>
          </div>
          <button type="button" className="btn btn-sm btn-outline-secondary" onClick={loadDuelState}>
            Reîmprospătează
          </button>
        </div>
        {leaderboard.length === 0 ? (
          <p className="text-secondary mb-0">Încă nu sunt date de afișat.</p>
        ) : (
          <div className="list-group list-group-flush">
            {leaderboard.map((entry, index) => {
              const isWinner = entry.userId === duelState?.winnerUserId && duelState?.duelCompleted;
              const isCurrentUser = entry.userId === user?.id;
              const listHighlight = isCurrentUser ? 'bg-primary-subtle' : '';
              return (
                <div
                  key={entry.userId || entry.username || index}
                  className={`list-group-item px-0 d-flex justify-content-between align-items-center ${listHighlight}`}
                >
                  <div>
                    <p className="mb-0 fw-semibold d-flex align-items-center gap-2">
                      #{entry.rank ?? index + 1} · {entry.username || 'Anonim'}
                      {isWinner && <span className="badge text-bg-success">Câștigător</span>}
                    </p>
                    <small className="text-secondary">
                      {!duelInProgress && `${entry.ready ? 'Pregătit' : 'Ne-pregătit'} · `}
                      {entry.correctAnswers ?? 0} răspunsuri corecte
                    </small>
                  </div>
                  <div className="text-end">
                    <span className="badge text-bg-dark">{entry.score ?? 0} pct</span>
                    <div className="text-secondary small">{entry.answeredQuestions ?? '-'} / {entry.totalQuestions ?? '-'}</div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );

  const renderWaitingArea = () => (
    <div className="card shadow-sm border-0">
      <div className="card-body">
        <div className="d-flex flex-wrap justify-content-between align-items-start gap-3 mb-4">
          <div>
            <p className="text-uppercase text-secondary small fw-semibold mb-1">Lobby duel</p>
            <h2 className="h5 mb-1">Cod: <span className="text-uppercase">{room?.code}</span></h2>
            <p className="text-secondary mb-0">Trimite codul și pregătiți-vă să începeți meciul.</p>
          </div>
          <span className="badge text-bg-light text-secondary">{room?.participants?.length || 0}/2 jucători</span>
        </div>
        <div className="row g-3 text-center mb-4">
          <div className="col-12 col-md-4">
            <div className="border rounded-3 py-3">
              <p className="text-secondary small mb-1">Întrebări</p>
              <h3 className="h4 mb-0">{room?.questionCount || duelSummary.questionCount}</h3>
            </div>
          </div>
          <div className="col-12 col-md-4">
            <div className="border rounded-3 py-3">
              <p className="text-secondary small mb-1">Timp/întrebare</p>
              <h3 className="h4 mb-0">{room?.timePerQuestion || duelSummary.timePerQuestion}s</h3>
            </div>
          </div>
          <div className="col-12 col-md-4">
            <div className="border rounded-3 py-3">
              <p className="text-secondary small mb-1">Dificultate</p>
              <h3 className="h5 mb-0">{room?.difficulty || duelSummary.difficulty}</h3>
            </div>
          </div>
        </div>
        <div className="d-flex flex-wrap gap-2 mb-3">
          <button type="button" className="btn btn-outline-primary" onClick={handleCopyCode}>
            Copiază codul
          </button>
          <button type="button" className="btn btn-outline-danger" onClick={handleLeaveRoom}>
            Părăsește camera
          </button>
        </div>
        <div className="d-flex flex-wrap gap-2">
          <button
            type="button"
            className={`btn ${playerParticipant?.ready ? 'btn-success' : 'btn-outline-success'}`}
            onClick={handleReadyToggle}
          >
            {playerParticipant?.ready ? 'Ești pregătit' : 'Pregătește-te'}
          </button>
          {isOwner && (
            <button
              type="button"
              className="btn btn-primary"
              onClick={handleStartMatch}
              disabled={loading || (room?.participants || []).some((participant) => !participant.ready)}
            >
              Pornește duelul
            </button>
          )}
        </div>
        <p className="mt-3 text-secondary mb-0">
          Duelul poate începe când amândoi jucătorii sunt în cameră și au bifat "Pregătit".
        </p>
      </div>
    </div>
  );

  const getOptionVisualState = (optionKey) => {
    if (!currentQuestion) {
      return { extraClasses: '', badge: null };
    }
    if (currentReveal) {
      if (currentReveal.correctOption === optionKey) {
        return {
          extraClasses: 'border-success bg-success-subtle text-success fw-semibold',
          badge: { text: 'Corect', variant: 'success' },
        };
      }
      if (currentReveal.playerOption === optionKey && currentReveal.correct === false) {
        return {
          extraClasses: 'border-danger bg-danger-subtle text-danger',
          badge: { text: 'Greșit', variant: 'danger' },
        };
      }
    }
    if (selectedAnswer === optionKey) {
      return {
        extraClasses: 'border-primary bg-primary-subtle',
        badge: { text: 'Trimis', variant: 'primary' },
      };
    }
    return { extraClasses: '', badge: null };
  };

  const renderQuestionCard = () => (
    <div className="card shadow-sm border-0">
      <div className="card-body">
        {!currentQuestion ? (
          <div className="text-center py-5">
            <p className="text-secondary mb-2">Așteptăm următoarea întrebare sau rezultatele finale.</p>
            <p className="mb-0">Rămâi pe poziții – fiecare secundă contează într-un duel 1v1.</p>
          </div>
        ) : (
          <>
            <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
              <div className="d-flex align-items-center gap-2">
                <span className="badge text-bg-primary">Întrebarea {displayQuestionNumber || '?'}</span>
                {questionTimeLeft != null && (
                  <span className={`badge ${questionTimeLeft <= 5 ? 'text-bg-danger' : 'text-bg-dark'}`}>
                    {questionTimeLeft}s
                  </span>
                )}
              </div>
              {(currentQuestion.answered || selectedAnswer !== null) && (
                <span className="badge text-bg-success">Răspuns trimis</span>
              )}
            </div>
            {questionProgress != null && (
              <div className="progress bg-secondary-subtle mb-3" style={{ height: '6px' }}>
                <div className="progress-bar bg-primary" style={{ width: `${questionProgress}%` }} />
              </div>
            )}
            <p className="lead mb-4">{currentQuestion.text}</p>
            <div className="list-group">
              {[currentQuestion.optionA, currentQuestion.optionB, currentQuestion.optionC, currentQuestion.optionD].map((text, index) => {
                const { extraClasses, badge } = getOptionVisualState(index);
                const optionDisabled = currentQuestion.answered || duelState?.duelCompleted || selectedAnswer !== null || answering;
                return (
                  <button
                    key={`option-${index}`}
                    type="button"
                    className={`list-group-item list-group-item-action d-flex justify-content-between align-items-center py-3 ${extraClasses}`}
                    onClick={() => handleAnswerSelect(index)}
                    disabled={optionDisabled}
                  >
                    <span className="d-flex align-items-center gap-3">
                      <span className="badge text-bg-light rounded-pill">{answerLabels[index]}</span>
                      <span>{text || '—'}</span>
                    </span>
                    {badge && <span className={`badge text-bg-${badge.variant}`}>{badge.text}</span>}
                  </button>
                );
              })}
            </div>
            <div className="d-flex justify-content-between align-items-center mt-4">
              <p className="mb-0 text-secondary">
                Răspunsuri corecte: {session?.correctAnswers ?? 0} / {session?.totalQuestions ?? questions.length}
              </p>
              <small className="text-secondary text-end">
                Selectează o opțiune pentru a trimite instant răspunsul.
              </small>
            </div>
          </>
        )}
      </div>
    </div>
  );

  const renderResultBanner = () => {
    if (!duelState?.duelCompleted) {
      return null;
    }
    const winner = leaderboard.find((entry) => entry.userId === duelState.winnerUserId);
    const isWinner = duelState.winnerUserId && duelState.winnerUserId === user?.id;
    return (
      <div className={`alert ${isWinner ? 'alert-success' : 'alert-info'} mt-4`} role="alert">
        {winner ? (
          <>
            <strong>{winner.username}</strong> a câștigat duelul cu {winner.score ?? 0} puncte.
            {isWinner ? ' Felicitări!' : ' Încearcă o revanșă!'}
          </>
        ) : (
          'Duelul s-a încheiat. Este egalitate perfectă!'
        )}
      </div>
    );
  };

  if (!room) {
    return renderCreateOrJoin();
  }

  return (
    <div className="container py-5">
      <div className="rounded-4 border bg-white shadow-sm p-4 p-lg-5 mb-4">
        <div className="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-4">
          <div>
            <p className="text-uppercase text-secondary small fw-semibold mb-1">Camera #{room.id}</p>
            <h1 className="h4 mb-1">Duel 1v1 · Cod {room.code}</h1>
            <p className="mb-0 text-secondary">
              {room.questionCount} întrebări · {room.difficulty || 'Dificultate mixtă'} · {room.timePerQuestion}s / întrebare
            </p>
          </div>
          <div className="d-flex gap-2 flex-wrap">
            <span className={`badge ${roomStatusMeta.badge}`}>{roomStatusMeta.label}</span>
            <button type="button" className="btn btn-outline-secondary" onClick={() => navigate('/home')}>
              Home
            </button>
            <button type="button" className="btn btn-outline-danger" onClick={handleLeaveRoom}>
              Părăsește
            </button>
          </div>
        </div>
        <div className="row g-3 text-center">
          <div className="col-12 col-md-4">
            <div className="border rounded-3 py-3 h-100">
              <p className="text-secondary small mb-1">Întrebări</p>
              <h3 className="h4 mb-0">{duelSummary.questionCount}</h3>
            </div>
          </div>
          <div className="col-12 col-md-4">
            <div className="border rounded-3 py-3 h-100">
              <p className="text-secondary small mb-1">Timp/întrebare</p>
              <h3 className="h4 mb-0">{duelSummary.timePerQuestion}s</h3>
            </div>
          </div>
          <div className="col-12 col-md-4">
            <div className="border rounded-3 py-3 h-100">
              <p className="text-secondary small mb-1">Dificultate</p>
              <h3 className="h5 mb-0">{duelSummary.difficulty}</h3>
            </div>
          </div>
        </div>
      </div>

      {status.message && (
        <div className={`alert alert-${status.type || 'secondary'}`} role="alert">
          {status.message}
        </div>
      )}

      {renderInvitesPanel()}

      <div className="row g-4">
        <div className="col-12 col-lg-8">
          {room.status === 'IN_PROGRESS' ? renderQuestionCard() : renderWaitingArea()}
          {lastSubmission && (
            <div className={`alert mt-3 ${lastSubmission.correct ? 'alert-success' : 'alert-danger'}`} role="alert">
              {lastSubmission.correct ? 'Răspuns corect!' : 'Ai greșit de data asta.'} +{lastSubmission.pointsEarned || 0} puncte.
            </div>
          )}
          {renderResultBanner()}
        </div>
        <div className="col-12 col-lg-4">
          {renderParticipants()}
          <div className="mt-4">{renderScoreboard()}</div>
          {renderOnlinePlayersCard()}
        </div>
      </div>

      {room.status === 'IN_PROGRESS' && allQuestionsAnswered && !duelState?.duelCompleted && (
        <div className="alert alert-secondary mt-4" role="alert">
          Așteptăm finalizarea rundelor la adversar. Poți urmări scorul live în panoul din dreapta.
        </div>
      )}
    </div>
  );
};

export default DuelRoom;
