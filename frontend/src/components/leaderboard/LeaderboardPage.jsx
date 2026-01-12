import React, { useEffect, useMemo, useState, useCallback } from 'react';
import leaderboardService from '../../services/leaderboardService';
import { useAuth } from '../../context/AuthContext';

const LeaderboardPage = () => {
  const { user } = useAuth();
  const [leaderboardData, setLeaderboardData] = useState({ global: [] });
  const [leaderboardStatus, setLeaderboardStatus] = useState({ loading: true, error: '' });
  const [playerStats, setPlayerStats] = useState(null);
  const [playerStatus, setPlayerStatus] = useState({ loading: Boolean(user?.id), error: '' });

  const loadLeaderboard = useCallback(async () => {
    setLeaderboardStatus({ loading: true, error: '' });
    try {
      const response = await leaderboardService.getLeaderboard();
      setLeaderboardData(response || { global: [] });
      setLeaderboardStatus({ loading: false, error: '' });
    } catch (error) {
      setLeaderboardStatus({
        loading: false,
        error: error?.message || 'Nu am putut încărca clasamentul global.',
      });
    }
  }, []);

  const loadPlayerStats = useCallback(async () => {
    if (!user?.id) {
      setPlayerStatus({ loading: false, error: '' });
      return;
    }
    setPlayerStatus({ loading: true, error: '' });
    try {
      const response = await leaderboardService.getPlayerStats(user.id);
      setPlayerStats(response);
      setPlayerStatus({ loading: false, error: '' });
    } catch (error) {
      setPlayerStatus({
        loading: false,
        error: error?.message || 'Nu am putut încărca datele tale.',
      });
    }
  }, [user?.id]);

  useEffect(() => {
    loadLeaderboard();
  }, [loadLeaderboard]);

  useEffect(() => {
    loadPlayerStats();
  }, [loadPlayerStats]);

  const formatDate = (value) => {
    if (!value) {
      return '-';
    }
    return new Date(value).toLocaleString();
  };

  const renderLeaderboardTable = useMemo(() => {
    const entries = leaderboardData?.global || [];
    if (!entries.length) {
      return <p className="text-secondary mb-0">Încă nu există scoruri înregistrate.</p>;
    }
    return (
      <div className="table-responsive">
        <table className="table table-hover align-middle mb-0">
          <thead>
            <tr>
              <th>Loc</th>
              <th>Jucător</th>
              <th>Puncte totale</th>
              <th>Sesiuni</th>
              <th>Acuratețe medie</th>
              <th>Ultima sesiune</th>
            </tr>
          </thead>
          <tbody>
            {entries.map((entry, index) => {
              const isCurrentUser = entry.userId === user?.id;
              const rank = entry.rank ?? index + 1;
              const accuracy = entry.accuracy != null ? `${entry.accuracy.toFixed(1)}%` : '-';
              return (
                <tr key={entry.scoreId || `${entry.userId}-${rank}`} className={isCurrentUser ? 'table-success' : ''}>
                  <td>#{rank}</td>
                  <td>
                    <div className="fw-semibold">{entry.username || 'Anonim'}</div>
                    <div className="text-secondary small">ID utilizator: {entry.userId ?? '-'}</div>
                  </td>
                  <td>{entry.totalPoints ?? 0}</td>
                  <td>{entry.totalSessions ?? '-'}</td>
                  <td>{accuracy}</td>
                  <td>{formatDate(entry.createdAt)}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    );
  }, [leaderboardData?.global, user?.id]);

  return (
    <div className="container py-5">
      <div className="row g-4">
        <div className="col-12">
          <div className="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
            <div>
              <h1 className="h3 fw-bold mb-1">Clasament global</h1>
              <p className="text-secondary mb-0">Top 10 jucători după punctajul total cumulat.</p>
            </div>
            <button type="button" className="btn btn-outline-primary" onClick={loadLeaderboard} disabled={leaderboardStatus.loading}>
              {leaderboardStatus.loading ? 'Se încarcă...' : 'Reîncarcă datele'}
            </button>
          </div>
        </div>

        <div className="col-12 col-lg-8">
          <div className="card shadow-sm border-0 h-100">
            <div className="card-body">
              {leaderboardStatus.loading && <p className="text-secondary mb-0">Se încarcă clasamentul...</p>}
              {!leaderboardStatus.loading && leaderboardStatus.error && (
                <div className="alert alert-warning mb-0" role="alert">
                  {leaderboardStatus.error}
                </div>
              )}
              {!leaderboardStatus.loading && !leaderboardStatus.error && renderLeaderboardTable}
            </div>
          </div>
        </div>

        <div className="col-12 col-lg-4">
          <div className="card shadow-sm border-0 h-100">
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-center mb-2">
                <div>
                  <h2 className="h5 mb-0">Statistici personale</h2>
                  <p className="text-secondary mb-0">Progresul tău recent.</p>
                </div>
                <button
                  type="button"
                  className="btn btn-sm btn-outline-secondary"
                  onClick={loadPlayerStats}
                  disabled={playerStatus.loading}
                >
                  {playerStatus.loading ? '...' : 'Actualizează'}
                </button>
              </div>
              {!user?.id && <p className="text-secondary mb-0">Autentifică-te pentru a-ți vedea statisticile.</p>}
              {user?.id && playerStatus.loading && <p className="text-secondary mb-0">Se încarcă...</p>}
              {user?.id && !playerStatus.loading && playerStatus.error && (
                <div className="alert alert-warning" role="alert">
                  {playerStatus.error}
                </div>
              )}
              {user?.id && !playerStatus.loading && !playerStatus.error && playerStats && (
                <>
                  <div className="mb-3">
                    <div className="fw-semibold">{playerStats.username}</div>
                    <div className="text-secondary small">ID #{playerStats.userId}</div>
                    <div className="text-secondary small">Ultima sesiune: {formatDate(playerStats.lastPlayedAt)}</div>
                  </div>
                  <div className="row g-2 mb-3">
                    <div className="col-6">
                      <div className="border rounded p-3 text-center">
                        <div className="text-secondary small">Sesiuni</div>
                        <div className="h5 mb-0">{playerStats.totalSessions ?? 0}</div>
                      </div>
                    </div>
                    <div className="col-6">
                      <div className="border rounded p-3 text-center">
                        <div className="text-secondary small">Record</div>
                        <div className="h5 mb-0">{playerStats.bestScore ?? 0}</div>
                      </div>
                    </div>
                    <div className="col-12">
                      <div className="border rounded p-3 text-center">
                        <div className="text-secondary small">Medie puncte</div>
                        <div className="h5 mb-0">{playerStats.averageScore != null ? playerStats.averageScore.toFixed(1) : '0.0'}</div>
                      </div>
                    </div>
                  </div>
                  <div>
                    <h3 className="h6">Rezultate recente</h3>
                    {!playerStats.recentScores?.length && (
                      <p className="text-secondary mb-0">Nu avem încă sesiuni salvate.</p>
                    )}
                    {playerStats.recentScores?.length > 0 && (
                      <ul className="list-unstyled mb-0">
                        {playerStats.recentScores.map((score) => (
                          <li key={score.scoreId} className="border rounded px-3 py-2 mb-2">
                            <div className="d-flex justify-content-between">
                              <span>{score.quizTitle || 'Quiz liber'}</span>
                              <strong>{score.totalPoints}p</strong>
                            </div>
                            <div className="text-secondary small">
                              {formatDate(score.createdAt)} · {score.correctAnswers}/{score.totalQuestions} corecte
                            </div>
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default LeaderboardPage;
