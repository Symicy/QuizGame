import api from './api';

const sessionService = {
  startSoloSession: async (payload) => {
    const response = await api.post('/sessions/solo', payload);
    return response.data;
  },

  getSession: async (sessionId) => {
    const response = await api.get(`/sessions/${sessionId}`);
    return response.data;
  },

  submitAnswer: async (sessionId, payload) => {
    const response = await api.post(`/sessions/${sessionId}/submit`, payload);
    return response.data;
  },

  completeSession: async (sessionId) => {
    const response = await api.post(`/sessions/${sessionId}/complete`);
    return response.data;
  },

  getRecentSessions: async (userId) => {
    const response = await api.get(`/sessions/users/${userId}/recent`);
    return response.data;
  },
};

export default sessionService;
