import { useState, useEffect } from 'react';
import api from '../../../services/api';

export const useAgendaPersonal = () => {
  const [recursosAsignados, setRecursosAsignados] = useState([]);
  const [recursoSeleccionado, setRecursoSeleccionado] = useState(null);
  const [turnos, setTurnos] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchRecursosAsignados = async () => {
      try {
        setLoading(true);
        const response = await api.get('/recursos/mis-asignaciones');
        setRecursosAsignados(response.data);
      } catch (error) {
        console.error('Error al cargar asignaciones:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchRecursosAsignados();
  }, []);

  useEffect(() => {
    const fetchAgenda = async () => {
      if (!recursoSeleccionado) {
        setTurnos([]);
        return;
      }
      try {
        setLoading(true);
        const response = await api.get(`/turnos/agenda/${recursoSeleccionado}`);
        setTurnos(response.data);
      } catch (error) {
        console.error('Error al cargar agenda:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchAgenda();
  }, [recursoSeleccionado]);

  // NUEVA FUNCIÓN: CANCELAR TURNO
  const cancelarTurno = async (idTurno) => {
    try {
      await api.delete(`/turnos/${idTurno}`);
      setTurnos(turnosAnteriores => 
        turnosAnteriores.map(turno => 
          turno.id === idTurno ? { ...turno, estado: 'CANCELADO' } : turno
        )
      );
      return true; // Devolvemos true para que la vista sepa que salió bien
    } catch (error) {
      alert(error.response?.data?.message || 'Error al cancelar el turno.');
      return false; // Devolvemos false si falló
    }
  };

  return {
    recursosAsignados,
    recursoSeleccionado,
    setRecursoSeleccionado,
    turnos,
    loading,
    cancelarTurno // Exportamos la nueva función
  };
};