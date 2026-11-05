import { useState, useEffect } from 'react';
import { TurnosService } from '../../../services/TurnosService';

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
        const filtroRecurso = recursoId !== 'todos' ? recursoId : null;
        const data = await TurnosService.obtenerReportes(negocioId, x1, x2, filtroRecurso);
        setReporte(data);
      } catch (error) { console.error(error); } finally { setLoading(false); }
    };
    fetchReporte();
  }, [negocioId, fechaInicio, fechaFin, recursoId]);

  return { reporte, loading };
};