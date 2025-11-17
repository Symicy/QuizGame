import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const HomePage = () => {
  const navigate = useNavigate();
  const { logout, user } = useAuth();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="container py-5">
      <div className="row justify-content-center">
        <div className="col-12 col-lg-8">
          <div className="card shadow-sm border-0">
            <div className="card-body">
              <h1 className="h3 fw-bold mb-3">Home Page</h1>
              <p className="mb-2 text-secondary">Ești logat ca {user?.username || user?.email}.</p>
              {user?.role === 'ADMIN' && (
                <div className="d-flex flex-wrap gap-2">
                  <button
                    type="button"
                    className="btn btn-primary"
                    onClick={() => navigate('/admin')}
                  >
                    Deschide panoul admin
                  </button>
                </div>
              )}
            </div>
            <div className="card-footer bg-white border-0 pt-0 d-flex justify-content-end">
              <button type="button" className="btn btn-outline-danger" onClick={handleLogout}>
                Logout
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default HomePage;
