import { useState, useEffect, useCallback } from 'react';
import { TurnosService } from '../../../services/TurnosService';
import { useToast } from '../../../context/ToastContext';

export const useMisReservas = () => {
  const [reservas, setReservas] = useState([]);
  const [loading, setLoading] = useState(true);
  const { addToast } = useToast();

  const fetchReservas = useCallback(async () => {
    try {
      setLoading(true);
      const data = await TurnosService.obtenerMisTurnos();
      setReservas(data.sort((a, b) => new Date(a.fechaHoraInicio) - new Date(b.fechaHoraInicio)));
    } catch (err) { console.error(err); } finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchReservas(); }, [fetchReservas]);

  // Modificado: Ahora devuelve true o false en lugar de usar window.confirm
  const cancelarReserva = async (id) => {
    try {
      await TurnosService.cancelarTurno(id);
      addToast('Turno cancelado exitosamente.', 'success');
      await fetchReservas();
      return true;
    } catch (err) { 
      addToast(err.response?.data?.message || 'Error al cancelar la reserva.', 'error'); 
      return false;
    }
  };

  return { reservas, loading, cancelarReserva };
};