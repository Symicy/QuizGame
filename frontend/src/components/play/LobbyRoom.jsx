import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import lobbyService from '../../services/lobbyService';
import useLobbyEvents from '../../hooks/useLobbyEvents';

const eventHandlers = {
  LOBBY_COUNTDOWN: (payload, setState) => {
    setState((prev) => ({
      ...prev,
      phase: 'COUNTDOWN',
      countdownEndsAt: payload.countdownEndsAt,
      nextRoundNumber: payload.roundNumber,
      roundSummary: null,
      currentQuestion: null,
      questionEndsAt: null,
      resultsEndsAt: null,
      playerAnswered: false,
      activePlayers: payload.activePlayers ?? prev.activePlayers,
      selectedAnswer: null,
      answerWasCorrect: null,
    }));
  },
  LOBBY_QUESTION: (payload, setState) => {
    setState((prev) => ({
      ...prev,
      phase: 'QUESTION',
      roundNumber: payload.roundNumber,
      currentQuestion: payload.question,
      questionEndsAt: payload.endsAt,
      questionIndex: payload.questionIndex,
      totalQuestions: payload.totalQuestions,
      playerAnswered: false,
      countdownEndsAt: null,
      resultsEndsAt: null,
      roundSummary: null,
      loading: false,
      selectedAnswer: null,
      answerWasCorrect: null,
    }));
  },
  LOBBY_QUESTION_RESULT: (payload, setState) => {
    setState((prev) => ({
      ...prev,
      currentQuestion: prev.currentQuestion
        ? {
            ...prev.currentQuestion,
            correctOption: payload.correctOption,
          }
        : prev.currentQuestion,
      leaderboard: payload.leaderboard,
      activePlayers: payload.leaderboard?.length ?? prev.activePlayers,
    }));
  },
  LOBBY_ROUND_ENDED: (payload, setState) => {
    setState((prev) => ({
      ...prev,
      phase: 'RESULTS',
      roundSummary: payload,
      leaderboard: prev.leaderboard?.length ? prev.leaderboard : payload.topPlayers,
      currentQuestion: null,
      questionEndsAt: null,
      countdownEndsAt: null,
      resultsEndsAt: prev.resultsEndsAt,
      playerAnswered: false,
      selectedAnswer: null,
      answerWasCorrect: null,
    }));
  },
  LOBBY_SCORE_UPDATE: (payload, setState) => {
    setState((prev) => ({
      ...prev,
      leaderboard: payload,
      activePlayers: payload?.length ?? prev.activePlayers,
    }));
  },
  ROOM_UPDATED: (payload, setState) => {
    setState((prev) => ({
      ...prev,
      room: payload,
      activePlayers: payload?.currentPlayers ?? prev.activePlayers,
      roomCode: payload?.code || prev.roomCode,
    }));
  },
};

