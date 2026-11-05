// src/services/ServiciosService.js
import api from './api';

export const ServiciosService = {
  obtenerTodos: async () => {
    const response = await api.get('/servicios');
    return response.data;
  },

  obtenerPorId: async (id) => {
    const response = await api.get(`/servicios/${id}`);
    return response.data;
  },

  obtenerPorRecurso: async (recursoId) => {
    const response = await api.get(`/servicios/recurso/${recursoId}`);
    return response.data;
  },

  crearServicio: async (servicioDTO) => {
    const response = await api.post('/servicios', servicioDTO);
    return response.data;
  },

  actualizarServicio: async (id, servicioDTO) => {
    const response = await api.put(`/servicios/${id}`, servicioDTO);
    return response.data;
  },

  eliminarServicio: async (id) => {
    const response = await api.delete(`/servicios/${id}`);
    return response.data;
  }
};