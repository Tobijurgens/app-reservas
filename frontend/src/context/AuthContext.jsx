import { createContext, useState, useEffect } from 'react';
import api from '../services/api';

export const AuthContext = createContext();

// Función nativa para leer un JWT sin instalar librerías externas
const decodificarToken = (token) => {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(atob(base64));
  } catch (e) {
    console.error("Error al decodificar el token", e);
    return null;
  }
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const checkToken = () => {
      const token = localStorage.getItem('token');
      // Intentamos leer el email guardado, o sacarlo del token si falta
      let email = localStorage.getItem('userEmail'); 
      
      if (token) {
        const payload = decodificarToken(token);
        if (payload) {
             if(!email){
                 email = payload.sub || payload.email;
             }
          // Guardamos el perfil completo
          setUser({ 
            token: token, 
            email: email 
          });
        } else {
          localStorage.removeItem('token');
          localStorage.removeItem('userEmail');
        }
      }
      setLoading(false);
    };
    checkToken();
  }, []);

  const login = async (email, password) => {
    try {
      const response = await api.post('/auth/login', { email, password });
      const token = response.data.token; 
      
      const payload = decodificarToken(token);
      const emailFinal = payload?.sub || payload?.email || email;
      
      localStorage.setItem('token', token);
      localStorage.setItem('userEmail', emailFinal); // Guardamos el email
      
      setUser({ 
        token: token, 
        email: emailFinal 
      });
      
      return { success: true };
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.message || 'Error al iniciar sesión' 
      };
    }
  };

  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('userEmail');
    setUser(null); 
    window.location.href = '/'; 
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, loading }}>
      {children}
    </AuthContext.Provider>
  );
};