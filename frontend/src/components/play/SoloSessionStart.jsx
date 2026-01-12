import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import categoryService from '../../services/categoryService';
import sessionService from '../../services/sessionService';

const difficultyOptions = [
  { label: 'Orice dificultate', value: '' },
  { label: 'Ușor', value: 'EASY' },
  { label: 'Mediu', value: 'MEDIUM' },
  { label: 'Dificil', value: 'HARD' }
];

const SoloSessionStart = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [categories, setCategories] = useState([]);
  const [loadingCategories, setLoadingCategories] = useState(true);
  const [status, setStatus] = useState({ type: '', message: '' });
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState({
    categoryId: '',
    difficulty: '',
    questionCount: '10',
    timePerQuestion: '30'
  });

  const difficultyLabels = {
    EASY: 'Dificultate ușoară',
    MEDIUM: 'Dificultate medie',
    HARD: 'Dificultate dificilă'
  };
  const selectedDifficultyLabel = form.difficulty ? (difficultyLabels[form.difficulty] || form.difficulty) : 'Dificultate mixtă';

  useEffect(() => {
    let mounted = true;
    const loadCategories = async () => {
      try {
        setLoadingCategories(true);
        const data = await categoryService.getCategories(true);
        if (mounted) {
          setCategories(data);
        }
      } catch (error) {
        if (mounted) {
          setStatus({ type: 'danger', message: error.message || 'Nu am putut încărca categoriile.' });
        }
      } finally {
        if (mounted) {
          setLoadingCategories(false);
        }
      }
    };

    loadCategories();
    return () => {
      mounted = false;
    };
  }, []);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setForm((prev) => ({
      ...prev,
      [name]: value
    }));
  };

  const validateInputs = () => {
    const questionCount = Number(form.questionCount);
    if (Number.isNaN(questionCount) || questionCount < 5 || questionCount > 50) {
      return 'Numărul de întrebări trebuie să fie între 5 și 50.';
    }

    const timePerQuestion = Number(form.timePerQuestion);
    if (Number.isNaN(timePerQuestion) || timePerQuestion < 10 || timePerQuestion > 120) {
      return 'Timpul per întrebare trebuie să fie între 10 și 120 de secunde.';
    }

    if (!user?.id) {
      return 'Nu am putut identifica utilizatorul curent.';
    }

    return '';
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setStatus({ type: '', message: '' });
    const validationMessage = validateInputs();
    if (validationMessage) {
      setStatus({ type: 'danger', message: validationMessage });
      return;
    }

    const payload = {
      userId: user.id,
      categoryId: form.categoryId ? Number(form.categoryId) : null,
      difficulty: form.difficulty || null,
      questionCount: Number(form.questionCount),
      timePerQuestion: Number(form.timePerQuestion)
    };

    setSubmitting(true);
    try {
      const session = await sessionService.startSoloSession(payload);
      navigate(`/play/solo/${session.id}`, { state: { session } });
    } catch (error) {
      setStatus({ type: 'danger', message: error.message || 'Nu am putut porni sesiunea.' });
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="container py-5">
      <div className="row gy-4">
        <div className="col-12">
          <div className="rounded-4 border bg-white shadow-sm p-4 p-lg-5 d-flex flex-column flex-lg-row align-items-lg-center justify-content-between gap-4">
            <div>
              <p className="text-uppercase text-secondary fw-semibold small mb-2">Mod single-player</p>
              <h1 className="display-6 fw-bold mb-2">Antrenează-ți reflexele</h1>
              <p className="text-secondary mb-0">
                Configurează-ți sesiunea ideală și pornește un set personalizat de întrebări. Rezultatele apar instant,
                iar progresul tău se salvează automat în istoric.
              </p>
            </div>
            <div className="text-center">
              <div className="badge text-bg-primary fs-6 px-4 py-3 rounded-pill">
                {Number(form.questionCount) || 10} întrebări · {selectedDifficultyLabel}
              </div>
            </div>
          </div>
        </div>

        <div className="col-12 col-lg-7">
          <div className="card shadow-sm border-0 h-100">
            <div className="card-body p-4">
              <h2 className="h4 mb-1">Setează parametrii</h2>
              <p className="text-secondary mb-4">Poți reveni oricând pentru a ajusta criteriile.</p>
              {status.message && (
                <div className={`alert alert-${status.type}`} role="alert">
                  {status.message}
                </div>
              )}
              <form className="d-flex flex-column gap-3" onSubmit={handleSubmit}>
                <div>
                  <label className="form-label" htmlFor="categoryId">Categorie</label>
                  <select
                    id="categoryId"
                    name="categoryId"
                    className="form-select"
                    value={form.categoryId}
                    onChange={handleChange}
                    disabled={loadingCategories}
                  >
                    <option value="">Orice categorie</option>
                    {categories.map((category) => (
                      <option key={category.id} value={category.id}>
                        {category.name}
                      </option>
                    ))}
                  </select>
                </div>

                <div>
                  <label className="form-label" htmlFor="difficulty">Dificultate</label>
                  <select
                    id="difficulty"
                    name="difficulty"
                    className="form-select"
                    value={form.difficulty}
                    onChange={handleChange}
                  >
                    {difficultyOptions.map((option) => (
                      <option key={option.value || 'ANY'} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="row g-3">
                  <div className="col-12 col-md-6">
                    <label className="form-label" htmlFor="questionCount">Număr întrebări</label>
                    <input
                      type="number"
                      id="questionCount"
                      name="questionCount"
                      className="form-control"
                      min="5"
                      max="50"
                      value={form.questionCount}
                      onChange={handleChange}
                    />
                    <div className="form-text">Între 5 și 50 întrebări.</div>
                  </div>
                  <div className="col-12 col-md-6">
                    <label className="form-label" htmlFor="timePerQuestion">Timp per întrebare (secunde)</label>
                    <input
                      type="number"
                      id="timePerQuestion"
                      name="timePerQuestion"
                      className="form-control"
                      min="10"
                      max="120"
                      value={form.timePerQuestion}
                      onChange={handleChange}
                    />
                    <div className="form-text">Valoare recomandată: 30 sec.</div>
                  </div>
                </div>

                <button className="btn btn-primary" type="submit" disabled={submitting || loadingCategories}>
                  {submitting ? 'Se generează sesiunea...' : 'Pornește sesiunea'}
                </button>
              </form>
            </div>
          </div>
        </div>

        <div className="col-12 col-lg-5">
          <div className="card shadow-sm border-0 h-100">
            <div className="card-body p-4 d-flex flex-column gap-4">
              <section>
                <p className="text-uppercase text-secondary fw-semibold small mb-2">Sfaturi rapide</p>
                <ul className="list-unstyled mb-0 text-secondary small d-flex flex-column gap-2">
                  <li>• Combina categoria și dificultatea pentru a-ți exersa punctele slabe.</li>
                  <li>• Setează mai puține întrebări pentru încălzire, apoi crește treptat nivelul.</li>
                  <li>• Folosește timpul pe întrebare pentru a simula un ritm de concurs.</li>
                </ul>
              </section>
              <section className="border rounded-3 p-3 bg-light-subtle">
                <p className="text-secondary mb-2">Ai nevoie de o provocare mai socială?</p>
                <button type="button" className="btn btn-outline-primary w-100" onClick={() => navigate('/play/duel')}>
                  Invită un prieten la duel
                </button>
              </section>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SoloSessionStart;
