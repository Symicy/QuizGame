import api from './api';

const lobbyService = {
  getState: async (userId) => {
    const response = await api.get('/lobby', {
      params: userId ? { userId } : undefined,
    });
    return response.data;
  },

  join: async (userId) => {
    const response = await api.post('/lobby/join', { userId });
    return response.data;
  },

  leave: async (userId) => {
    const response = await api.post('/lobby/leave', { userId });
    return response.data;
  },

  submitAnswer: async ({ userId, questionId, answerIndex }) => {
    const response = await api.post('/lobby/answer', {
      userId,
      questionId,
      answerIndex,
    });
    return response.data;
  },
};

export default lobbyService;
