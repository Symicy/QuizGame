import React from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const ACTIONS = [
  {
    key: 'home',
    label: 'Home',
    path: '/home',
    variant: 'btn-outline-secondary',
    match: (pathname) => pathname === '/home',
  },
  {
    key: 'solo',
    label: 'Joacă single-player',
    path: '/play/solo',
    variant: 'btn-success',
    match: (pathname) => pathname.startsWith('/play/solo'),
  },
  {
    key: 'lobby',
    label: 'Intră în lobby public',
    path: '/play/lobby',
    variant: 'btn-warning',
    match: (pathname) => pathname.startsWith('/play/lobby'),
  },
  {
    key: 'duel',
    label: 'Creează duel 1v1',
    path: '/play/duel',
    variant: 'btn-info text-white',
    match: (pathname) => pathname.startsWith('/play/duel'),
  },
  {
    key: 'leaderboard',
    label: 'Vezi clasamentul global',
    path: '/leaderboard',
    variant: 'btn-outline-primary',
    match: (pathname) => pathname.startsWith('/leaderboard'),
  },
  {
    key: 'admin',
    label: 'Panou admin',
    path: '/admin',
    variant: 'btn-outline-dark',
    match: (pathname) => pathname.startsWith('/admin'),
    roles: ['ADMIN'],
  },
];

const QuickActionsButtons = ({ size = 'md', align = 'start', className = '' }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { user } = useAuth();

  const visibleActions = ACTIONS.filter((action) => {
    if (!action.roles) {
      return true;
    }
    return action.roles.includes(user?.role);
  });

  const wrapperClasses = ['d-flex', 'flex-wrap', 'gap-2'];
  if (align === 'end') {
    wrapperClasses.push('justify-content-end');
  }
  if (className) {
    wrapperClasses.push(className);
  }

  const baseBtnClass = size === 'sm' ? 'btn btn-sm' : 'btn';

  return (
    <div className={wrapperClasses.join(' ')}>
      {visibleActions.map((action) => {
        const isActive = action.match ? action.match(location.pathname) : location.pathname === action.path;
        return (
          <button
            key={action.key}
            type="button"
            className={`${baseBtnClass} ${action.variant} ${isActive ? 'active' : ''}`.trim()}
            onClick={() => navigate(action.path)}
          >
            {action.label}
          </button>
        );
      })}
    </div>
  );
};

export default QuickActionsButtons;
