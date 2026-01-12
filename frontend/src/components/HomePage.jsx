import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import sessionService from '../services/sessionService';

const HomePage = () => {
  const navigate = useNavigate();
  const { logout, user, pendingInvites } = useAuth();
  const [history, setHistory] = useState([]);
  const [historyStatus, setHistoryStatus] = useState({ loading: true, error: '' });

  const historyLoaded = !historyStatus.loading && !historyStatus.error;
  const totalSessions = history.length;
  const bestScore = history.reduce((max, session) => Math.max(max, session.finalScore || 0), 0);
  const correctAnswersSum = history.reduce((sum, session) => sum + (session.correctAnswers || 0), 0);
  const totalQuestionsSum = history.reduce((sum, session) => sum + (session.totalQuestions || 0), 0);
  const accuracy = totalQuestionsSum > 0 ? Math.round((correctAnswersSum / totalQuestionsSum) * 100) : 0;

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  useEffect(() => {
    let active = true;
    const loadHistory = async () => {
      if (!user?.id) {
        setHistoryStatus({ loading: false, error: '' });
        return;
      }
      setHistoryStatus({ loading: true, error: '' });
      try {
        const data = await sessionService.getRecentSessions(user.id);
        if (active) {
          setHistory(data || []);
          setHistoryStatus({ loading: false, error: '' });
        }
      } catch (error) {
        if (active) {
          setHistoryStatus({
            loading: false,
            error: error?.response?.data?.message || error.message || 'Nu am putut încărca istoricul sesiunilor.'
          });
        }
      }
    };

    loadHistory();
    return () => {
      active = false;
    };
  }, [user?.id]);

  const heroStyle = {
    background: 'linear-gradient(120deg, rgba(25,135,84,0.12), rgba(13,110,253,0.12))'
  };

  return (
    <div className="container py-5">
      <div className="row gy-4">
        <div className="col-12">
          <div className="rounded-4 p-4 p-md-5 border bg-white shadow-sm" style={heroStyle}>
            <p className="text-uppercase fw-semibold text-secondary mb-1">Bun venit</p>
            <h1 className="display-6 fw-bold mb-2">Salut, {user?.username || user?.email}</h1>
            <p className="text-secondary mb-3">
              Continuă progresul, provoacă un prieten la duel sau urcă în clasamentul global.
            </p>
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => navigate('/play/solo')}
            >
              Pornește un quiz acum
            </button>
          </div>
        </div>

        {pendingInvites?.length > 0 && (
          <div className="col-12">
            <div className="alert alert-info d-flex flex-wrap justify-content-between align-items-center" role="alert">
              <div>
                Ai {pendingInvites.length} invitație{pendingInvites.length > 1 ? 'i' : ''} la duel în așteptare.
              </div>
              <button type="button" className="btn btn-sm btn-primary" onClick={() => navigate('/play/duel')}>
                Vezi invitațiile
              </button>
            </div>
          </div>
        )}

        <div className="col-12 col-lg-4">
          <div className="card shadow-sm border-0 h-100">
            <div className="card-body">
              <p className="text-uppercase text-secondary fw-semibold small mb-2">Rezumat rapid</p>
              <div className="d-flex flex-column gap-3">
                <div className="border rounded-3 p-3">
                  <p className="text-secondary mb-1">Ultimele sesiuni</p>
                  <h3 className="h4 mb-0">{historyStatus.loading ? '...' : totalSessions || '—'}</h3>
                </div>
                <div className="border rounded-3 p-3">
                  <p className="text-secondary mb-1">Cel mai bun scor</p>
                  <h3 className="h4 mb-0">{historyStatus.loading ? '...' : `${bestScore}p`}</h3>
                </div>
                <div className="border rounded-3 p-3">
                  <p className="text-secondary mb-1">Acuratețe medie</p>
                  <h3 className="h4 mb-0">{historyStatus.loading ? '...' : `${accuracy}%`}</h3>
                </div>
              </div>
            </div>
            <div className="card-footer bg-white border-0 pt-0">
              <button type="button" className="btn btn-outline-danger w-100" onClick={handleLogout}>
                Logout
              </button>
            </div>
          </div>
        </div>

        <div className="col-12 col-lg-8">
          <div className="card shadow-sm border-0 h-100">
            <div className="card-body">
              <div className="d-flex flex-wrap justify-content-between align-items-center mb-3">
                <div>
                  <h2 className="h5 mb-0">Istoric sesiuni recente</h2>
                  <p className="text-secondary mb-0">Ultimele 5 încercări single-player.</p>
                </div>
                <button type="button" className="btn btn-outline-secondary btn-sm" onClick={() => navigate('/play/solo')}>
                  Pornește o sesiune nouă
                </button>
              </div>
              {historyStatus.loading && <p className="text-secondary mb-0">Se încarcă...</p>}
              {!historyStatus.loading && historyStatus.error && (
                <div className="alert alert-warning" role="alert">
                  {historyStatus.error}
                </div>
              )}
              {historyLoaded && history.length === 0 && (
                <p className="text-secondary mb-0">Nu ai încă sesiuni salvate. Încearcă un quiz!</p>
              )}
              {historyLoaded && history.length > 0 && (
                <div className="table-responsive">
                  <table className="table align-middle mb-0">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>Titlu/Categorie</th>
                        <th>Data</th>
                        <th>Scor</th>
                        <th>Status</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      {history.map((sessionItem) => (
                        <tr key={sessionItem.id}>
                          <td>#{sessionItem.id}</td>
                          <td>
                            <div className="fw-semibold">{sessionItem.quizTitle || 'Întrebări generate'}</div>
                            <div className="text-secondary small">
                              {sessionItem.categoryName || 'Categorie aleatorie'} · {sessionItem.difficulty || '---'}
                            </div>
                          </td>
                          <td>{sessionItem.createdAt ? new Date(sessionItem.createdAt).toLocaleString() : '-'}</td>
                          <td>
                            <div>{sessionItem.finalScore ?? 0} puncte</div>
                            <div className="text-secondary small">
                              {sessionItem.correctAnswers ?? 0}/{sessionItem.totalQuestions ?? '-'} corecte
                            </div>
                          </td>
                          <td>
                            <span className={`badge bg-${sessionItem.status === 'COMPLETED' ? 'success' : 'secondary'}`}>
                              {sessionItem.status}
                            </span>
                          </td>
                          <td className="text-end">
                            <button
                              type="button"
                              className="btn btn-sm btn-outline-primary"
                              onClick={() => navigate(`/play/solo/${sessionItem.id}`, { state: { session: sessionItem } })}
                            >
                              Vezi detalii
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HomePage;
