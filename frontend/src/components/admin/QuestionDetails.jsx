import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import categoryService from '../../services/categoryService';
import questionService from '../../services/questionService';

const difficultyOptions = [
  { label: 'Ușor', value: 'EASY' },
  { label: 'Mediu', value: 'MEDIUM' },
  { label: 'Dificil', value: 'HARD' }
];

const sourceOptions = [
  { label: 'Admin', value: 'ADMIN' },
  { label: 'OpenTrivia', value: 'API_IMPORT' }
];

const buildEmptyForm = () => ({
  text: '',
  optionA: '',
  optionB: '',
  optionC: '',
  optionD: '',
  correctOption: '0',
  difficultyLevel: 'EASY',
  explanation: '',
  points: '',
  categoryId: '',
  sourceType: 'ADMIN',
  isActive: true
});

const QuestionDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [categories, setCategories] = useState([]);
  const [form, setForm] = useState(buildEmptyForm);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [status, setStatus] = useState({ type: '', message: '' });
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const categoryOptions = useMemo(() => [
    { id: '', name: 'Selectează categoria' },
    ...categories
  ], [categories]);

  const bootstrap = useCallback(async () => {
    try {
      setLoading(true);
      setError('');
      const [cats, question] = await Promise.all([
        categoryService.getCategories(true),
        questionService.getQuestion(id)
      ]);
      setCategories(cats);
      setForm({
        text: question.text ?? '',
        optionA: question.optionA ?? '',
        optionB: question.optionB ?? '',
        optionC: question.optionC ?? '',
        optionD: question.optionD ?? '',
        correctOption: String(question.correctOption ?? 0),
        difficultyLevel: question.difficultyLevel ?? 'EASY',
        explanation: question.explanation ?? '',
        points: question.points != null ? String(question.points) : '',
        categoryId: question.categoryId ? String(question.categoryId) : '',
        sourceType: question.sourceType ?? 'ADMIN',
        isActive: question.isActive ?? true
      });
    } catch (err) {
      setError(err.message || 'Nu am putut încărca întrebarea.');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    bootstrap();
  }, [bootstrap]);

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target;
    setForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setStatus({ type: '', message: '' });
    setSaving(true);
    try {
      const payload = {
        ...form,
        correctOption: Number(form.correctOption),
        categoryId: form.categoryId ? Number(form.categoryId) : null,
        points: form.points ? Number(form.points) : undefined,
        isActive: form.isActive
      };

      if (!payload.categoryId) {
        throw new Error('Selectează categoria înainte de salvare.');
      }

      const updated = await questionService.updateQuestion(id, payload);
      setStatus({ type: 'success', message: 'Întrebarea a fost actualizată.' });
      setForm((prev) => ({
        ...prev,
        points: updated.points != null ? String(updated.points) : ''
      }));
    } catch (err) {
      setStatus({ type: 'danger', message: err.message || 'Actualizarea a eșuat.' });
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    const confirmed = window.confirm('Sigur vrei să ștergi întrebarea aceasta?');
    if (!confirmed) {
      return;
    }
    setDeleting(true);
    setStatus({ type: '', message: '' });
    try {
      await questionService.deleteQuestion(id);
      navigate('/admin/questions');
    } catch (err) {
      setStatus({ type: 'danger', message: err.message || 'Ștergerea a eșuat.' });
    } finally {
      setDeleting(false);
    }
  };

  if (loading) {
    return (
      <div className="container py-5">
        <div className="alert alert-info">Se încarcă detaliile întrebării...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container py-5">
        <div className="alert alert-danger mb-3">{error}</div>
        <button type="button" className="btn btn-outline-secondary" onClick={() => navigate('/admin/questions')}>
          Înapoi la listă
        </button>
      </div>
    );
  }

  return (
    <div className="container py-5">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <p className="text-secondary small mb-1">Întrebarea #{id}</p>
          <h1 className="h3 mb-0">Editează întrebarea</h1>
        </div>
        <div className="d-flex gap-2">
          <button type="button" className="btn btn-outline-danger" onClick={handleDelete} disabled={deleting}>
            {deleting ? 'Se șterge...' : 'Șterge întrebarea'}
          </button>
          <button type="button" className="btn btn-outline-secondary" onClick={() => navigate('/admin/questions')}>
            Înapoi la listă
          </button>
          <Link to="/admin" className="btn btn-outline-secondary">
            Panou admin
          </Link>
        </div>
      </div>

      {status.message && (
        <div className={`alert alert-${status.type}`} role="alert">
          {status.message}
        </div>
      )}

      <div className="card shadow-sm">
        <div className="card-body">
          <form className="d-flex flex-column gap-3" onSubmit={handleSubmit}>
            <div>
              <label className="form-label" htmlFor="text">Întrebare</label>
              <textarea
                id="text"
                name="text"
                className="form-control"
                rows={3}
                value={form.text}
                onChange={handleChange}
                required
              />
            </div>

            {['A', 'B', 'C', 'D'].map((label) => (
              <div key={label}>
                <label className="form-label" htmlFor={`option${label}`}>
                  Varianta {label}
                </label>
                <input
                  id={`option${label}`}
                  name={`option${label}`}
                  type="text"
                  className="form-control"
                  value={form[`option${label}`]}
                  onChange={handleChange}
                  required
                />
              </div>
            ))}

            <div className="row g-3">
              <div className="col-12 col-md-4">
                <label className="form-label" htmlFor="correctOption">Răspuns corect</label>
                <select
                  id="correctOption"
                  name="correctOption"
                  className="form-select"
                  value={form.correctOption}
                  onChange={handleChange}
                >
                  <option value="0">Varianta A</option>
                  <option value="1">Varianta B</option>
                  <option value="2">Varianta C</option>
                  <option value="3">Varianta D</option>
                </select>
              </div>
              <div className="col-12 col-md-4">
                <label className="form-label" htmlFor="difficultyLevel">Dificultate</label>
                <select
                  id="difficultyLevel"
                  name="difficultyLevel"
                  className="form-select"
                  value={form.difficultyLevel}
                  onChange={handleChange}
                >
                  {difficultyOptions.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
              </div>
              <div className="col-12 col-md-4">
                <label className="form-label" htmlFor="sourceType">Sursa</label>
                <select
                  id="sourceType"
                  name="sourceType"
                  className="form-select"
                  value={form.sourceType}
                  onChange={handleChange}
                  disabled={form.sourceType === 'API_IMPORT'}
                >
                  {sourceOptions.map((option) => (
                    <option key={option.value} value={option.value}>{option.label}</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="row g-3">
              <div className="col-12 col-md-6">
                <label className="form-label" htmlFor="categoryId">Categorie</label>
                <select
                  id="categoryId"
                  name="categoryId"
                  className="form-select"
                  value={form.categoryId}
                  onChange={handleChange}
                  required
                >
                  {categoryOptions.map((category) => (
                    <option key={category.id || 'placeholder'} value={category.id}>{category.name}</option>
                  ))}
                </select>
              </div>
              <div className="col-12 col-md-6">
                <label className="form-label" htmlFor="points">Puncte</label>
                <input
                  id="points"
                  name="points"
                  type="number"
                  className="form-control"
                  value={form.points}
                  min="0"
                  onChange={handleChange}
                />
              </div>
            </div>

            <div>
              <label className="form-label" htmlFor="explanation">Explicație</label>
              <textarea
                id="explanation"
                name="explanation"
                className="form-control"
                rows={2}
                value={form.explanation}
                onChange={handleChange}
              />
            </div>

            <div className="form-check form-switch">
              <input
                className="form-check-input"
                type="checkbox"
                id="isActive"
                name="isActive"
                checked={form.isActive}
                onChange={handleChange}
              />
              <label className="form-check-label" htmlFor="isActive">Întrebarea este activă</label>
            </div>

            <div className="d-flex justify-content-end gap-2">
              <button type="button" className="btn btn-outline-secondary" onClick={bootstrap} disabled={saving}>
                Resetează
              </button>
              <button className="btn btn-primary" type="submit" disabled={saving}>
                {saving ? 'Se salvează...' : 'Salvează' }
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default QuestionDetails;
