import api from './api';

export const NotificacionesService = {
  obtenerMisNotificaciones: async (email) => {
    const response = await api.get('/notificaciones', {
      headers: { 'X-User-Email': email }
    });
    return response.data;
  },

  contarNoLeidas: async (email) => {
    const response = await api.get('/notificaciones/no-leidas/count', {
      headers: { 'X-User-Email': email }
    });
    return response.data;
  },

  marcarComoLeida: async (id, email) => {
    const response = await api.put(`/notificaciones/${id}/leer`, null, {
      headers: { 'X-User-Email': email }
    });
    return response.data;
  },

  marcarTodasComoLeidas: async (email) => {
    const response = await api.put('/notificaciones/leer-todas', null, {
      headers: { 'X-User-Email': email }
    });
    return response.data;
  }
};