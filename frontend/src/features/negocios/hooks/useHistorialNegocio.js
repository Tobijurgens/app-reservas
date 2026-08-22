import { useState, useEffect } from 'react';
import api from '../../../services/api';

export const useHistorialNegocio = (negocioId) => {
  const [turnos, setTurnos] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // Estados para la paginación
  const [paginaActual, setPaginaActual] = useState(0);
  const [totalPaginas, setTotalPaginas] = useState(0);
  const [totalTurnos, setTotalTurnos] = useState(0);

  useEffect(() => {
    if (!negocioId) return;

    const fetchHistorial = async () => {
      try {
        setLoading(true);
        // Usamos x1 para la página actual y x2 para el tamaño (20)
        const response = await api.get(`/turnos/negocio/${negocioId}?x1=${paginaActual}&x2=20`);
        
        setTurnos(response.data.content); // Spring devuelve la lista dentro de "content"
        setTotalPaginas(response.data.totalPages);
        setTotalTurnos(response.data.totalElements);
      } catch (error) {
        console.error('Error al cargar el historial del negocio:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchHistorial();
  }, [negocioId, paginaActual]); // Se recarga si cambiás de página

  return { 
    turnos, 
    loading, 
    paginaActual, 
    setPaginaActual, 
    totalPaginas,
    totalTurnos
  };
};