import React, { createContext, useState, useContext, useEffect, useCallback } from 'react';
import authService from '../services/authService';
import presenceService from '../services/presenceService';
import useUserEvents from '../hooks/useUserEvents';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [pendingInvites, setPendingInvites] = useState([]);

  useEffect(() => {
    const currentUser = authService.getCurrentUser();
    setUser(currentUser);
    setLoading(false);
  }, []);

  useEffect(() => {
    let intervalRef;
    if (user?.id) {
      const sendPing = async () => {
        try {
          await presenceService.ping(user.id);
        } catch (error) {
          // silent presence errors
        }
      };
      sendPing();
      intervalRef = setInterval(sendPing, 20000);
    }
    return () => {
      if (intervalRef) {
        clearInterval(intervalRef);
      }
      if (user?.id) {
        presenceService.markOffline(user.id).catch(() => {});
      }
    };
  }, [user?.id]);

  const handleUserEvent = useCallback((message) => {
    if (!message?.type) {
      return;
    }
    if (message.type === 'DUEL_INVITE' && message.payload) {
      setPendingInvites((prev) => {
        const alreadyExists = prev.some((invite) => invite.inviteId === message.payload.inviteId);
        if (alreadyExists) {
          return prev;
        }
        return [...prev, message.payload];
      });
    }
  }, []);

  useUserEvents({ userId: user?.id, onMessage: handleUserEvent });

  const login = async (credentials) => {
    const userData = await authService.login(credentials);
    setPendingInvites([]);
    setUser(userData);
    return userData;
  };

  const register = async (userData) => {
    const newUser = await authService.register(userData);
    setPendingInvites([]);
    setUser(newUser);
    return newUser;
  };

  const logout = () => {
    if (user?.id) {
      presenceService.markOffline(user.id).catch(() => {});
    }
    authService.logout();
    setPendingInvites([]);
    setUser(null);
  };

  const dismissInvite = (inviteId) => {
    setPendingInvites((prev) => prev.filter((invite) => invite.inviteId !== inviteId));
  };

  const value = {
    user,
    login,
    register,
    logout,
    isAuthenticated: !!user,
    pendingInvites,
    dismissInvite,
  };

  return (
    <AuthContext.Provider value={value}>
      {!loading && children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
};
