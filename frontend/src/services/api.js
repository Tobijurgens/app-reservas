import axios from 'axios';

// 1. Instancia base que apunta a tu API Gateway
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// 2. Interceptor de Solicitud (Request)
// Se ejecuta siempre ANTES de que la petición salga al backend
api.interceptors.request.use(
  (config) => {
    // Buscamos el token en localStorage
    const token = localStorage.getItem('token');
    
    // Si el usuario está logueado, inyectamos el Bearer token
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 3. Interceptor de Respuesta (Response)
// Se ejecuta cuando Spring Boot responde, ANTES de llegar a tus componentes
api.interceptors.response.use(
  (response) => {
    // Todo salió bien (status 200-299)
    return response;
  },
  (error) => {
    // Manejo global de errores
    if (error.response) {
      // Si Spring Boot nos devuelve un 401 (Unauthorized - token vencido o inválido)
      if (error.response.status === 401) {
        console.warn("Sesión expirada o token faltante.");
        localStorage.removeItem('token');
        
        // ¡BINGO! Eliminamos el window.location.href = '/login';
        // Ahora Axios rechaza la promesa limpiamente y deja que el componente 
        // (ej: ModalReserva) ataje el error y muestre el modal de login sin recargar la página.
      }
    }
    return Promise.reject(error);
  }
);

export default api;