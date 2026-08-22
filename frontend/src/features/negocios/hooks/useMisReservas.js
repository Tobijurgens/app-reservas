import { useState, useEffect, useCallback } from 'react';
import api from '../../../services/api';

export const useMisReservas = () => {
  const [reservas, setReservas] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchReservas = useCallback(async () => {
    try {
      setLoading(true);
      // Ajustá esta ruta según tu controlador de Spring Boot
      const response = await api.get('/turnos/mis-turnos'); 
      
      // Ordenamos para que los turnos más próximos aparezcan primero
      const turnosOrdenados = response.data.sort((a, b) => 
        new Date(a.fechaHoraInicio) - new Date(b.fechaHoraInicio)
      );
      
      setReservas(turnosOrdenados);
    } catch (err) {
      console.error('Error al traer reservas:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchReservas();
  }, [fetchReservas]);

  const cancelarReserva = async (id) => {
    if (window.confirm('¿Estás seguro de que querés cancelar este turno? Esta acción no se puede deshacer.')) {
      try {
        // CORREGIDO: Usamos el método delete y la ruta exacta que espera tu controlador
        await api.delete(`/turnos/${id}`); 
        
        await fetchReservas(); // Recargamos la lista para que se actualice la vista
      } catch (err) {
        alert(err.response?.data?.message || 'Error al cancelar la reserva.');
      }
    }
  };

  return { reservas, loading, cancelarReserva };
};