const LobbyRoom = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [state, setState] = useState({
    loading: true,
    roomCode: null,
    phase: 'WAITING',
    roundNumber: 0,
    nextRoundNumber: 1,
    currentQuestion: null,
    questionIndex: 0,
    totalQuestions: 0,
    questionEndsAt: null,
    countdownEndsAt: null,
    resultsEndsAt: null,
    leaderboard: [],
    roundSummary: null,
    playerAnswered: false,
    playerScore: null,
    playerRank: null,
    activePlayers: 0,
    room: null,
    isParticipant: false,
    selectedAnswer: null,
    answerWasCorrect: null,
  });
  const [joining, setJoining] = useState(false);
  const [answering, setAnswering] = useState(false);
  const [error, setError] = useState('');
  const [now, setNow] = useState(Date.now());

  const phaseMeta = useMemo(() => {
    switch (state.phase) {
      case 'QUESTION':
        return { label: 'Întrebări în desfășurare', badge: 'text-bg-primary' };
      case 'COUNTDOWN':
        return { label: 'Începe o rundă nouă', badge: 'text-bg-warning' };
      case 'RESULTS':
        return { label: 'Rezultate afișate', badge: 'text-bg-info' };
      default:
        return { label: 'Așteptăm jucători', badge: 'text-bg-secondary' };
    }
  }, [state.phase]);

  const heroStats = useMemo(() => ({
    activePlayers: state.activePlayers || state.room?.currentPlayers || 0,
    round: state.roundNumber || 0,
    playerScore: state.playerScore ?? 0,
  }), [state.activePlayers, state.room?.currentPlayers, state.roundNumber, state.playerScore]);

  useEffect(() => {
    const timer = setInterval(() => {
      setNow(Date.now());
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  const activeTimer = useMemo(() => {
    if (state.phase === 'QUESTION') {
      return state.questionEndsAt;
    }
    if (state.phase === 'COUNTDOWN') {
      return state.countdownEndsAt;
    }
    if (state.phase === 'RESULTS') {
      return state.resultsEndsAt;
    }
    return null;
  }, [state.phase, state.countdownEndsAt, state.questionEndsAt, state.resultsEndsAt]);

  const timeRemaining = useMemo(() => {
    if (!activeTimer) {
      return null;
    }
    const diffMs = new Date(activeTimer).getTime() - now;
    return Math.max(Math.round(diffMs / 1000), 0);
  }, [activeTimer, now]);

  const refreshState = useCallback(async () => {
    if (!user?.id) {
      return null;
    }
    try {
      const lobbyState = await lobbyService.getState(user.id);
      setError('');
      setState((prev) => {
        const nextQuestion = lobbyState.currentQuestion;
        const questionChanged = (nextQuestion?.id ?? null) !== (prev.currentQuestion?.id ?? null);
        const mergedQuestion = nextQuestion
          ? {
              ...nextQuestion,
              correctOption: questionChanged
                ? nextQuestion?.correctOption ?? null
                : nextQuestion?.correctOption ?? prev.currentQuestion?.correctOption ?? null,
            }
          : null;
        return {
          ...prev,
          loading: false,
          roomCode: lobbyState.roomCode || prev.roomCode,
          phase: lobbyState.phase || prev.phase,
          roundNumber: lobbyState.roundNumber ?? prev.roundNumber,
          nextRoundNumber: lobbyState.roundNumber != null ? lobbyState.roundNumber + 1 : prev.nextRoundNumber,
          currentQuestion: mergedQuestion,
          questionIndex:
            lobbyState.currentQuestionIndex != null
              ? Math.max(lobbyState.currentQuestionIndex + 1, 1)
              : prev.questionIndex,
          totalQuestions: lobbyState.totalQuestions ?? prev.totalQuestions,
          questionEndsAt: lobbyState.questionEndsAt,
          countdownEndsAt: lobbyState.countdownEndsAt,
          resultsEndsAt: lobbyState.resultsEndsAt,
          leaderboard: lobbyState.leaderboard || prev.leaderboard,
          playerScore: lobbyState.playerScore ?? prev.playerScore,
          playerRank: lobbyState.playerRank ?? prev.playerRank,
          playerAnswered: lobbyState.playerAnswered ?? prev.playerAnswered,
          activePlayers: lobbyState.activePlayers ?? prev.activePlayers,
          isParticipant: Boolean(lobbyState.participant),
          roundSummary: lobbyState.phase === 'RESULTS' ? prev.roundSummary : null,
          selectedAnswer: questionChanged ? null : prev.selectedAnswer,
          answerWasCorrect: questionChanged ? null : prev.answerWasCorrect,
        };
      });
      return lobbyState;
    } catch (err) {
      setState((prev) => ({
        ...prev,
        loading: false,
      }));
      setError(err?.response?.data?.message || err.message || 'Nu am putut încărca starea lobby-ului.');
      return null;
    }
  }, [user?.id]);

  useEffect(() => {
    if (!user?.id) {
      return undefined;
    }
    let cancelled = false;
    const ensureMembership = async () => {
      setJoining(true);
      try {
        const stateSnapshot = await refreshState();
        if (cancelled) {
          return;
        }
        if (stateSnapshot?.participant) {
          return;
        }
        const response = await lobbyService.join(user.id);
        if (!cancelled) {
          setError('');
          setState((prev) => ({
            ...prev,
            roomCode: response.code,
            loading: false,
            activePlayers: response.currentPlayers ?? prev.activePlayers,
            room: response,
            isParticipant: true,
          }));
          await refreshState();
        }
      } catch (err) {
        if (!cancelled) {
          setError(err?.response?.data?.message || err.message || 'Nu am putut intra în lobby.');
        }
      } finally {
        if (!cancelled) {
          setJoining(false);
        }
      }
    };
    ensureMembership();
    return () => {
      cancelled = true;
      lobbyService.leave(user.id).catch(() => {});
    };
  }, [user?.id, refreshState]);

  useEffect(() => {
    if (!user?.id || !state.roomCode) {
      return undefined;
    }
    const interval = setInterval(() => {
      refreshState();
    }, 2000);
    return () => clearInterval(interval);
  }, [user?.id, state.roomCode, refreshState]);

  const handleSocketMessage = useCallback((message) => {
    const handler = eventHandlers[message.type];
    if (handler) {
      handler(message.payload, setState);
    }
  }, []);

  useLobbyEvents({ roomCode: state.roomCode, onMessage: handleSocketMessage });

  const handleManualRefresh = useCallback(() => {
    setState((prev) => ({
      ...prev,
      loading: true,
    }));
    refreshState();
  }, [refreshState]);

  const handleAnswer = async (answerIndex) => {
    if (!state.currentQuestion || state.playerAnswered || !user?.id) {
      return;
    }
    setAnswering(true);
    try {
      const response = await lobbyService.submitAnswer({
        userId: user.id,
        questionId: state.currentQuestion.id,
        answerIndex,
      });
      setState((prev) => {
        const accepted = Boolean(response?.accepted);
        const updatedQuestion =
          response?.correctOption != null && prev.currentQuestion
            ? { ...prev.currentQuestion, correctOption: response.correctOption }
            : prev.currentQuestion;
        return {
          ...prev,
          playerAnswered: accepted || prev.playerAnswered,
          playerScore: response?.playerScore ?? prev.playerScore,
          playerRank: response?.playerRank ?? prev.playerRank,
          currentQuestion: updatedQuestion,
          selectedAnswer: accepted ? answerIndex : prev.selectedAnswer,
          answerWasCorrect: accepted ? response?.correct ?? null : prev.answerWasCorrect,
        };
      });
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Nu am putut trimite răspunsul.');
    } finally {
      setAnswering(false);
    }
  };

  const renderTimer = () => {
    if (timeRemaining == null) {
      return null;
    }
    const upcomingRoundLabel = state.nextRoundNumber || (state.roundNumber ? state.roundNumber + 1 : '');
    const labels = {
      QUESTION: 'Timp rămas pentru întrebare',
      COUNTDOWN: upcomingRoundLabel ? `Runda ${upcomingRoundLabel} începe în` : 'Runda începe în',
      RESULTS: 'Următoarea rundă pornește în',
    };
    const label = labels[state.phase] || 'Timp rămas';
    return (
      <div className="alert alert-info text-center" role="alert">
        {label}: <strong>{timeRemaining}s</strong>
      </div>
    );
  };

  const renderQuestion = () => {
    if (!state.currentQuestion) {
      const upcomingRound = state.nextRoundNumber || (state.roundNumber ? state.roundNumber + 1 : 1);
      const currentRound = state.roundNumber || (upcomingRound > 1 ? upcomingRound - 1 : 0);
      const messageByPhase = {
        WAITING: `Așteptăm suficienți jucători pentru o rundă nouă (activi acum: ${state.activePlayers || 0}).`,
        COUNTDOWN: `Runda ${upcomingRound} începe imediat.`,
        RESULTS: currentRound
          ? `Runda ${currentRound} s-a încheiat. Pregătește-te pentru următoarea!`
          : 'Pregătim următoarea rundă.',
      };
      const message = messageByPhase[state.phase] || 'Așteptăm următoarea întrebare...';
      return (
        <div className="card shadow-sm border-0">
          <div className="card-body text-center py-5">
            <p className="text-secondary mb-2">Runda curentă: {currentRound || '-'}</p>
            <h2 className="h4 mb-3">{message}</h2>
            <p className="text-secondary mb-0">Rămâi pregătit – vei primi o notificare imediat ce apare o întrebare.</p>
          </div>
        </div>
      );
    }
    const questionNumber = state.questionIndex && state.questionIndex > 0 ? state.questionIndex : '?';
    const totalQuestions = state.totalQuestions || '?';
    const options = [
      { key: 0, label: 'A', text: state.currentQuestion.optionA },
      { key: 1, label: 'B', text: state.currentQuestion.optionB },
      { key: 2, label: 'C', text: state.currentQuestion.optionC },
      { key: 3, label: 'D', text: state.currentQuestion.optionD },
    ];

    return (
      <div className="card shadow-sm border-0">
        <div className="card-body">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <div className="d-flex gap-2 align-items-center">
              <span className="badge text-bg-primary">Întrebarea {questionNumber}/{totalQuestions}</span>
              {state.playerAnswered && <span className="badge text-bg-success">Răspuns trimis</span>}
            </div>
            <span className="badge text-bg-light text-secondary">{state.currentQuestion.difficulty || '---'}</span>
          </div>
          <p className="lead">{state.currentQuestion.text}</p>
          <div className="list-group mt-4">
            {options.map((option) => {
              const correctOption =
                state.currentQuestion?.correctOption !== undefined && state.currentQuestion?.correctOption !== null
                  ? Number(state.currentQuestion.correctOption)
                  : null;
              const hasCorrectOption = correctOption !== null && Number.isFinite(correctOption);
              const isSelected = state.selectedAnswer === option.key;
              const buttonDisabled = state.playerAnswered || state.phase !== 'QUESTION' || answering;

              let extraClasses = '';
              let badge = null;
              if (hasCorrectOption) {
                if (option.key === correctOption) {
                  extraClasses = 'border-success bg-success-subtle text-success fw-semibold';
                  badge = { text: 'Corect', variant: 'success' };
                } else if (isSelected && state.answerWasCorrect === false) {
                  extraClasses = 'border-danger bg-danger-subtle text-danger';
                  badge = { text: 'Greșit', variant: 'danger' };
                }
              } else if (isSelected) {
                extraClasses = 'border-primary bg-primary-subtle';
                badge = { text: 'Selectat', variant: 'primary' };
              }

              return (
                <button
                  key={option.key}
                  type="button"
                  className={`list-group-item list-group-item-action d-flex justify-content-between align-items-center py-3 ${extraClasses}`}
                  onClick={() => handleAnswer(option.key)}
                  disabled={buttonDisabled}
                >
                  <span className="d-flex align-items-center gap-3">
                    <span className="badge text-bg-light rounded-pill">{option.label}</span>
                    <span>{option.text || '—'}</span>
                  </span>
                  {badge && <span className={`badge text-bg-${badge.variant}`}>{badge.text}</span>}
                </button>
              );
            })}
          </div>
          {state.playerAnswered && state.phase === 'QUESTION' && state.answerWasCorrect != null && (
            <p className={state.answerWasCorrect ? 'text-success mt-3 mb-0' : 'text-danger mt-3 mb-0'}>
              {state.answerWasCorrect ? 'Răspuns corect!' : 'Răspuns greșit. Corectul este evidențiat mai sus.'}
            </p>
          )}
        </div>
      </div>
    );
  };

  const renderLeaderboard = () => {
    if (!state.leaderboard || state.leaderboard.length === 0) {
      return <p className="text-secondary mb-0">Niciun scor disponibil încă.</p>;
    }
    return (
      <div className="list-group list-group-flush">
        {state.leaderboard.map((entry) => (
          <div
            key={entry.userId}
            className={`list-group-item d-flex justify-content-between align-items-center ${entry.userId === user?.id ? 'bg-primary-subtle' : ''}`}
          >
            <div>
              <div className="fw-semibold">#{entry.rank ?? '-'} · {entry.username}</div>
              <small className="text-secondary">{entry.correctAnswers} răspunsuri corecte</small>
            </div>
            <span className="badge text-bg-dark">{entry.score}p</span>
          </div>
        ))}
      </div>
    );
  };

  const renderRoundSummary = () => {
    const topPlayers = state.roundSummary?.topPlayers || [];
    if (!state.roundSummary || topPlayers.length === 0) {
      return null;
    }
    const playerTop = topPlayers.find((entry) => entry.userId === user?.id);
    return (
      <div className="alert alert-info" role="alert">
        <h2 className="h5 mb-2">Top 3 runda {state.roundSummary.roundNumber}</h2>
        <ol className="mb-2">
          {topPlayers.map((entry) => (
            <li key={entry.userId}>
              {entry.username} - {entry.score} puncte
            </li>
          ))}
        </ol>
        {playerTop && <p className="mb-2">Ești în top 3 cu {playerTop.score} puncte!</p>}
        <p className="mb-0">Runda {state.nextRoundNumber || state.roundSummary.roundNumber + 1} începe în câteva secunde.</p>
      </div>
    );
  };

  if (state.loading) {
    return (
      <div className="container py-5 text-center">
        <div className="spinner-border" role="status" />
        <p className="mt-3 mb-0">Se încarcă lobby-ul...</p>
      </div>
    );
  }

  return (
    <div className="container py-5">
      <div className="rounded-4 border bg-white shadow-sm p-4 p-lg-5 mb-4">
        <div className="d-flex flex-wrap justify-content-between gap-3 align-items-center">
          <div>
            <p className="text-uppercase text-secondary small fw-semibold mb-1">Lobby public</p>
            <h1 className="h4 mb-1">Cod: {state.roomCode || '---'}</h1>
            <p className="mb-0 text-secondary">Participanți activi: <strong>{heroStats.activePlayers}</strong></p>
          </div>
          <div className="d-flex flex-wrap gap-2">
            <span className={`badge ${phaseMeta.badge} align-self-center`}>{phaseMeta.label}</span>
            <button
              type="button"
              className="btn btn-outline-primary"
              onClick={handleManualRefresh}
              disabled={state.loading || joining}
            >
              Reîncarcă starea
            </button>
            <button type="button" className="btn btn-outline-secondary" onClick={() => navigate('/home')}>
              Înapoi la home
            </button>
          </div>
        </div>
      </div>

      {error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}

      {renderTimer()}

      <div className="row g-4">
        <div className="col-12 col-lg-8">
          {renderQuestion()}
          {renderRoundSummary()}
        </div>
        <div className="col-12 col-lg-4">
          <div className="card shadow-sm border-0 mb-4">
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h2 className="h5 mb-0">Clasament live</h2>
                <span className="text-secondary small">Actualizat în timp real</span>
              </div>
              {renderLeaderboard()}
            </div>
          </div>
          <div className="card shadow-sm border-0">
            <div className="card-body">
              <div className="mb-3">
                <p className="text-uppercase text-secondary small fw-semibold mb-1">Lobby</p>
                <div className="d-flex justify-content-between">
                  <span>Runda curentă</span>
                  <strong>{state.roundNumber || '-'}</strong>
                </div>
                <div className="d-flex justify-content-between text-secondary">
                  <span>Next</span>
                  <span>{state.nextRoundNumber || '-'}</span>
                </div>
              </div>
              <div className="border-top pt-3">
                <p className="text-uppercase text-secondary small fw-semibold mb-1">Statistici personale</p>
                <div className="d-flex justify-content-between">
                  <span>Scor curent</span>
                  <strong>{state.playerScore ?? '-'}</strong>
                </div>
                <div className="d-flex justify-content-between">
                  <span>Loc curent</span>
                  <strong>{state.playerRank ?? '-'}</strong>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LobbyRoom;
