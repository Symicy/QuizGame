import api from './api';

const presenceService = {
  ping: async (userId) => {
    if (!userId) {
      return null;
    }
    const response = await api.post('/presence/ping', { userId });
    return response.data;
  },

  markOffline: async (userId) => {
    if (!userId) {
      return;
    }
    await api.delete(`/presence/${userId}`);
  },

  getOnlineUsers: async (excludeUserId) => {
    const response = await api.get('/presence/online', {
      params: excludeUserId ? { excludeUserId } : undefined,
    });
    return response.data || [];
  },
};

export default presenceService;
