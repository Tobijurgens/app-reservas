// src/services/TurnosService.js
import api from './api';

/**
 * @typedef {Object} TurnoDTO
 * @property {number} idServicio
 * @property {number} [idRecurso] - Se valida contra el recurso del servicio.
 * @property {string} fechaHoraInicio - Formato ISO date-time
 */

export const TurnosService = {
  
  /**
   * Crea un nuevo turno.
   * @param {TurnoDTO} turnoDTO 
   */
  crearTurno: async (turnoDTO) => {
    const response = await api.post('/turnos', turnoDTO);
    return response.data;
  },

  /**
   * Obtiene todos los turnos del usuario autenticado (Cliente).
   */
  obtenerMisTurnos: async () => {
    const response = await api.get('/turnos/mis-turnos');
    return response.data;
  },

  /**
   * Obtiene los horarios disponibles para reservar.
   * @param {string} fecha - Formato YYYY-MM-DD
   * @param {number} idRecurso 
   * @param {number} idServicio 
   */
  obtenerHorariosDisponibles: async (fecha, idRecurso, idServicio) => {
    const response = await api.get('/turnos/disponibles', {
      params: { fecha, idRecurso, idServicio }
    });
    return response.data;
  },

  obtenerDiasLlenos: async (anio, mes, idRecurso, idServicio) => {
    const response = await api.get('/turnos/dias-llenos', {
      params: { anio, mes, idRecurso, idServicio }
    });
    return response.data;
  },

  /**
   * Obtiene la agenda de turnos futuros de un recurso.
   * @param {number} recursoId 
   */
  obtenerAgendaRecurso: async (recursoId) => {
    const response = await api.get(`/turnos/agenda/${recursoId}`);
    return response.data;
  },

  /**
   * Cancela un turno por su ID.
   * @param {number} id 
   */
  cancelarTurno: async (id) => {
    const response = await api.delete(`/turnos/${id}`);
    return response.data;
  },

  /**
   * Cancela o consulta idempotentemente CONFIRMADO; no reconfirma turnos cancelados.
   * @param {number} id 
   * @param {string} nuevoEstado 
   */
  actualizarEstado: async (id, nuevoEstado) => {
    const response = await api.patch(`/turnos/${id}/estado`, null, {
      params: { nuevoEstado }
    });
    return response.data;
  },

  /**
   * Obtiene el historial de turnos de un negocio con paginación.
   */
  obtenerHistorialNegocio: async (negocioId, page = 0, size = 20) => {
    const response = await api.get(`/turnos/negocio/${negocioId}`, {
      params: { page, size }
    });
    return response.data;
  },

  /**
   * Obtiene el historial de turnos de un recurso específico con paginación.
   */
  obtenerHistorialRecurso: async (recursoId, page = 0, size = 20) => {
    const response = await api.get(`/turnos/recurso/${recursoId}/historial`, {
      params: { page, size }
    });
    return response.data;
  },

  /**
   * Obtiene los reportes de rendimiento.
   */
  obtenerReportes: async (negocioId, fechaInicio, fechaFin, recursoId = null) => {
    const params = { fechaInicio, fechaFin };
    if (recursoId) params.recursoId = recursoId;
    
    const response = await api.get(`/turnos/negocio/${negocioId}/reportes`, { params });
    return response.data;
  }
};
