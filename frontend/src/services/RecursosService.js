// src/services/RecursosService.js
import api from './api';

export const RecursosService = {
  obtenerTodos: async () => {
    const response = await api.get('/recursos');
    return response.data;
  },

  obtenerPorId: async (id) => {
    const response = await api.get(`/recursos/${id}`);
    return response.data;
  },

  obtenerPorNegocio: async (negocioId) => {
    const response = await api.get(`/recursos/negocio/${negocioId}`);
    return response.data;
  },

  obtenerTodosPorNegocio: async (negocioId) => {
    const response = await api.get(`/recursos/negocio/${negocioId}/todos`);
    return response.data;
  },

  obtenerMisAsignaciones: async () => {
    const response = await api.get('/recursos/mis-asignaciones');
    return response.data;
  },

  crearRecurso: async (recursoDTO) => {
    const response = await api.post('/recursos', recursoDTO);
    return response.data;
  },

  actualizarRecurso: async (id, recursoDTO) => {
    const response = await api.put(`/recursos/${id}`, recursoDTO);
    return response.data;
  },

  eliminarRecurso: async (id) => {
    const response = await api.delete(`/recursos/${id}`);
    return response.data;
  },

  aceptarInvitacion: async (token) => {
    const response = await api.get('/recursos/invitacion/aceptar', { params: { token } });
    return response.data;
  },

  rechazarInvitacion: async (token) => {
    const response = await api.get('/recursos/invitacion/rechazar', { params: { token } });
    return response.data;
  },

  procesarInvitacion: async (accion, token) => {
    const response = await api.get(`/recursos/invitacion/${accion}`, { params: { token } });
    return response.data;
  }
};