import { useState, useEffect } from 'react';
import api from '../../../services/api';

// Ahora recibe recursoId
export const useReportesNegocio = (negocioId, fechaInicio, fechaFin, recursoId) => {
  const [reporte, setReporte] = useState(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!negocioId || !fechaInicio || !fechaFin) return;

    const fetchReporte = async () => {
      try {
        setLoading(true);
        const x1 = `${fechaInicio}T00:00:00`;
        const x2 = `${fechaFin}T23:59:59`;
        
        // Armamos la URL base con las fechas
        let url = `/turnos/negocio/${negocioId}/reportes?x1=${x1}&x2=${x2}`;
        
        // Si hay un recurso seleccionado y no es 'todos', le pasamos el ID al backend
        if (recursoId && recursoId !== 'todos') {
          url += `&recursoId=${recursoId}`;
        }

        const response = await api.get(url);
        setReporte(response.data);
      } catch (error) {
        console.error('Error al cargar reportes:', error);
      } finally {
        setLoading(false);
      }
    };

    fetchReporte();
  }, [negocioId, fechaInicio, fechaFin, recursoId]); // Agregamos recursoId a las dependencias

  return { reporte, loading };
};