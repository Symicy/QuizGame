import api from './api';

const leaderboardService = {
  getLeaderboard: async (filters = {}) => {
    const response = await api.get('/leaderboards', {
      params: filters && (filters.categoryId || filters.quizId) ? filters : undefined,
    });
    return response.data;
  },

  getPlayerStats: async (userId) => {
    if (!userId) {
      throw new Error('User ID is required to load player stats.');
    }
    const response = await api.get(`/leaderboards/players/${userId}`);
    return response.data;
  },
};

export default leaderboardService;
