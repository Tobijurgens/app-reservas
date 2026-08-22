import { useState, useEffect } from 'react';
import api from '../../../services/api';

export const useHistorialRecurso = (recursoId) => {
  const [turnos, setTurnos] = useState([]);
  const [loading, setLoading] = useState(true);
  
  const [paginaActual, setPaginaActual] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [totalTurnos, setTotalTurnos] = useState(0);

  useEffect(() => {
    if (!recursoId) return; // Si no hay ID, no busca nada

    const fetchHistorial = async () => {
      try {
        setLoading(true);
        // Usamos x1 y x2 como definimos en el backend
        const response = await api.get(`/turnos/recurso/${recursoId}/historial?x1=${paginaActual}&x2=20`);
        
        if (response.data && response.data.content) {
          setTurnos(response.data.content);
          setTotalPaginas(response.data.totalPages);
          setTotalTurnos(response.data.totalElements);
        } else {
          setTurnos([]);
        }
      } catch (error) {
        console.error('Error al cargar el historial del recurso:', error);
        setTurnos([]);
      } finally {
        setLoading(false);
      }
    };

    fetchHistorial();
  }, [recursoId, paginaActual]);

  return { 
    turnos, loading, paginaActual, setPaginaActual, totalPaginas, totalTurnos 
  };
};