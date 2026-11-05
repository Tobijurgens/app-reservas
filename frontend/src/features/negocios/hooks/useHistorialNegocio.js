import { useState, useEffect } from 'react';
import { TurnosService } from '../../../services/TurnosService';

export const useHistorialNegocio = (negocioId) => {
  const [turnos, setTurnos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [paginaActual, setPaginaActual] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [totalTurnos, setTotalTurnos] = useState(0);

  useEffect(() => {
    if (!negocioId) return;
    const fetchHistorial = async () => {
      try {
        setLoading(true);
        const data = await TurnosService.obtenerHistorialNegocio(negocioId, paginaActual, 20);
        setTurnos(data.content);
        setTotalPaginas(data.totalPages);
        setTotalTurnos(data.totalElements);
      } catch (error) { console.error('Error al cargar historial:', error); } 
      finally { setLoading(false); }
    };
    fetchHistorial();
  }, [negocioId, paginaActual]);

  return { turnos, loading, paginaActual, setPaginaActual, totalPaginas, totalTurnos };
};