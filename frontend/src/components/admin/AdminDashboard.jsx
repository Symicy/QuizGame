import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import categoryService from '../../services/categoryService';
import questionService from '../../services/questionService';
import { useAuth } from '../../context/AuthContext';

const difficultyOptions = [
  { label: 'Ușor', value: 'EASY' },
  { label: 'Mediu', value: 'MEDIUM' },
  { label: 'Dificil', value: 'HARD' }
];

const encodingOptions = [
  { label: 'HTML (implicit)', value: 'DEFAULT' },
  { label: 'URL encoded (RFC3986)', value: 'URL_3986' },
  { label: 'URL encoded (legacy)', value: 'URL_LEGACY' },
  { label: 'Base64', value: 'BASE64' }
];

const AdminDashboard = () => {
  const { user } = useAuth();
  const [categories, setCategories] = useState([]);
  const [triviaCategories, setTriviaCategories] = useState([]);
  const [loadingData, setLoadingData] = useState(true);
  const [manualStatus, setManualStatus] = useState({ type: '', message: '' });
  const [importStatus, setImportStatus] = useState({ type: '', message: '' });
  const [manualSubmitting, setManualSubmitting] = useState(false);
  const [importSubmitting, setImportSubmitting] = useState(false);
  const [tokenActionPending, setTokenActionPending] = useState(false);
  const [persistedToken, setPersistedToken] = useState('');

  const resolveInternalIdForExternal = useCallback((externalId) => {
    if (!externalId) {
      return '';
    }
    const external = triviaCategories.find((category) => String(category.id) === String(externalId));
    if (!external) {
      return '';
    }
    const match = categories.find((category) => category.name === external.name);
    return match ? String(match.id) : '';
  }, [categories, triviaCategories]);

  const manualInitialState = useMemo(() => ({
    text: '',
    optionA: '',
    optionB: '',
    optionC: '',
    optionD: '',
    correctOption: '0',
    difficultyLevel: 'EASY',
    explanation: '',
    points: '',
    categoryId: categories[0]?.id ? String(categories[0].id) : '',
    isActive: true
  }), [categories]);

  const importInitialState = useMemo(() => {
    const defaultExternalId = triviaCategories[0]?.id ? String(triviaCategories[0].id) : '';
    const inferredCategoryId = resolveInternalIdForExternal(defaultExternalId)
      || (categories[0]?.id ? String(categories[0].id) : '');
    return {
      categoryId: inferredCategoryId,
      externalCategoryId: defaultExternalId,
      difficultyLevel: 'ANY',
      amount: 10,
      encoding: 'DEFAULT',
      sessionToken: persistedToken
    };
  }, [categories, triviaCategories, resolveInternalIdForExternal, persistedToken]);

  const [manualForm, setManualForm] = useState(manualInitialState);
  const [importForm, setImportForm] = useState(importInitialState);

  useEffect(() => {
    setManualForm(manualInitialState);
    setImportForm(importInitialState);
  }, [manualInitialState, importInitialState]);

  const loadData = useCallback(async () => {
    try {
      setLoadingData(true);
      const [syncedCategories, triviaCats, storedToken] = await Promise.all([
        categoryService.syncFromOpenTrivia(),
        questionService.getOpenTriviaCategories(),
        questionService.getStoredOpenTriviaToken()
      ]);
      setCategories(syncedCategories);
      setTriviaCategories(triviaCats);
      setPersistedToken(storedToken || '');
    } catch (error) {
      const message = error.message || 'Nu am putut sincroniza categoriile.';
      setImportStatus({ type: 'danger', message });
    } finally {
      setLoadingData(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleManualChange = (event) => {
    const { name, value, type, checked } = event.target;
    setManualForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
  };

  const handleImportChange = (event) => {
    const { name, value } = event.target;
    setImportForm((prev) => {
      const updated = {
        ...prev,
        [name]: value
      };

      if (name === 'externalCategoryId') {
        const inferred = resolveInternalIdForExternal(value);
        if (inferred) {
          updated.categoryId = inferred;
        }
      }

      return updated;
    });
  };

  const resetManualStatus = () => setManualStatus({ type: '', message: '' });
  const resetImportStatus = () => setImportStatus({ type: '', message: '' });

  const handleManualSubmit = async (event) => {
    event.preventDefault();
    resetManualStatus();
    setManualSubmitting(true);

    try {
      const payload = {
        ...manualForm,
        correctOption: Number(manualForm.correctOption),
        categoryId: manualForm.categoryId ? Number(manualForm.categoryId) : null,
        points: manualForm.points ? Number(manualForm.points) : undefined,
        sourceType: 'ADMIN'
      };

      if (!payload.categoryId) {
        throw new Error('Selectează o categorie înainte să salvezi întrebarea.');
      }

      await questionService.createQuestion(payload);
      setManualStatus({ type: 'success', message: 'Întrebarea a fost salvată.' });
      setManualForm(manualInitialState);
    } catch (error) {
      setManualStatus({ type: 'danger', message: error.message || 'Salvarea întrebării a eșuat.' });
    } finally {
      setManualSubmitting(false);
    }
  };

  const handleImportSubmit = async (event) => {
    event.preventDefault();
    resetImportStatus();
    setImportSubmitting(true);

    try {
      const payload = {
        categoryId: importForm.categoryId ? Number(importForm.categoryId) : null,
        amount: Number(importForm.amount),
        externalCategoryId: importForm.externalCategoryId ? Number(importForm.externalCategoryId) : undefined,
        difficultyLevel: importForm.difficultyLevel !== 'ANY' ? importForm.difficultyLevel : null,
        encoding: importForm.encoding,
        sessionToken: importForm.sessionToken?.trim() || null
      };

      if (!payload.categoryId && !payload.externalCategoryId) {
        throw new Error('Selectează cel puțin o categorie pentru import.');
      }

      const imported = await questionService.importFromOpenTrivia(payload);
      setImportStatus({
        type: 'success',
        message: `Au fost importate ${imported.length} întrebări.`
      });
    } catch (error) {
      setImportStatus({ type: 'danger', message: error.message || 'Importul a eșuat.' });
    } finally {
      setImportSubmitting(false);
    }
  };

  const handleTokenRequest = async () => {
    resetImportStatus();
    setTokenActionPending(true);
    try {
      const token = await questionService.requestOpenTriviaToken();
      setPersistedToken(token || '');
      setImportForm((prev) => ({ ...prev, sessionToken: token }));
      setImportStatus({ type: 'success', message: 'Token OpenTrivia generat. Se va folosi la următorul import.' });
    } catch (error) {
      setImportStatus({ type: 'danger', message: error.message || 'Nu am putut genera tokenul.' });
    } finally {
      setTokenActionPending(false);
    }
  };

  const handleTokenReset = async () => {
    if (!importForm.sessionToken) {
      setImportStatus({ type: 'danger', message: 'Completează tokenul pe care vrei să îl resetezi.' });
      return;
    }
    resetImportStatus();
    setTokenActionPending(true);
    try {
      const token = await questionService.resetOpenTriviaToken(importForm.sessionToken.trim());
      setImportStatus({ type: 'success', message: 'Tokenul a fost resetat. Nu vei primi întrebări duplicate.' });
      setPersistedToken(token || '');
      setImportForm((prev) => ({ ...prev, sessionToken: token }));
    } catch (error) {
      setImportStatus({ type: 'danger', message: error.message || 'Resetarea tokenului a eșuat.' });
    } finally {
      setTokenActionPending(false);
    }
  };

  if (loadingData) {
    return (
      <div className="container py-5">
        <div className="alert alert-info">Se încarcă datele necesare...</div>
      </div>
    );
  }

  return (
    <div className="container py-5">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h1 className="h3 mb-1">Panou Admin</h1>
          <p className="mb-0 text-secondary">Bine ai venit, {user?.username || user?.email}</p>
        </div>
        <div className="d-flex gap-2">
          <Link to="/admin/questions" className="btn btn-outline-primary">Vezi întrebări</Link>
          <Link to="/home" className="btn btn-outline-secondary">&larr; Înapoi la Home</Link>
        </div>
      </div>

      <div className="row g-4">
        <div className="col-12 col-lg-6">
          <div className="card shadow-sm h-100">
            <div className="card-body">
              <h2 className="h5 mb-3">Adaugă întrebare manual</h2>
              {manualStatus.message && (
                <div className={`alert alert-${manualStatus.type}`} role="alert">
                  {manualStatus.message}
                </div>
              )}
              <form className="d-flex flex-column gap-3" onSubmit={handleManualSubmit}>
                <div>
                  <label className="form-label" htmlFor="text">Întrebare</label>
                  <textarea
                    id="text"
                    name="text"
                    className="form-control"
                    rows={3}
                    value={manualForm.text}
                    onChange={handleManualChange}
                    required
                  />
                </div>

                {['A', 'B', 'C', 'D'].map((label, index) => (
                  <div key={label}>
                    <label className="form-label" htmlFor={`option${label}`}>
                      Varianta {label}
                    </label>
                    <input
                      id={`option${label}`}
                      name={`option${label}`}
                      type="text"
                      className="form-control"
                      value={manualForm[`option${label}`]}
                      onChange={handleManualChange}
                      required
                    />
                  </div>
                ))}

                <div className="row g-3">
                  <div className="col-12 col-md-6">
                    <label className="form-label" htmlFor="correctOption">Răspuns corect</label>
                    <select
                      id="correctOption"
                      name="correctOption"
                      className="form-select"
                      value={manualForm.correctOption}
                      onChange={handleManualChange}
                    >
                      <option value="0">Varianta A</option>
                      <option value="1">Varianta B</option>
                      <option value="2">Varianta C</option>
                      <option value="3">Varianta D</option>
                    </select>
                  </div>
                  <div className="col-12 col-md-6">
                    <label className="form-label" htmlFor="difficultyLevel">Dificultate</label>
                    <select
                      id="difficultyLevel"
                      name="difficultyLevel"
                      className="form-select"
                      value={manualForm.difficultyLevel}
                      onChange={handleManualChange}
                    >
                      {difficultyOptions.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="row g-3">
                  <div className="col-12 col-md-6">
                    <label className="form-label" htmlFor="categoryId">Categorie internă</label>
                    <select
                      id="categoryId"
                      name="categoryId"
                      className="form-select"
                      value={manualForm.categoryId}
                      onChange={handleManualChange}
                      required
                    >
                      <option value="">Alege categoria</option>
                      {categories.map((category) => (
                        <option key={category.id} value={category.id}>
                          {category.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="col-12 col-md-6">
                    <label className="form-label" htmlFor="points">Puncte (opțional)</label>
                    <input
                      id="points"
                      name="points"
                      type="number"
                      className="form-control"
                      value={manualForm.points}
                      onChange={handleManualChange}
                      min="0"
                    />
                  </div>
                </div>

                <div>
                  <label className="form-label" htmlFor="explanation">Explicație (opțional)</label>
                  <textarea
                    id="explanation"
                    name="explanation"
                    className="form-control"
                    rows={2}
                    value={manualForm.explanation}
                    onChange={handleManualChange}
                  />
                </div>

                <div className="form-check form-switch">
                  <input
                    className="form-check-input"
                    type="checkbox"
                    id="isActive"
                    name="isActive"
                    checked={manualForm.isActive}
                    onChange={handleManualChange}
                  />
                  <label className="form-check-label" htmlFor="isActive">
                    Activează imediat întrebarea
                  </label>
                </div>

                <button className="btn btn-primary" type="submit" disabled={manualSubmitting}>
                  {manualSubmitting ? 'Se salvează...' : 'Salvează întrebarea'}
                </button>
              </form>
            </div>
          </div>
        </div>

        <div className="col-12 col-lg-6">
          <div className="card shadow-sm h-100">
            <div className="card-body">
              <div className="d-flex justify-content-between align-items-center mb-3">
                <h2 className="h5 mb-0">Import OpenTrivia</h2>
                <button
                  type="button"
                  className="btn btn-outline-secondary btn-sm"
                  onClick={loadData}
                  disabled={loadingData}
                >
                  Resincronizează categoriile
                </button>
              </div>
              {importStatus.message && (
                <div className={`alert alert-${importStatus.type}`} role="alert">
                  {importStatus.message}
                </div>
              )}
              <form className="d-flex flex-column gap-3" onSubmit={handleImportSubmit}>
                <div className="row g-3">
                  <div className="col-12 col-md-6">
                    <label className="form-label" htmlFor="importCategoryId">Categorie internă</label>
                    <select
                      id="importCategoryId"
                      name="categoryId"
                      className="form-select"
                      value={importForm.categoryId}
                      onChange={handleImportChange}
                      required
                    >
                      <option value="">Alege categoria</option>
                      {categories.map((category) => (
                        <option key={category.id} value={category.id}>
                          {category.name}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="col-12 col-md-6">
                    <label className="form-label" htmlFor="externalCategoryId">Categorie OpenTrivia</label>
                    <select
                      id="externalCategoryId"
                      name="externalCategoryId"
                      className="form-select"
                      value={importForm.externalCategoryId}
                      onChange={handleImportChange}
                    >
                      <option value="">Oricare categorie</option>
                      {triviaCategories.map((category) => (
                        <option key={category.id} value={category.id}>
                          {category.name}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="row g-3">
                  <div className="col-12 col-md-6">
                    <label className="form-label" htmlFor="difficulty">Dificultate OpenTrivia</label>
                    <select
                      id="difficulty"
                      name="difficultyLevel"
                      className="form-select"
                      value={importForm.difficultyLevel}
                      onChange={handleImportChange}
                    >
                      <option value="ANY">Orice dificultate</option>
                      {difficultyOptions.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="col-12 col-md-6">
                    <label className="form-label" htmlFor="amount">Număr întrebări</label>
                    <input
                      id="amount"
                      name="amount"
                      type="number"
                      className="form-control"
                      value={importForm.amount}
                      onChange={handleImportChange}
                      min="1"
                      max="50"
                      required
                    />
                    <div className="form-text">OpenTrivia permite maximum 50 de întrebări per cerere.</div>
                  </div>
                </div>

                <div className="row g-3">
                  <div className="col-12 col-md-6">
                    <label className="form-label" htmlFor="encoding">Codare răspunsuri</label>
                    <select
                      id="encoding"
                      name="encoding"
                      className="form-select"
                      value={importForm.encoding}
                      onChange={handleImportChange}
                    >
                      {encodingOptions.map((option) => (
                        <option key={option.value} value={option.value}>
                          {option.label}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="col-12 col-md-6">
                    <label className="form-label" htmlFor="sessionToken">Token sesiune (opțional)</label>
                    <div className="input-group">
                      <input
                        id="sessionToken"
                        name="sessionToken"
                        type="text"
                        className="form-control"
                        value={importForm.sessionToken}
                        onChange={handleImportChange}
                        placeholder="token OpenTrivia"
                      />
                      <button
                        type="button"
                        className="btn btn-outline-primary"
                        onClick={handleTokenRequest}
                        disabled={tokenActionPending}
                      >
                        Generează
                      </button>
                      <button
                        type="button"
                        className="btn btn-outline-warning"
                        onClick={handleTokenReset}
                        disabled={tokenActionPending || !importForm.sessionToken}
                      >
                        Resetează
                      </button>
                    </div>
                    <div className="form-text">
                      Folosește tokenuri pentru a evita întrebările duplicate; resetează tokenul după ce le epuizezi.
                    </div>
                  </div>
                </div>

                <button className="btn btn-success" type="submit" disabled={importSubmitting}>
                  {importSubmitting ? 'Se importă...' : 'Importă întrebări'}
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminDashboard;
