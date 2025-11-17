import api from './api';

const categoryService = {
  getCategories: async (activeOnly = true) => {
    const response = await api.get('/categories', {
      params: { activeOnly }
    });
    return response.data;
  },
  syncFromOpenTrivia: async () => {
    const response = await api.post('/categories/opentdb/sync');
    return response.data;
  }
};

export default categoryService;
