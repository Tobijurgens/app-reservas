import { useState, useEffect, useCallback } from 'react';
import api from '../../../services/api';

export const useGestionNegocio = (idNegocio) => {
  const [negocio, setNegocio] = useState(null);
  const [recursosTotales, setRecursosTotales] = useState([]); // Histórico completo
  const [recursosActivos, setRecursosActivos] = useState([]); // Solo activos
  const [loading, setLoading] = useState(true);

  const fetchData = useCallback(async () => {
    if (!idNegocio) return;
    try {
      setLoading(true);
      const resNegocio = await api.get(`/negocios/${idNegocio}`);
      setNegocio(resNegocio.data);

      // 1. Endpoint público: SOLO los activos (para editar servicios y ver agenda)
      const resRecursosActivos = await api.get(`/recursos/negocio/${idNegocio}`);
      const activosData = resRecursosActivos.data;

      // 2. Endpoint histórico: TODOS (para reportes e historial)
      const resRecursosTotales = await api.get(`/recursos/negocio/${idNegocio}/todos`);
      const totalesData = resRecursosTotales.data;

      // 3. A los activos les buscamos sus servicios para poder editarlos en el acordeón
      const activosConServicios = await Promise.all(
        activosData.map(async (recurso) => {
          try {
            const resServicios = await api.get(`/servicios/recurso/${recurso.id}`);
            return { ...recurso, servicios: resServicios.data };
          } catch (err) {
            console.error(`Error servicios recurso ${recurso.id}`, err);
            return { ...recurso, servicios: [] };
          }
        })
      );
      
      setRecursosActivos(activosConServicios); 
      setRecursosTotales(totalesData);         
      
    } catch (err) {
      console.error('Error al cargar datos:', err);
    } finally {
      setLoading(false);
    }
  }, [idNegocio]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const guardarRecurso = async (formData, editingId) => {
    try {
      const payload = {
        ...formData,
        idNegocio: Number(idNegocio),
        emailUsuario: formData.emailUsuario?.trim() || null
      };

      if (editingId) await api.put(`/recursos/${editingId}`, payload);
      else await api.post('/recursos', payload);
      
      await fetchData();
      return { success: true };
    } catch (err) {
      return { success: false, message: err.response?.data?.message || 'Error al guardar.' };
    }
  };

  const eliminarRecurso = async (id, nombre) => {
    if (window.confirm(`¿Eliminar el recurso "${nombre}" y todos sus servicios?`)) {
      try {
        await api.delete(`/recursos/${id}`);
        await fetchData();
      } catch (err) {
        alert(err.response?.data?.message || 'Error al eliminar');
      }
    }
  };

  const guardarServicio = async (formData, editingId, idRecurso) => {
    try {
      const payload = {
        ...formData,
        precio: Number(formData.precio),
        duracionMinutos: Number(formData.duracionMinutos),
        idRecurso: idRecurso
      };

      if (editingId) await api.put(`/servicios/${editingId}`, payload);
      else await api.post('/servicios', payload);
      
      await fetchData();
      return { success: true };
    } catch (err) {
      return { success: false, message: err.response?.data?.message || 'Error al guardar.' };
    }
  };

  const eliminarServicio = async (id, nombre) => {
    if (window.confirm(`¿Seguro de eliminar el servicio "${nombre}"?`)) {
      try {
        await api.delete(`/servicios/${id}`);
        await fetchData();
      } catch (err) {
        alert(err.response?.data?.message || 'Error al eliminar');
      }
    }
  };

  return { 
    negocio, 
    recursosTotales, 
    recursosActivos, 
    loading, 
    guardarRecurso, 
    eliminarRecurso, 
    guardarServicio, 
    eliminarServicio 
  };
};