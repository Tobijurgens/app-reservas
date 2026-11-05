import { useState, useEffect } from 'react';
import { RecursosService } from '../../../services/RecursosService';
import { ServiciosService } from '../../../services/ServiciosService';

export const useGestionMiTrabajo = (recursoId) => {
  const [recurso, setRecurso] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchRecurso = async () => {
    try {
      setLoading(true);
      const recursoData = await RecursosService.obtenerPorId(recursoId);
      try {
        recursoData.servicios = await ServiciosService.obtenerPorRecurso(recursoId);
      } catch (err) { recursoData.servicios = []; }
      setRecurso(recursoData);
    } catch (error) { console.error(error); } finally { setLoading(false); }
  };

  useEffect(() => { if (recursoId) fetchRecurso(); }, [recursoId]);

  const guardarRecurso = async (formData) => {
    try {
      await RecursosService.actualizarRecurso(recursoId, formData);
      await fetchRecurso();
      return { success: true };
    } catch (error) { return { success: false, message: error.response?.data?.message || 'Error' }; }
  };

  const guardarServicio = async (formData, servicioId) => {
    try {
      if (servicioId) await ServiciosService.actualizarServicio(servicioId, formData);
      else await ServiciosService.crearServicio({ ...formData, idRecurso: recursoId });
      await fetchRecurso();
      return { success: true };
    } catch (error) { return { success: false }; }
  };

  const eliminarServicio = async (servicioId) => {
    try { await ServiciosService.eliminarServicio(servicioId); await fetchRecurso(); } catch (error) { console.error(error); }
  };

  return { recurso, loading, guardarRecurso, guardarServicio, eliminarServicio };
};