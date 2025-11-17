import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import categoryService from '../../services/categoryService';
import questionService from '../../services/questionService';

const difficultyOptions = [
  { label: 'Oricare', value: '' },
  { label: 'Ușor', value: 'EASY' },
  { label: 'Mediu', value: 'MEDIUM' },
  { label: 'Dificil', value: 'HARD' }
];

const sourceOptions = [
  { label: 'Oricare', value: '' },
  { label: 'Admin', value: 'ADMIN' },
  { label: 'OpenTrivia', value: 'API_IMPORT' }
];

const pageSizeOptions = [6, 12, 24];

const buildDefaultFilters = () => ({
  categoryId: '',
  difficulty: '',
  sourceType: '',
  activeOnly: true
});

const QuestionExplorer = () => {
  const [categories, setCategories] = useState([]);
  const [questions, setQuestions] = useState([]);
  const [filters, setFilters] = useState(buildDefaultFilters);
  const [appliedFilters, setAppliedFilters] = useState(buildDefaultFilters);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [pagination, setPagination] = useState({
    page: 0,
    size: pageSizeOptions[0],
    totalPages: 0,
    totalElements: 0
  });

  const categoryOptions = useMemo(() => [
    { id: '', name: 'Toate categoriile' },
    ...categories
  ], [categories]);

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

  const truncate = (text, maxLength = 160) => {
    if (!text) {
      return '';
    }
    return text.length > maxLength ? `${text.slice(0, maxLength)}…` : text;
  };

  const loadCategories = useCallback(async () => {
    try {
      const data = await categoryService.getCategories(true);
      setCategories(data);
    } catch (err) {
      setError(err.message || 'Nu am putut încărca categoriile.');
    }
  }, []);

  const loadQuestions = useCallback(async (override = {}) => {
    try {
      setLoading(true);
      setError('');
      const targetPage = typeof override.page === 'number' ? override.page : pagination.page;
      const targetSize = typeof override.size === 'number' ? override.size : pagination.size;
      const filterSet = override.filters || appliedFilters;
      const params = { activeOnly: filterSet.activeOnly, page: targetPage, size: targetSize };
      if (filterSet.categoryId) {
        params.categoryId = Number(filterSet.categoryId);
      }
      if (filterSet.difficulty) {
        params.difficulty = filterSet.difficulty;
      }
      if (filterSet.sourceType) {
        params.sourceType = filterSet.sourceType;
      }
      const data = await questionService.getQuestions(params);
      setQuestions(data.content || []);
      setPagination((prev) => ({
        ...prev,
        page: data.page ?? targetPage,
        size: data.size ?? targetSize,
        totalPages: data.totalPages ?? 0,
        totalElements: data.totalElements ?? 0
      }));
    } catch (err) {
      setError(err.message || 'Nu am putut încărca întrebările.');
    } finally {
      setLoading(false);
    }
  }, [appliedFilters, pagination.page, pagination.size]);

  const loadQuestionsRef = useRef(loadQuestions);

  useEffect(() => {
    loadQuestionsRef.current = loadQuestions;
  }, [loadQuestions]);

  useEffect(() => {
    loadQuestionsRef.current();
  }, []);

  useEffect(() => {
    loadCategories();
  }, [loadCategories]);

  const handleFilterChange = (event) => {
    const { name, value, type, checked } = event.target;
    setFilters((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    const nextFilters = { ...filters };
    setPagination((prev) => ({ ...prev, page: 0 }));
    setAppliedFilters(nextFilters);
    loadQuestions({ filters: nextFilters, page: 0 });
  };

  const handleReset = () => {
    const reset = buildDefaultFilters();
    setFilters(reset);
    setAppliedFilters(reset);
    setPagination((prev) => ({ ...prev, page: 0, size: pageSizeOptions[0] }));
    loadQuestions({ filters: reset, page: 0, size: pageSizeOptions[0] });
  };

  const handlePageSizeChange = (event) => {
    const newSize = Number(event.target.value);
    setPagination((prev) => ({ ...prev, size: newSize, page: 0 }));
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

  const startEntry = pagination.totalElements === 0 ? 0 : pagination.page * pagination.size + 1;
  const endEntry = pagination.totalElements === 0
    ? 0
    : Math.min(pagination.totalElements, pagination.page * pagination.size + questions.length);
  const currentPageDisplay = pagination.totalPages > 0 ? pagination.page + 1 : pagination.totalElements > 0 ? 1 : 0;
  const totalPagesDisplay = pagination.totalPages > 0 ? pagination.totalPages : pagination.totalElements > 0 ? 1 : 0;
  const canGoPrev = pagination.page > 0;
  const canGoNext = pagination.totalPages ? pagination.page + 1 < pagination.totalPages : false;

  return (
    <div className="container py-5">
      <div className="d-flex justify-content-between flex-wrap gap-3 align-items-center mb-4">
        <div>
          <h1 className="h3 mb-1">Descoperă întrebări</h1>
          <p className="mb-0 text-secondary">Filtrează baza de întrebări după categorie, dificultate și sursă.</p>
        </div>
        <div className="d-flex gap-2">
          <Link to="/home" className="btn btn-outline-secondary">Înapoi la home</Link>
          <button type="button" className="btn btn-outline-primary" onClick={loadQuestions} disabled={loading}>
            Reîncarcă lista
          </button>
        </div>
      </div>

      <div className="card shadow-sm mb-4">
        <div className="card-body">
          <form className="row g-3 align-items-end" onSubmit={handleSubmit}>
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
              <div className="form-check mt-4">
                <input
                  id="activeOnly"
                  name="activeOnly"
                  className="form-check-input"
                  type="checkbox"
                  checked={filters.activeOnly}
                  onChange={handleFilterChange}
                />
                <label className="form-check-label" htmlFor="activeOnly">
                  Doar active
                </label>
              </div>
            </div>

            <div className="col-12 col-md-1 d-flex gap-2">
              <button className="btn btn-primary flex-grow-1" type="submit" disabled={loading}>
                Aplică
              </button>
              <button className="btn btn-outline-secondary flex-grow-1" type="button" onClick={handleReset} disabled={loading}>
                Reset
              </button>
            </div>
          </form>
        </div>
      </div>

      {error && (
        <div className="alert alert-danger mb-4" role="alert">
          {error}
        </div>
      )}

      {loading ? (
        <div className="card shadow-sm">
          <div className="card-body text-center text-secondary">
            Se încarcă întrebările...
          </div>
        </div>
      ) : questions.length === 0 ? (
        <div className="card shadow-sm">
          <div className="card-body text-center text-secondary">
            Nu am găsit întrebări pentru filtrele selectate.
          </div>
        </div>
      ) : (
        <>
          <div className="row row-cols-1 row-cols-lg-2 g-4">
            {questions.map((question) => (
              <div className="col" key={question.id}>
                <div className="card h-100 shadow-sm">
                  <div className="card-body d-flex flex-column">
                    <p className="text-secondary small mb-1">{question.categoryName || 'Fără categorie'}</p>
                    <h2 className="h5 mb-3">{truncate(question.text)}</h2>
                    <div className="d-flex gap-2 mb-3 flex-wrap">
                      <span className="badge bg-light text-dark border">{formatDifficulty(question.difficultyLevel)}</span>
                      <span className={`badge ${question.isActive ? 'bg-success' : 'bg-secondary'}`}>
                        {question.isActive ? 'Activă' : 'Inactivă'}
                      </span>
                      <span className="badge bg-primary text-white">
                        {formatSource(question.sourceType)}
                      </span>
                    </div>
                    <div className="mt-auto d-flex justify-content-between align-items-center">
                      <span className="text-secondary small">{question.points ?? 0} puncte</span>
                      <Link to={`/questions/${question.id}`} className="btn btn-outline-primary btn-sm">
                        Vezi detalii
                      </Link>
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
          <div className="d-flex flex-wrap justify-content-between align-items-center gap-3 mt-4">
            <div className="text-secondary small">
              Afișate {startEntry}-{endEntry} din {pagination.totalElements} întrebări
            </div>
            <div className="d-flex flex-wrap gap-3 align-items-center">
              <div className="d-flex align-items-center gap-2">
                <label className="form-label mb-0" htmlFor="explorerPageSize">Pe pagină</label>
                <select
                  id="explorerPageSize"
                  className="form-select form-select-sm"
                  value={pagination.size}
                  onChange={handlePageSizeChange}
                >
                  {pageSizeOptions.map((option) => (
                    <option key={option} value={option}>{option}</option>
                  ))}
                </select>
              </div>
              <div className="btn-group" role="group" aria-label="Paginare întrebări">
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
        </>
      )}
    </div>
  );
};

export default QuestionExplorer;
