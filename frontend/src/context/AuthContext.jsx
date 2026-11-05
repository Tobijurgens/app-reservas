import { createContext, useState, useEffect } from 'react';
import { AuthService } from '../services/AuthService';
import { jwtDecode } from 'jwt-decode';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const checkToken = () => {
      const token = localStorage.getItem('token');
      let email = localStorage.getItem('userEmail'); 
      if (token) {
        try {
          const payload = jwtDecode(token);
          if (payload) {
            if (!email) email = payload.sub || payload.email;
            const expirationTime = payload.exp * 1000;
            const timeUntilExp = expirationTime - Date.now();

            if (timeUntilExp <= 0) {
              localStorage.removeItem('token');
              localStorage.removeItem('userEmail');
              setUser(null);
            } else {
              setUser({ token, email });
              setTimeout(() => {
                logout();
                window.location.href = '/login';
              }, timeUntilExp);
            }
          }
        } catch (error) {
          localStorage.removeItem('token');
          localStorage.removeItem('userEmail');
          setUser(null);
        }
      }
      setLoading(false);
    };
    checkToken();
  }, []);

  const login = async (email, password) => {
    try {
      const response = await AuthService.login({ email, password });
      const token = response.token; 
      const payload = jwtDecode(token);
      const emailFinal = payload?.sub || payload?.email || email;
      
      localStorage.setItem('token', token);
      localStorage.setItem('userEmail', emailFinal);
      setUser({ token, email: emailFinal });
      
      return { success: true };
    } catch (error) {
      let msg = error.response?.data?.message || error.response?.data || 'Error al iniciar sesión';
      
      if (typeof msg === 'string') {
        const msgLower = msg.toLowerCase();
        
        if (msgLower.includes('disabled') || msgLower.includes('deshabilitado')) {
          msg = 'Tu cuenta aún no está verificada. Por favor, revisá tu correo y activala para poder ingresar.';
        } 
        else if (msgLower.includes('bad credentials') || msgLower.includes('credenciales')) {
          msg = 'El correo o la contraseña son incorrectos. Intentá nuevamente.';
        }
      }

      return { success: false, message: msg };
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