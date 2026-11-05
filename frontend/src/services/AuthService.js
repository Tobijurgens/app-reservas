// src/services/AuthService.js
import api from './api';

export const AuthService = {
  /**
   * Registra un nuevo usuario.
   * @param {Object} registerRequest - { nombre, apellido, email, telefono, password }
   */
  register: async (registerRequest) => {
    const response = await api.post('/auth/register', registerRequest);
    return response.data;
  },

  /**
   * Inicia sesión y devuelve el token JWT.
   * @param {Object} loginRequest - { email, password }
   */
  login: async (loginRequest) => {
    const response = await api.post('/auth/login', loginRequest);
    return response.data;
  },

  /**
   * Verifica la cuenta del usuario mediante un token.
   * @param {string} token 
   */
  verificarCuenta: async (token) => {
    const response = await api.get('/auth/verificar', { params: { token } });
    return response.data;
  }
};