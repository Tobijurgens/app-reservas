import { useState, useEffect, useCallback } from 'react';
import { NegociosService } from '../../../services/NegociosService';
import { RecursosService } from '../../../services/RecursosService';
import { ServiciosService } from '../../../services/ServiciosService';

export const useGestionNegocio = (idNegocio) => {
  const [negocio, setNegocio] = useState(null);
  const [recursosTotales, setRecursosTotales] = useState([]);
  const [recursosActivos, setRecursosActivos] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchData = useCallback(async () => {
    if (!idNegocio) return;
    try {
      setLoading(true);
      const [negocioData, activosData, totalesData] = await Promise.all([
        NegociosService.obtenerPorId(idNegocio),
        RecursosService.obtenerPorNegocio(idNegocio),
        RecursosService.obtenerTodosPorNegocio(idNegocio)
      ]);

      setNegocio(negocioData);
      setRecursosTotales(totalesData);

      const activosConServicios = await Promise.all(
        activosData.map(async (recurso) => {
          try {
            const servicios = await ServiciosService.obtenerPorRecurso(recurso.id);
            return { ...recurso, servicios };
          } catch (err) {
            return { ...recurso, servicios: [] };
          }
        })
      );
      setRecursosActivos(activosConServicios); 
    } catch (err) {
      console.error('Error al cargar datos:', err);
    } finally {
      setLoading(false);
    }
  }, [idNegocio]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const guardarRecurso = async (formData, editingId) => {
    try {
      const payload = { ...formData, idNegocio: Number(idNegocio), emailUsuario: formData.emailUsuario?.trim() || null };
      if (editingId) await RecursosService.actualizarRecurso(editingId, payload);
      else await RecursosService.crearRecurso(payload);
      
      await fetchData();
      return { success: true };
    } catch (err) {
      return { success: false, message: err.response?.data?.message || 'Error al guardar.' };
    }
  };

  const eliminarRecurso = async (id) => {
    try {
      await RecursosService.eliminarRecurso(id);
      await fetchData();
      return { success: true };
    } catch (err) { return { success: false, message: err.response?.data?.message || 'Error al eliminar' }; }
  };

  const guardarServicio = async (formData, editingId, idRecurso) => {
    try {
      const payload = { ...formData, precio: Number(formData.precio), duracionMinutos: Number(formData.duracionMinutos), idRecurso };
      if (editingId) await ServiciosService.actualizarServicio(editingId, payload);
      else await ServiciosService.crearServicio(payload);
      
      await fetchData();
      return { success: true };
    } catch (err) { return { success: false, message: err.response?.data?.message || 'Error.' }; }
  };

  const eliminarServicio = async (id) => {
    try {
      await ServiciosService.eliminarServicio(id);
      await fetchData();
      return { success: true };
    } catch (err) { return { success: false, message: err.response?.data?.message || 'Error al eliminar' }; }
  };

  return { negocio, recursosTotales, recursosActivos, loading, guardarRecurso, eliminarRecurso, guardarServicio, eliminarServicio };
};