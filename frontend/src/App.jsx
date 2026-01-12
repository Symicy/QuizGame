import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import Login from './components/auth/Login';
import Register from './components/auth/Register';
import HomePage from './components/HomePage';
import AdminDashboard from './components/admin/AdminDashboard';
import QuestionList from './components/admin/QuestionList';
import QuestionDetails from './components/admin/QuestionDetails';
import SoloSessionStart from './components/play/SoloSessionStart';
import PlaySession from './components/play/PlaySession';
import LobbyRoom from './components/play/LobbyRoom';
import DuelRoom from './components/play/DuelRoom';
import LeaderboardPage from './components/leaderboard/LeaderboardPage';
import AppLayout from './components/layout/AppLayout';


const PrivateRoute = ({ children, requiredRoles }) => {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (requiredRoles && !requiredRoles.includes(user?.role)) {
    return <Navigate to="/home" replace />;
  }

  return <AppLayout>{children}</AppLayout>;
};

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route 
            path="/home" 
            element={
              <PrivateRoute>
                <HomePage />
              </PrivateRoute>
            } 
          />
          <Route
            path="/play/solo"
            element={
              <PrivateRoute>
                <SoloSessionStart />
              </PrivateRoute>
            }
          />
          <Route
            path="/play/lobby"
            element={
              <PrivateRoute>
                <LobbyRoom />
              </PrivateRoute>
            }
          />
          <Route
            path="/play/duel"
            element={
              <PrivateRoute>
                <DuelRoom />
              </PrivateRoute>
            }
          />
          <Route
            path="/leaderboard"
            element={
              <PrivateRoute>
                <LeaderboardPage />
              </PrivateRoute>
            }
          />
          <Route
            path="/play/solo/:sessionId"
            element={
              <PrivateRoute>
                <PlaySession />
              </PrivateRoute>
            }
          />
          <Route
            path="/admin"
            element={
              <PrivateRoute requiredRoles={['ADMIN']}>
                <AdminDashboard />
              </PrivateRoute>
            }
          />
          <Route
            path="/admin/questions"
            element={
              <PrivateRoute requiredRoles={['ADMIN']}>
                <QuestionList />
              </PrivateRoute>
            }
          />
          <Route
            path="/admin/questions/:id"
            element={
              <PrivateRoute requiredRoles={['ADMIN']}>
                <QuestionDetails />
              </PrivateRoute>
            }
          />
          <Route path="/" element={<Navigate to="/home" />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
