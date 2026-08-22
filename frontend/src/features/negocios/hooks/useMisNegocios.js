import { useState, useEffect } from 'react';
import api from '../../../services/api';

export const useMisNegocios = () => {
  const [misNegocios, setMisNegocios] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchMisNegocios = async () => {
    try {
      setIsLoading(true);
      const response = await api.get('/negocios/mis-negocios'); 
      setMisNegocios(response.data);
    } catch (err) {
      setError("Error al traer negocios");
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    fetchMisNegocios();
  }, []);

  const eliminarNegocio = async (id, nombre) => {
    if (window.confirm(`¿Seguro que querés eliminar "${nombre}"?`)) {
      try {
        await api.delete(`/negocios/${id}`);
        fetchMisNegocios();
      } catch (err) {
        alert(err.response?.data?.message || 'Error al eliminar el negocio.');
      }
    }
  };

  const guardarNegocio = async (formData, editingId) => {
    setIsSubmitting(true);
    try {
      if (editingId) {
        await api.put(`/negocios/${editingId}`, formData);
      } else {
        await api.post('/negocios', formData);
      }
      await fetchMisNegocios();
      return { success: true };
    } catch (err) {
      return { success: false, message: err.response?.data?.message || 'Error.' };
    } finally {
      setIsSubmitting(false);
    }
  };

  return { misNegocios, isLoading, error, isSubmitting, eliminarNegocio, guardarNegocio };
};