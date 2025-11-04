import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import AuthLayout from './AuthLayout';
import { validateEmail } from '../../utils/validators';

const Login = () => {
  const navigate = useNavigate();
  const { login } = useAuth();
  
  const [formData, setFormData] = useState({
    email: '',
    password: '',
  });
  
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value,
    }));
    if (errors[name]) {
      setErrors(prev => ({
        ...prev,
        [name]: '',
      }));
    }
  };

  const validate = () => {
    const newErrors = {};
    
    if (!formData.email) {
      newErrors.email = 'Emailul este obligatoriu';
    } else if (!validateEmail(formData.email)) {
      newErrors.email = 'Formatul emailului este invalid';
    }
    
    if (!formData.password) {
      newErrors.password = 'Parola este obligatorie';
    }
    
    return newErrors;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setApiError('');
    
    const newErrors = validate();
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    setLoading(true);
    try {
      await login(formData);
      navigate('/home'); 
    } catch (error) {
      setApiError(
        error.message || 'Credentiale invalide. Te rog încearcă din nou.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout title="Loghează-te în contul tău">
      <form onSubmit={handleSubmit} className="d-flex flex-column gap-3">
        {apiError && (
          <div className="alert alert-danger" role="alert">
            {apiError}
          </div>
        )}

        <div className="mb-3">
          <label htmlFor="email" className="form-label">Email</label>
          <input
            type="email"
            id="email"
            name="email"
            value={formData.email}
            onChange={handleChange}
            className={`form-control${errors.email ? ' is-invalid' : ''}`}
            placeholder="adresa.email@exemplu.com"
          />
          {errors.email && (
            <div className="invalid-feedback">{errors.email}</div>
          )}
        </div>

        <div className="mb-3">
          <label htmlFor="password" className="form-label">Parola</label>
          <input
            type="password"
            id="password"
            name="password"
            value={formData.password}
            onChange={handleChange}
            className={`form-control${errors.password ? ' is-invalid' : ''}`}
            placeholder="Introdu parola ta"
          />
          {errors.password && (
            <div className="invalid-feedback">{errors.password}</div>
          )}
        </div>

        <button 
          type="submit" 
          className="btn btn-primary w-100"
          disabled={loading}
        >
          {loading ? 'Logging in...' : 'Login'}
        </button>

        <div className="text-center mt-2">
          <p className="mb-0">
            Nu ai un cont?{' '}
            <Link to="/register" className="fw-semibold text-decoration-none">Înregistrează-te aici</Link>
          </p>
        </div>
      </form>
    </AuthLayout>
  );
};

export default Login;
