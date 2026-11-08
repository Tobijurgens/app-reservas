import { useCallback, useEffect, useState } from 'react';
import { AuthContext } from './AuthContext';
import { AuthService } from '../services/AuthService';
import api from '../services/api';
import { mensajeError } from '../utils/errores';
import { debeInvalidarSesion, sesionDesdeToken } from '../utils/sesion';

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => sesionDesdeToken(localStorage.getItem('token')));

  const logout = useCallback(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('userEmail');
    setUser(null);
  }, []);

  useEffect(() => {
    if (!user) {
      localStorage.removeItem('token');
      localStorage.removeItem('userEmail');
      return;
    }
    const timer = setTimeout(logout, Math.max(0, user.expiraEn - Date.now()));
    return () => clearTimeout(timer);
  }, [user, logout]);

  useEffect(() => {
    const interceptor = api.interceptors.response.use(
      response => response,
      error => {
        if (debeInvalidarSesion(error, localStorage.getItem('token'))) logout();
        return Promise.reject(error);
      },
    );
    return () => api.interceptors.response.eject(interceptor);
  }, [logout]);

  const login = async (email, password) => {
    try {
      const response = await AuthService.login({ email, password });
      const sesion = sesionDesdeToken(response.token);
      if (!sesion) return { success: false, message: 'No se recibió una sesión válida. Intentá nuevamente.' };
      localStorage.setItem('token', sesion.token);
      localStorage.setItem('userEmail', sesion.email);
      setUser(sesion);
      return { success: true };
    } catch (error) {
      let message = mensajeError(error, 'Error al iniciar sesión.');
      const texto = message.toLowerCase();
      if (texto.includes('disabled') || texto.includes('deshabilitado')) {
        message = 'Tu cuenta aún no está verificada. Por favor, revisá tu correo y activala para poder ingresar.';
      } else if (texto.includes('bad credentials') || texto.includes('credenciales')) {
        message = 'El correo o la contraseña son incorrectos. Intentá nuevamente.';
      }
      return { success: false, message };
    }
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, loading: false }}>
      {children}
    </AuthContext.Provider>
  );
};
