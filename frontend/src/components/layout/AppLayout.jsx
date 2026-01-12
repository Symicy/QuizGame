import React from 'react';
import QuickActionsButtons from './QuickActionsButtons';

const AppLayout = ({ children }) => (
  <div className="bg-light min-vh-100 d-flex flex-column">
    <header className="bg-white border-bottom shadow-sm">
      <div className="container py-3">
        <div className="d-flex flex-wrap justify-content-between align-items-center gap-3">
          <div>
            <p className="text-uppercase text-secondary small fw-semibold mb-0">Navigare rapidă</p>
            <h1 className="h6 mb-0">Selectează următoarea provocare</h1>
          </div>
          <QuickActionsButtons size="sm" align="end" />
        </div>
      </div>
    </header>
    <main className="flex-grow-1 py-4">
      {children}
    </main>
  </div>
);

export default AppLayout;
