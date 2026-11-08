// src/services/UsuariosService.js
import api from './api';

export const UsuariosService = {
  obtenerMiPerfil: async () => {
    const response = await api.get('/usuarios/perfil');
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
