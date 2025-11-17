import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import questionService from '../../services/questionService';

const QuestionDetails = () => {
  const { id } = useParams();
  const [question, setQuestion] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const formatDifficulty = (value) => {
    switch (value) {
      case 'EASY':
        return 'Ușor';
      case 'MEDIUM':
        return 'Mediu';
      case 'HARD':
        return 'Dificil';
      default:
        return 'Necunoscută';
    }
  };

  const formatSource = (value) => {
    if (value === 'API_IMPORT') {
      return 'OpenTrivia';
    }
    if (value === 'ADMIN') {
      return 'Admin';
    }
    return '—';
  };

  const loadQuestion = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const data = await questionService.getQuestion(id);
      setQuestion(data);
    } catch (err) {
      setError(err.message || 'Nu am putut încărca întrebarea solicitată.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadQuestion();
  }, [loadQuestion]);

  const answerOptions = useMemo(() => {
    if (!question) {
      return [];
    }
    return [
      { label: 'A', value: question.optionA },
      { label: 'B', value: question.optionB },
      { label: 'C', value: question.optionC },
      { label: 'D', value: question.optionD }
    ];
  }, [question]);

  return (
    <div className="container py-5">
      <div className="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-4">
        <div>
          <p className="text-secondary small mb-1">Întrebarea #{id}</p>
          <h1 className="h3 mb-0">Detalii întrebare</h1>
        </div>
        <div className="d-flex gap-2">
          <Link to="/questions" className="btn btn-outline-secondary">Înapoi la listă</Link>
          <button type="button" className="btn btn-outline-primary" onClick={loadQuestion} disabled={loading}>
            Reîncarcă
          </button>
        </div>
      </div>

      {loading && (
        <div className="card shadow-sm">
          <div className="card-body text-center text-secondary">Se încarcă întrebare...</div>
        </div>
      )}

      {!loading && error && (
        <div className="alert alert-danger" role="alert">
          {error}
        </div>
      )}

      {!loading && !error && question && (
        <div className="row g-4">
          <div className="col-12">
            <div className="card shadow-sm h-100">
              <div className="card-body">
                <div className="d-flex gap-2 flex-wrap mb-3">
                  <span className="badge bg-light text-dark border">{formatDifficulty(question.difficultyLevel)}</span>
                  <span className={`badge ${question.isActive ? 'bg-success' : 'bg-secondary'}`}>
                    {question.isActive ? 'Activă' : 'Inactivă'}
                  </span>
                  <span className="badge bg-primary text-white">{formatSource(question.sourceType)}</span>
                </div>
                <h2 className="h4 mb-3">{question.text}</h2>
                <div className="mb-4 text-secondary">
                  Categorie: <strong>{question.categoryName || 'Fără categorie'}</strong> · Puncte: <strong>{question.points ?? 0}</strong>
                </div>
                <div className="d-flex flex-column gap-3">
                  {answerOptions.map((option, index) => (
                    <div
                      key={option.label}
                      className={`border rounded p-3 ${question.correctOption === index ? 'border-success bg-light' : ''}`}
                    >
                      <div className="d-flex justify-content-between align-items-center mb-1">
                        <strong>Varianta {option.label}</strong>
                        {question.correctOption === index && (
                          <span className="badge bg-success">Răspuns corect</span>
                        )}
                      </div>
                      <p className="mb-0">{option.value || '—'}</p>
                    </div>
                  ))}
                </div>
                {question.explanation && (
                  <div className="alert alert-info mt-4 mb-0">
                    <div className="fw-semibold mb-1">Explicație</div>
                    <p className="mb-0">{question.explanation}</p>
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default QuestionDetails;
