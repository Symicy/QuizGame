import React from 'react';

const AuthLayout = ({ children, title }) => {
  return (
    <div className="min-vh-100 d-flex align-items-center justify-content-center bg-body-tertiary py-5">
      <div className="container">
        <div className="row justify-content-center">
          <div className="col-12 col-md-8 col-lg-5">
            <div className="card shadow-sm border-0 rounded-4">
              <div className="card-body p-4 p-lg-5">
                <div className="text-center mb-4">
                  <h1 className="h3 fw-bold text-primary mb-1">Quiz Online</h1>
                  <p className="fs-5 text-secondary mb-0">{title}</p>
                </div>
                {children}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AuthLayout;
