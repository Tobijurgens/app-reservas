import { useState, useEffect } from 'react';
import api from '../../../services/api';

export const useAgendaRecurso = (recursoId) => {
  const [turnos, setTurnos] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!recursoId) return;

    const fetchAgenda = async () => {
      try {
        setLoading(true);
        const response = await api.get(`/turnos/agenda/${recursoId}`);
        setTurnos(response.data);
      } catch (error) {
        console.error('Error al cargar agenda del recurso:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchAgenda();
  }, [recursoId]);

  const cancelarTurno = async (idTurno) => {
    try {
      await api.delete(`/turnos/${idTurno}`);
      setTurnos(turnosAnteriores => 
        turnosAnteriores.map(turno => 
          turno.id === idTurno ? { ...turno, estado: 'CANCELADO' } : turno
        )
      );
      return true;
    } catch (error) {
      alert(error.response?.data?.message || 'Error al cancelar el turno.');
      return false;
    }
  };

  return { turnos, loading, cancelarTurno };
};