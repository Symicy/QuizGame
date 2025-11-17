import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import categoryService from '../../services/categoryService';
import questionService from '../../services/questionService';

const difficultyOptions = [
  { label: 'Toate', value: '' },
  { label: 'Ușor', value: 'EASY' },
  { label: 'Mediu', value: 'MEDIUM' },
  { label: 'Dificil', value: 'HARD' }
];

const sourceOptions = [
  { label: 'Toate', value: '' },
  { label: 'Admin', value: 'ADMIN' },
  { label: 'OpenTrivia', value: 'API_IMPORT' }
];

const pageSizeOptions = [10, 20, 50];

const QuestionList = () => {
  const [categories, setCategories] = useState([]);
  const [questions, setQuestions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [pagination, setPagination] = useState({
    page: 0,
    size: 10,
    totalPages: 0,
    totalElements: 0
  });
  const [filters, setFilters] = useState({
    categoryId: '',
    difficulty: '',
    sourceType: '',
    activeOnly: 'true'
  });
  const [deleteStatus, setDeleteStatus] = useState({ type: '', message: '' });
  const [deletingId, setDeletingId] = useState(null);

  const categoryOptions = useMemo(() => [
    { id: '', name: 'Toate categoriile' },
    ...categories
  ], [categories]);

  const buildParams = useCallback((pageOverride, sizeOverride) => {
    const params = {
      activeOnly: filters.activeOnly === 'true',
      page: pageOverride ?? pagination.page,
      size: sizeOverride ?? pagination.size
    };
    if (filters.categoryId) {
      params.categoryId = Number(filters.categoryId);
    }
    if (filters.difficulty) {
      params.difficulty = filters.difficulty;
    }
    if (filters.sourceType) {
      params.sourceType = filters.sourceType;
    }
    return params;
  }, [filters, pagination.page, pagination.size]);

  const loadQuestions = useCallback(async (override = {}) => {
    try {
      setLoading(true);
      setError('');
      const params = buildParams(override.page, override.size);
      const data = await questionService.getQuestions(params);
      setQuestions(data.content || []);
      setPagination((prev) => ({
        ...prev,
        page: data.page ?? params.page ?? 0,
        size: data.size ?? params.size ?? prev.size,
        totalPages: data.totalPages ?? 0,
        totalElements: data.totalElements ?? 0
      }));
    } catch (err) {
      setError(err.message || 'Nu am putut încărca întrebările.');
    } finally {
      setLoading(false);
    }
  }, [buildParams]);

  const loadQuestionsRef = useRef(loadQuestions);

  useEffect(() => {
    loadQuestionsRef.current = loadQuestions;
  }, [loadQuestions]);

  useEffect(() => {
    loadQuestionsRef.current();
  }, []);

  useEffect(() => {
    const bootstrap = async () => {
      try {
        const cats = await categoryService.getCategories(true);
        setCategories(cats);
      } catch (err) {
        setError(err.message || 'Nu am putut încărca categoriile.');
      }
    };
    bootstrap();
  }, []);

  const handleFilterChange = (event) => {
    const { name, value } = event.target;
    setFilters((prev) => ({
      ...prev,
      [name]: value
    }));
  };

  const handleApplyFilters = (event) => {
    event.preventDefault();
    loadQuestions({ page: 0 });
  };

  const handlePageSizeChange = (event) => {
    const newSize = Number(event.target.value);
    loadQuestions({ page: 0, size: newSize });
  };

  const handlePageChange = (direction) => {
    const nextPage = pagination.page + direction;
    if (nextPage < 0) {
      return;
    }
    if (pagination.totalPages && nextPage >= pagination.totalPages) {
      return;
    }
    loadQuestions({ page: nextPage });
  };

  const truncate = (text, maxLength = 90) => {
    if (!text) {
      return '';
    }
    return text.length > maxLength ? `${text.slice(0, maxLength)}…` : text;
  };

  const formatDifficulty = (value) => {
    switch (value) {
      case 'EASY':
        return 'Ușor';
      case 'MEDIUM':
        return 'Mediu';
      case 'HARD':
        return 'Dificil';
      default:
        return value || '-';
    }
  };

  const startEntry = pagination.totalElements === 0 ? 0 : pagination.page * pagination.size + 1;
  const endEntry = pagination.totalElements === 0
    ? 0
    : Math.min(pagination.totalElements, pagination.page * pagination.size + questions.length);
  const currentPageDisplay = pagination.totalPages > 0 ? pagination.page + 1 : pagination.totalElements > 0 ? 1 : 0;
  const totalPagesDisplay = pagination.totalPages > 0 ? pagination.totalPages : pagination.totalElements > 0 ? 1 : 0;
  const canGoPrev = pagination.page > 0;
  const canGoNext = pagination.totalPages ? pagination.page + 1 < pagination.totalPages : false;

  const handleDeleteQuestion = async (question) => {
    const confirmed = window.confirm(`Sigur vrei să ștergi întrebarea "${truncate(question.text, 80)}"?`);
    if (!confirmed) {
      return;
    }
    setDeleteStatus({ type: '', message: '' });
    setDeletingId(question.id);
    try {
      await questionService.deleteQuestion(question.id);
      setDeleteStatus({ type: 'success', message: 'Întrebarea a fost ștearsă.' });
      await loadQuestions();
    } catch (err) {
      setDeleteStatus({ type: 'danger', message: err.message || 'Ștergerea a eșuat.' });
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div className="container py-5">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 mb-1">Întrebări</h1>
          <p className="mb-0 text-secondary">Vizualizează sau filtrează întrebările importate/creat manual</p>
        </div>
        <div className="d-flex gap-2">
          <Link to="/admin" className="btn btn-outline-secondary">Înapoi la panou</Link>
          <button className="btn btn-outline-primary" type="button" onClick={loadQuestions} disabled={loading}>
            Reîncarcă lista
          </button>
        </div>
      </div>

      <div className="card shadow-sm mb-4">
        <div className="card-body">
          <form className="row g-3" onSubmit={handleApplyFilters}>
            <div className="col-12 col-md-3">
              <label className="form-label" htmlFor="categoryId">Categorie</label>
              <select
                id="categoryId"
                name="categoryId"
                className="form-select"
                value={filters.categoryId}
                onChange={handleFilterChange}
              >
                {categoryOptions.map((category) => (
                  <option key={category.id || 'all'} value={category.id}>{category.name}</option>
                ))}
              </select>
            </div>

            <div className="col-12 col-md-3">
              <label className="form-label" htmlFor="difficulty">Dificultate</label>
              <select
                id="difficulty"
                name="difficulty"
                className="form-select"
                value={filters.difficulty}
                onChange={handleFilterChange}
              >
                {difficultyOptions.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </div>

            <div className="col-12 col-md-3">
              <label className="form-label" htmlFor="sourceType">Sursă</label>
              <select
                id="sourceType"
                name="sourceType"
                className="form-select"
                value={filters.sourceType}
                onChange={handleFilterChange}
              >
                {sourceOptions.map((option) => (
                  <option key={option.value} value={option.value}>{option.label}</option>
                ))}
              </select>
            </div>

            <div className="col-12 col-md-2">
              <label className="form-label" htmlFor="activeOnly">Stare</label>
              <select
                id="activeOnly"
                name="activeOnly"
                className="form-select"
                value={filters.activeOnly}
                onChange={handleFilterChange}
              >
                <option value="true">Doar active</option>
                <option value="false">Toate</option>
              </select>
            </div>

            <div className="col-12 col-md-1 d-flex align-items-end">
              <button className="btn btn-primary w-100" type="submit" disabled={loading}>
                Aplică
              </button>
            </div>
          </form>
        </div>
      </div>

      {(error || deleteStatus.type === 'danger') && (
        <div className="alert alert-danger" role="alert">
          {error || deleteStatus.message}
        </div>
      )}

      {deleteStatus.message && deleteStatus.type === 'success' && (
        <div className="alert alert-success" role="alert">
          {deleteStatus.message}
        </div>
      )}

      <div className="card shadow-sm">
        <div className="card-body p-0">
          {loading ? (
            <div className="p-4 text-center text-secondary">Se încarcă întrebările...</div>
          ) : questions.length === 0 ? (
            <div className="p-4 text-center text-secondary">Nu există întrebări pentru filtrele selectate.</div>
          ) : (
            <div className="table-responsive">
              <table className="table mb-0">
                <thead>
                  <tr>
                    <th className="text-nowrap">Întrebare</th>
                    <th>Categorie</th>
                    <th>Dificultate</th>
                    <th>Sursă</th>
                    <th>Active</th>
                    <th>Acțiuni</th>
                  </tr>
                </thead>
                <tbody>
                  {questions.map((question) => (
                    <tr key={question.id}>
                      <td className="text-break" style={{ maxWidth: '400px' }}>
                        {truncate(question.text)}
                      </td>
                      <td>{question.categoryName || '-'}</td>
                      <td>{formatDifficulty(question.difficultyLevel)}</td>
                      <td>
                        {question.sourceType === 'API_IMPORT' ? 'OpenTrivia' : 'Admin'}
                      </td>
                      <td>
                        {question.isActive ? (
                          <span className="badge bg-success">Da</span>
                        ) : (
                          <span className="badge bg-secondary">Nu</span>
                        )}
                      </td>
                      <td className="d-flex gap-2">
                        <Link to={`/admin/questions/${question.id}`} className="btn btn-outline-primary btn-sm">
                          Detalii
                        </Link>
                        <button
                          type="button"
                          className="btn btn-outline-danger btn-sm"
                          onClick={() => handleDeleteQuestion(question)}
                          disabled={deletingId === question.id}
                        >
                          {deletingId === question.id ? 'Se șterge...' : 'Șterge'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
        {!loading && questions.length > 0 && (
          <div className="card-footer bg-white d-flex flex-wrap justify-content-between align-items-center gap-3">
            <div className="text-secondary small">
              Afișate {startEntry}-{endEntry} din {pagination.totalElements} întrebări
            </div>
            <div className="d-flex flex-wrap gap-3 align-items-center">
              <div className="d-flex align-items-center gap-2">
                <label className="form-label mb-0" htmlFor="pageSize">Pe pagină</label>
                <select
                  id="pageSize"
                  className="form-select form-select-sm"
                  value={pagination.size}
                  onChange={handlePageSizeChange}
                >
                  {pageSizeOptions.map((option) => (
                    <option key={option} value={option}>{option}</option>
                  ))}
                </select>
              </div>
              <div className="btn-group" role="group" aria-label="Navigare paginare">
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm"
                  onClick={() => handlePageChange(-1)}
                  disabled={!canGoPrev}
                >
                  &larr;
                </button>
                <button type="button" className="btn btn-outline-secondary btn-sm" disabled>
                  Pagina {currentPageDisplay} / {Math.max(totalPagesDisplay, 1)}
                </button>
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm"
                  onClick={() => handlePageChange(1)}
                  disabled={!canGoNext}
                >
                  &rarr;
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default QuestionList;
