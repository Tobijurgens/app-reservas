import api from './api';

export const NotificacionesService = {
  obtenerMisNotificaciones: async () => {
    const response = await api.get('/notificaciones');
    return response.data;
  },

  contarNoLeidas: async () => {
    const response = await api.get('/notificaciones/no-leidas/count');
    return response.data;
  },

  marcarComoLeida: async (id) => {
    const response = await api.put(`/notificaciones/${id}/leer`, null);
    return response.data;
  },

  marcarTodasComoLeidas: async () => {
    const response = await api.put('/notificaciones/leer-todas', null);
    return response.data;
  }
};
