import { useState, useEffect, useCallback } from 'react';
import { TurnosService } from '../../../services/TurnosService';
import { useToast } from '../../../context/ToastContext';
import { mensajeError } from '../../../utils/errores';

export const useMisReservas = () => {
  const [reservas, setReservas] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { addToast } = useToast();

  const fetchReservas = useCallback((vigente = () => true) => (
    TurnosService.obtenerMisTurnos()
      .then(data => {
        if (vigente()) {
          setReservas([...data].sort((a, b) => new Date(a.fechaHoraInicio) - new Date(b.fechaHoraInicio)));
          setError(null);
        }
      })
      .catch(err => { if (vigente()) setError(mensajeError(err, 'No pudimos cargar tus reservas. Revisá tu conexión.')); })
      .finally(() => { if (vigente()) setLoading(false); })
  ), []);

  useEffect(() => {
    let vigente = true;
    fetchReservas(() => vigente);
    return () => { vigente = false; };
  }, [fetchReservas]);

  const reintentar = () => {
    setLoading(true);
    setError(null);
    return fetchReservas();
  };

  const cancelarReserva = async id => {
    try {
      await TurnosService.cancelarTurno(id);
      addToast('Turno cancelado exitosamente.', 'success');
      await reintentar();
      return true;
    } catch (err) {
      addToast(mensajeError(err, 'Error al cancelar la reserva.'), 'error');
      return false;
    }
  };

  return { reservas, loading, error, reintentar, cancelarReserva };
};
