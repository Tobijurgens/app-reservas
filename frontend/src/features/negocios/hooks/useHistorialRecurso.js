import { useState, useEffect } from 'react';
import { TurnosService } from '../../../services/TurnosService';

export const useHistorialRecurso = (recursoId) => {
  const [turnos, setTurnos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [paginaActual, setPaginaActual] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [totalTurnos, setTotalTurnos] = useState(0);

  useEffect(() => {
    if (!recursoId) return;
    const fetchHistorial = async () => {
      try {
        setLoading(true);
        const data = await TurnosService.obtenerHistorialRecurso(recursoId, paginaActual, 20);
        if (data && data.content) {
          setTurnos(data.content);
          setTotalPaginas(data.totalPages);
          setTotalTurnos(data.totalElements);
        } else { setTurnos([]); }
      } catch (error) { 
        console.error('Error al cargar historial:', error); 
        setTurnos([]); 
      } finally { setLoading(false); }
    };
    fetchHistorial();
  }, [recursoId, paginaActual]);

  return { turnos, loading, paginaActual, setPaginaActual, totalPaginas, totalTurnos };
};