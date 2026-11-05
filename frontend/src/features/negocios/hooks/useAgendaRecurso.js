import { useState, useEffect } from 'react';
import { TurnosService } from '../../../services/TurnosService';

export const useAgendaRecurso = (recursoId) => {
  const [turnos, setTurnos] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!recursoId) return;
    const fetchAgenda = async () => {
      try {
        setLoading(true);
        const data = await TurnosService.obtenerAgendaRecurso(recursoId);
        setTurnos(data);
      } catch (error) { console.error('Error al cargar agenda del recurso:', error); } 
      finally { setLoading(false); }
    };
    fetchAgenda();
  }, [recursoId]);

  const cancelarTurno = async (idTurno) => {
    try {
      await TurnosService.cancelarTurno(idTurno);
      setTurnos(prev => prev.map(t => t.id === idTurno ? { ...t, estado: 'CANCELADO' } : t));
      return true;
    } catch (error) {
      alert(error.response?.data?.message || 'Error al cancelar el turno.');
      return false;
    }
  };

  return { turnos, loading, cancelarTurno };
};