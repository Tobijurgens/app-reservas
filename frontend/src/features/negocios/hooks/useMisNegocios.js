import { useState, useEffect } from 'react';
import { NegociosService } from '../../../services/NegociosService';

export const useMisNegocios = () => {
  const [misNegocios, setMisNegocios] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchMisNegocios = async () => {
    try {
      setIsLoading(true);
      const data = await NegociosService.obtenerMisNegocios();
      setMisNegocios(data);
    } catch (err) { 
      setError("Error al traer negocios"); 
      console.error(err); 
    } finally { setIsLoading(false); }
  };

  useEffect(() => { fetchMisNegocios(); }, []);

  const eliminarNegocio = async (id) => {
    try {
      await NegociosService.eliminarNegocio(id);
      await fetchMisNegocios();
      return { success: true };
    } catch (err) { 
      return { success: false, message: err.response?.data?.message || 'Error al eliminar el negocio.' }; 
    }
  };

  const guardarNegocio = async (formData, editingId) => {
    setIsSubmitting(true);
    try {
      if (editingId) await NegociosService.actualizarNegocio(editingId, formData);
      else await NegociosService.crearNegocio(formData);
      
      await fetchMisNegocios();
      return { success: true };
    } catch (err) { 
      return { success: false, message: err.response?.data?.message || 'Error.' }; 
    } finally { setIsSubmitting(false); }
  };

  return { misNegocios, isLoading, error, isSubmitting, eliminarNegocio, guardarNegocio };
};