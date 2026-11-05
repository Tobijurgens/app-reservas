// src/services/UsuariosService.js
import api from './api';

export const UsuariosService = {
  obtenerTodos: async () => {
    const response = await api.get('/usuarios');
    return response.data;
  },

  obtenerPorId: async (id) => {
    const response = await api.get(`/usuarios/${id}`);
    return response.data;
  },

  obtenerPorEmail: async (email) => {
    const response = await api.get(`/usuarios/email/${email}`);
    return response.data;
  },

  actualizarPerfil: async (perfilRequest) => {
    const response = await api.put('/usuarios/perfil', perfilRequest);
    return response.data;
  },

  eliminarUsuario: async (id) => {
    const response = await api.delete(`/usuarios/${id}`);
    return response.data;
  }
};