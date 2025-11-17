import api from './api';

const questionService = {
  getQuestions: async (filters = {}) => {
    const response = await api.get('/questions/search', {
      params: filters,
    });
    return response.data;
  },
  createQuestion: async (payload) => {
    const response = await api.post('/questions', payload);
    return response.data;
  },
  updateQuestion: async (id, payload) => {
    const response = await api.put(`/questions/${id}`, payload);
    return response.data;
  },
  importFromOpenTrivia: async (payload) => {
    const response = await api.post('/questions/import/opentdb', payload);
    return response.data;
  },
  getOpenTriviaCategories: async () => {
    const response = await api.get('/questions/opentdb/categories');
    return response.data;
  },
  getQuestion: async (id) => {
    const response = await api.get(`/questions/${id}`);
    return response.data;
  },
  deleteQuestion: async (id) => {
    await api.delete(`/questions/${id}`);
  },
  requestOpenTriviaToken: async () => {
    const response = await api.post('/questions/opentdb/token/request');
    return response.data;
  },
  resetOpenTriviaToken: async (token) => {
    const response = await api.post('/questions/opentdb/token/reset', { token });
    return response.data;
  },
  getStoredOpenTriviaToken: async () => {
    try {
      const response = await api.get('/questions/opentdb/token');
      return response.data;
    } catch (error) {
      if (error.response?.status === 204 || error.response?.status === 404) {
        return '';
      }
      throw error;
    }
  }
};

export default questionService;
