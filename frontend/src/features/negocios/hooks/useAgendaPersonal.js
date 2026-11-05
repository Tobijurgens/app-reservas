import { useState, useEffect } from 'react';
import { RecursosService } from '../../../services/RecursosService';
import { TurnosService } from '../../../services/TurnosService';

export const useAgendaPersonal = () => {
  const [recursosAsignados, setRecursosAsignados] = useState([]);
  const [recursoSeleccionado, setRecursoSeleccionado] = useState(null);
  const [turnos, setTurnos] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchRecursos = async () => {
      try {
        setLoading(true);
        const data = await RecursosService.obtenerMisAsignaciones();
        setRecursosAsignados(data);
      } catch (error) { console.error('Error al cargar asignaciones:', error); } 
      finally { setLoading(false); }
    };
    fetchRecursos();
  }, []);

  useEffect(() => {
    const fetchAgenda = async () => {
      if (!recursoSeleccionado) { setTurnos([]); return; }
      try {
        setLoading(true);
        const data = await TurnosService.obtenerAgendaRecurso(recursoSeleccionado);
        setTurnos(data);
      } catch (error) { console.error('Error al cargar agenda:', error); } 
      finally { setLoading(false); }
    };
    fetchAgenda();
  }, [recursoSeleccionado]);

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

  return { recursosAsignados, recursoSeleccionado, setRecursoSeleccionado, turnos, loading, cancelarTurno };
};