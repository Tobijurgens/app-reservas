// src/services/NegociosService.js
import api from './api';

export const NegociosService = {
  obtenerTodos: async () => {
    const response = await api.get('/negocios');
    return response.data;
  },

  obtenerMisNegocios: async () => {
    const response = await api.get('/negocios/mis-negocios');
    return response.data;
  },

  obtenerPorId: async (id) => {
    const response = await api.get(`/negocios/${id}`);
    return response.data;
  },

  obtenerPorAlias: async (alias) => {
    const response = await api.get(`/negocios/publico/${alias}`);
    return response.data;
  },

  crearNegocio: async (negocioDTO) => {
    const response = await api.post('/negocios', negocioDTO);
    return response.data;
  },

  actualizarNegocio: async (id, negocioDTO) => {
    const response = await api.put(`/negocios/${id}`, negocioDTO);
    return response.data;
  },

  eliminarNegocio: async (id) => {
    const response = await api.delete(`/negocios/${id}`);
    return response.data;
  }
};