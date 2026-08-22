import { useState, useEffect } from 'react';
import api from '../../../services/api';

export const useGestionMiTrabajo = (recursoId) => {
  const [recurso, setRecurso] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchRecurso = async () => {
    try {
      setLoading(true);
      
      // 1. Traemos los datos del espacio de trabajo
      const response = await api.get(`/recursos/${recursoId}`);
      const recursoData = response.data;
      
      // 2. CORRECCIÓN: Traemos los servicios asignados a este recurso
      try {
        const resServicios = await api.get(`/servicios/recurso/${recursoId}`);
        recursoData.servicios = resServicios.data;
      } catch (err) {
        console.error('Error al cargar servicios del recurso', err);
        recursoData.servicios = [];
      }

      setRecurso(recursoData);
    } catch (error) {
      console.error('Error al cargar mi espacio de trabajo:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (recursoId) fetchRecurso();
  }, [recursoId]);

  const guardarRecurso = async (formData) => {
    try {
      await api.put(`/recursos/${recursoId}`, formData);
      await fetchRecurso();
      return { success: true };
    } catch (error) {
      return { success: false, message: error.response?.data?.message || 'Error al guardar configuración' };
    }
  };

  const guardarServicio = async (formData, servicioId) => {
    try {
      if (servicioId) {
        await api.put(`/servicios/${servicioId}`, formData);
      } else {
        await api.post(`/servicios`, { ...formData, idRecurso: recursoId });
      }
      await fetchRecurso();
      return { success: true };
    } catch (error) {
      console.error(error);
      return { success: false };
    }
  };

  const eliminarServicio = async (servicioId) => {
    try {
      await api.delete(`/servicios/${servicioId}`);
      await fetchRecurso();
    } catch (error) {
      console.error(error);
    }
  };

  return { recurso, loading, guardarRecurso, guardarServicio, eliminarServicio };
};