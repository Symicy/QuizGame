import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate, useParams } from 'react-router-dom';
import sessionService from '../../services/sessionService';

const answerLabels = ['A', 'B', 'C', 'D'];
const getOptionValue = (question, optionIndex) => {
  if (!question || optionIndex == null || optionIndex < 0 || optionIndex > 3) {
    return '';
  }
  const options = [question.optionA, question.optionB, question.optionC, question.optionD];
  return options[optionIndex] || '—';
};

const PlaySession = () => {
  const { sessionId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const initialSession = location.state?.session;

  const [session, setSession] = useState(initialSession || null);
  const [loading, setLoading] = useState(!initialSession);
  const [error, setError] = useState('');
  const [currentIndex, setCurrentIndex] = useState(0);
  const [selectedAnswer, setSelectedAnswer] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [submissionResult, setSubmissionResult] = useState(null);

  const fetchSession = useCallback(async () => {
    setLoading(true);
    try {
      const data = await sessionService.getSession(sessionId);
      setSession(data);
      setError('');
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Nu am putut încărca sesiunea.');
    } finally {
      setLoading(false);
    }
  }, [sessionId]);

  useEffect(() => {
    if (!session || String(session.id) !== String(sessionId)) {
      fetchSession();
    }
  }, [fetchSession, session, sessionId]);

  useEffect(() => {
    if (session?.questions?.length) {
      const nextIndex = session.questions.findIndex((question) => question.answered === false);
      if (nextIndex >= 0) {
        setCurrentIndex(nextIndex);
      }
    }
  }, [session]);

  const answeredIds = useMemo(() => new Set(session?.answeredQuestionIds || []), [session]);
  const totalQuestions = session?.totalQuestions || session?.questions?.length || 0;
  const answeredCount = useMemo(() => {
    if (session?.questions?.length) {
      return session.questions.filter((question) => question.answered).length;
    }
    return answeredIds.size;
  }, [answeredIds, session]);
  const progressPercent = totalQuestions > 0 ? Math.round((answeredCount / totalQuestions) * 100) : 0;
  const allQuestionsAnswered = totalQuestions > 0 && answeredCount >= totalQuestions;
  const isCompleted = session?.status === 'COMPLETED';
  const currentQuestion = session?.questions?.[currentIndex];
  const hasReviewData = useMemo(
    () => session?.questions?.some((question) => typeof question.correctOption === 'number'),
    [session]
  );
  const showReviewSection = hasReviewData && (allQuestionsAnswered || isCompleted);

  const getOptionVisualState = (optionKey) => {
    if (!currentQuestion) {
      return { className: '', badge: null };
    }
    if (typeof currentQuestion.correctOption === 'number') {
      if (currentQuestion.correctOption === optionKey) {
        return {
          className: 'border-success bg-success-subtle text-success fw-semibold',
          badge: { text: 'Corect', variant: 'success' },
        };
      }
      if (currentQuestion.selectedOption === optionKey && currentQuestion.correct === false) {
        return {
          className: 'border-danger bg-danger-subtle text-danger',
          badge: { text: 'Greșit', variant: 'danger' },
        };
      }
    }
    if (!currentQuestion.answered && selectedAnswer === optionKey) {
      return {
        className: 'border-primary bg-primary-subtle',
        badge: { text: 'Selectat', variant: 'primary' },
      };
    }
    return { className: '', badge: null };
  };

  const handleAnswerSelect = (index) => {
    if (currentQuestion?.answered || isCompleted) {
      return;
    }
    setSelectedAnswer(index);
  };

  const handleSubmitAnswer = async () => {
    if (!currentQuestion || selectedAnswer === null) {
      return;
    }

    setSubmitting(true);
    setSubmissionResult(null);
    try {
      const payload = {
        questionId: currentQuestion.id,
        answerIndex: selectedAnswer
      };
      const result = await sessionService.submitAnswer(session.id, payload);
      setSubmissionResult(result);
      await fetchSession();
      setSelectedAnswer(null);
    } catch (err) {
      setError(err?.response?.data?.message || err.message || 'Nu am putut trimite răspunsul.');
    } finally {
      setSubmitting(false);
    }
  };

  const optionItems = currentQuestion
    ? [
        { key: 0, label: 'A', text: currentQuestion.optionA },
        { key: 1, label: 'B', text: currentQuestion.optionB },
        { key: 2, label: 'C', text: currentQuestion.optionC },
        { key: 3, label: 'D', text: currentQuestion.optionD }
      ]
    : [];

  const progressText = `${Math.min(answeredCount, totalQuestions)}/${totalQuestions} întrebări rezolvate`;

  if (loading) {
    return (
      <div className="container py-5 text-center">
        <div className="spinner-border text-primary" role="status" />
        <p className="mt-3 mb-0">Se încarcă sesiunea...</p>
      </div>
    );
  }

  if (error && !session) {
    return (
      <div className="container py-5">
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
        <button type="button" className="btn btn-outline-primary" onClick={() => navigate('/home')}>
          Înapoi acasă
        </button>
      </div>
    );
  }

  if (!session) {
    return null;
  }

  return (
    <div className="container py-5">
      <div className="rounded-4 border bg-white shadow-sm p-4 p-lg-5 mb-4">
        <div className="d-flex flex-wrap justify-content-between gap-3">
          <div>
            <p className="text-uppercase text-secondary small fw-semibold mb-1">Sesiune #{session.id}</p>
            <h1 className="h4 mb-1">{session.quizTitle || 'Întrebări generate aleatoriu'}</h1>
            <p className="mb-0 text-secondary">
              {session.categoryName ? `Categorie: ${session.categoryName}` : 'Categorie aleatorie'} · {session.difficulty || 'Dificultate mixtă'}
            </p>
          </div>
          <div className="text-end">
            <p className="mb-1 fw-semibold">{progressText}</p>
            <p className="mb-1">Scor curent: {session.finalScore ?? 0}</p>
            <span className={`badge ${isCompleted ? 'text-bg-success' : 'text-bg-secondary'}`}>{session.status}</span>
          </div>
        </div>
      </div>

      {error && (
        <div className="alert alert-warning" role="alert">
          {error}
        </div>
      )}

      <div className="row gy-4">
        <div className="col-12 col-lg-8">
          <div className="card shadow-sm border-0 h-100">
            <div className="card-body p-4">
              {currentQuestion ? (
                <>
                  <div className="d-flex justify-content-between align-items-center mb-3">
                    <div className="d-flex align-items-center gap-2">
                      <span className="badge text-bg-primary">Întrebarea {currentIndex + 1}</span>
                      {currentQuestion.answered && <span className="badge text-bg-success">Răspuns trimis</span>}
                    </div>
                    <span className="text-secondary small">{session.questions?.length || 0} întrebări totale</span>
                  </div>
                  <p className="lead mb-4">{currentQuestion.text}</p>
                  <div className="list-group">
                    {optionItems.map((option) => {
                      const visualState = getOptionVisualState(option.key);
                      return (
                        <button
                          key={option.key}
                          type="button"
                          className={`list-group-item list-group-item-action fs-6 py-3 d-flex align-items-center justify-content-between text-start ${visualState.className}`}
                          onClick={() => handleAnswerSelect(option.key)}
                          disabled={currentQuestion.answered || isCompleted}
                        >
                          <span className="d-flex align-items-center gap-3">
                            <span className="badge rounded-pill text-bg-light fw-semibold">{answerLabels[option.key]}</span>
                            <span>{option.text || '—'}</span>
                          </span>
                          {visualState.badge && (
                            <span className={`badge text-bg-${visualState.badge.variant}`}>
                              {visualState.badge.text}
                            </span>
                          )}
                        </button>
                      );
                    })}
                  </div>

                  {typeof currentQuestion.correctOption === 'number' && (
                    <div className="alert alert-secondary mt-4" role="alert">
                      <p className="mb-1">
                        Răspuns corect:{' '}
                        <strong>
                          {answerLabels[currentQuestion.correctOption]}. {getOptionValue(currentQuestion, currentQuestion.correctOption)}
                        </strong>
                      </p>
                      {typeof currentQuestion.selectedOption === 'number' && (
                        <p className="mb-0">
                          Răspunsul tău:{' '}
                          <strong>
                            {answerLabels[currentQuestion.selectedOption]}. {getOptionValue(currentQuestion, currentQuestion.selectedOption)}
                          </strong>{' '}
                          {currentQuestion.correct ? '✅' : '❌'}
                        </p>
                      )}
                    </div>
                  )}

                  <div className="d-flex justify-content-between align-items-center mt-4 flex-wrap gap-2">
                    <div className="btn-group">
                      <button
                        type="button"
                        className="btn btn-outline-secondary"
                        onClick={() => setCurrentIndex((prev) => Math.max(prev - 1, 0))}
                        disabled={currentIndex === 0}
                      >
                        Înapoi
                      </button>
                      <button
                        type="button"
                        className="btn btn-outline-secondary"
                        onClick={() =>
                          setCurrentIndex((prev) =>
                            session.questions ? Math.min(prev + 1, session.questions.length - 1) : prev
                          )
                        }
                        disabled={!session.questions || currentIndex === session.questions.length - 1}
                      >
                        Înainte
                      </button>
                    </div>
                    <button
                      type="button"
                      className="btn btn-primary px-4"
                      onClick={handleSubmitAnswer}
                      disabled={currentQuestion.answered || selectedAnswer === null || submitting || isCompleted}
                    >
                      {submitting ? 'Se trimite...' : 'Trimite răspunsul'}
                    </button>
                  </div>
                </>
              ) : (
                <p className="mb-0">Nu există întrebări în această sesiune.</p>
              )}
            </div>
          </div>

          {submissionResult && (
            <div className={`alert mt-3 ${submissionResult.correct ? 'alert-success' : 'alert-danger'}`} role="alert">
              {submissionResult.correct ? 'Răspuns corect!' : 'Răspuns greșit.'} Ai acumulat {submissionResult.pointsEarned || 0} puncte.
            </div>
          )}

          {isCompleted ? (
            <div className="alert alert-info mt-4" role="alert">
              <strong>Felicitări!</strong> Ai terminat sesiunea cu {session.finalScore || 0} puncte și {session.correctAnswers || 0} răspunsuri corecte din {totalQuestions} întrebări.
              <div className="mt-2">
                <button type="button" className="btn btn-outline-primary btn-sm" onClick={() => navigate('/home')}>
                  Înapoi la home
                </button>
              </div>
            </div>
          ) : (
            <div className="alert alert-secondary mt-4" role="alert">
              Răspunde la toate întrebările pentru a salva scorul. Poți părăsi pagina în siguranță după completarea lor.
            </div>
          )}
        </div>

        <div className="col-12 col-lg-4">
          <div className="card shadow-sm border-0 mb-4">
            <div className="card-body">
              <h2 className="h6 text-uppercase text-secondary">Progres general</h2>
              <div className="progress my-3" style={{ height: '8px' }}>
                <div className="progress-bar" role="progressbar" style={{ width: `${progressPercent}%` }} />
              </div>
              <p className="mb-1">Întrebări răspunse: {answeredCount}/{totalQuestions}</p>
              <p className="mb-1">Răspunsuri corecte: {session.correctAnswers ?? 0}</p>
              <p className="mb-0">Durată (sec): {session.durationSeconds ?? '-'}</p>
            </div>
          </div>

          <div className="card shadow-sm border-0">
            <div className="card-body">
              <h2 className="h6 text-uppercase text-secondary">Navighează rapid</h2>
              <div className="d-flex flex-wrap gap-2">
                {session.questions?.map((question, index) => {
                  const isCurrent = index === currentIndex;
                  let variant = 'btn-outline-secondary';
                  if (question.answered) {
                    variant = question.correct ? 'btn-success' : 'btn-danger';
                  }
                  const className = isCurrent ? 'btn btn-primary btn-sm' : `btn btn-sm ${variant}`;
                  return (
                    <button
                      key={question.id}
                      type="button"
                      className={className}
                      onClick={() => setCurrentIndex(index)}
                    >
                      {index + 1}
                    </button>
                  );
                })}
              </div>
            </div>
          </div>
        </div>
      </div>

      {showReviewSection && (
        <div className="card shadow-sm border-0 mt-4">
          <div className="card-body">
            <div className="d-flex justify-content-between align-items-center mb-4">
              <div>
                <p className="text-uppercase text-secondary small fw-semibold mb-1">Revizuiește răspunsurile</p>
                <h2 className="h5 mb-0">Rezumat întrebare cu întrebare</h2>
              </div>
              <span className="badge text-bg-secondary">{totalQuestions} întrebări</span>
            </div>
            <div className="list-group list-group-flush">
              {session.questions?.map((question, index) => {
                const correctIndex = question.correctOption;
                if (typeof correctIndex !== 'number') {
                  return (
                    <div key={question.id} className="list-group-item">
                      <p className="fw-semibold mb-1">{index + 1}. {question.text}</p>
                      <p className="text-muted mb-0">Răspunsul corect va fi disponibil după ce finalizezi sesiunea.</p>
                    </div>
                  );
                }

                const playerIndex = question.selectedOption;
                const answeredCorrectly = question.correct === true;
                return (
                  <div key={question.id} className="list-group-item">
                    <div className="d-flex justify-content-between align-items-start">
                      <p className="fw-semibold mb-1 me-3">{index + 1}. {question.text}</p>
                      <span className={`badge ${answeredCorrectly ? 'text-bg-success' : 'text-bg-danger'}`}>
                        {answeredCorrectly ? 'Corect' : 'Greșit'}
                      </span>
                    </div>
                    <div className="row g-3">
                      <div className="col-12 col-md-6">
                        <small className="text-secondary text-uppercase">Răspuns corect</small>
                        <p className="text-success mb-0">
                          <strong>{answerLabels[correctIndex]}. {getOptionValue(question, correctIndex)}</strong>
                        </p>
                      </div>
                      <div className="col-12 col-md-6">
                        <small className="text-secondary text-uppercase">Răspunsul tău</small>
                        {typeof playerIndex === 'number' ? (
                          <p className={`${answeredCorrectly ? 'text-success' : 'text-danger'} mb-0`}>
                            <strong>{answerLabels[playerIndex]}. {getOptionValue(question, playerIndex)}</strong>
                          </p>
                        ) : (
                          <p className="text-muted mb-0">Nu ai trimis niciun răspuns pentru această întrebare.</p>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PlaySession;
