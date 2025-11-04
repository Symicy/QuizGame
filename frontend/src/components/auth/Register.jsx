import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import AuthLayout from './AuthLayout';
import { validateEmail, validatePassword, validateUsername } from '../../utils/validators';

const Register = () => {
  const navigate = useNavigate();
  const { register } = useAuth();
  
  const [formData, setFormData] = useState({
    email: '',
    username: '',
    password: '',
    confirmPassword: '',
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
    
    if (!formData.username) {
      newErrors.username = 'Username-ul este obligatoriu';
    } else if (!validateUsername(formData.username)) {
      newErrors.username = 'Username-ul trebuie să aibă între 3 și 20 de caractere';
    }
    
    if (!formData.password) {
      newErrors.password = 'Parola este obligatorie';
    } else if (!validatePassword(formData.password)) {
      newErrors.password = 'Parola trebuie să aibă cel puțin 6 caractere';
    }
    
    if (formData.password !== formData.confirmPassword) {
      newErrors.confirmPassword = 'Parolele nu se potrivesc';
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
      const { confirmPassword, ...registerData } = formData;
      await register(registerData);
      navigate('/home');
    } catch (error) {
      setApiError(
        error.message || 'Înregistrarea a eșuat. Te rugăm să încerci din nou.'
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <AuthLayout title="Creează-ți contul">
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
            placeholder="your.email@example.com"
          />
          {errors.email && (
            <div className="invalid-feedback">{errors.email}</div>
          )}
        </div>

        <div className="mb-3">
          <label htmlFor="username" className="form-label">Username</label>
          <input
            type="text"
            id="username"
            name="username"
            value={formData.username}
            onChange={handleChange}
            className={`form-control${errors.username ? ' is-invalid' : ''}`}
            placeholder="Alege un username"
          />
          {errors.username && (
            <div className="invalid-feedback">{errors.username}</div>
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
            placeholder="Creează o parolă"
          />
          {errors.password && (
            <div className="invalid-feedback">{errors.password}</div>
          )}
        </div>

        <div className="mb-3">
          <label htmlFor="confirmPassword" className="form-label">Confirmă parola</label>
          <input
            type="password"
            id="confirmPassword"
            name="confirmPassword"
            value={formData.confirmPassword}
            onChange={handleChange}
            className={`form-control${errors.confirmPassword ? ' is-invalid' : ''}`}
            placeholder="Confirmă parola"
          />
          {errors.confirmPassword && (
            <div className="invalid-feedback">{errors.confirmPassword}</div>
          )}
        </div>

        <button 
          type="submit" 
          className="btn btn-primary w-100"
          disabled={loading}
        >
          {loading ? 'Crearea contului...' : 'Înregistrează-te'}
        </button>

        <div className="text-center mt-2">
          <p className="mb-0">
            Ai deja un cont?{' '}
            <Link to="/login" className="fw-semibold text-decoration-none">Conectează-te aici</Link>
          </p>
        </div>
      </form>
    </AuthLayout>
  );
};

export default Register;
