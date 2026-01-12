import api from './api';

const duelService = {
  createRoom: async (payload) => {
    const response = await api.post('/rooms', {
      ...payload,
      roomType: 'DUEL',
      maxPlayers: 2,
    });
    return response.data;
  },

  getRoom: async (code) => {
    const response = await api.get(`/rooms/${code}`);
    return response.data;
  },

  joinRoom: async (code, userId) => {
    const response = await api.post(`/rooms/${code}/join`, { userId });
    return response.data;
  },

  leaveRoom: async (code, userId) => {
    const response = await api.post(`/rooms/${code}/leave`, { userId });
    return response.data;
  },

  setReady: async (code, userId, ready) => {
    const response = await api.post(`/rooms/${code}/ready`, { userId, ready });
    return response.data;
  },

  startRoom: async (code, userId) => {
    const response = await api.post(`/rooms/${code}/start`, { userId });
    return response.data;
  },

  invitePlayer: async (code, inviterId, targetUserId) => {
    await api.post(`/rooms/${code}/invite`, { inviterId, targetUserId });
  },

  getState: async (code, userId) => {
    const response = await api.get(`/duels/${code}`, {
      params: { userId },
    });
    return response.data;
  },

  submitAnswer: async (code, payload) => {
    const response = await api.post(`/duels/${code}/answer`, payload);
    return response.data;
  },
};

export default duelService;
