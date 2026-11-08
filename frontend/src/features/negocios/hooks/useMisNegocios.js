import { useState, useEffect, useCallback } from 'react';
import { NegociosService } from '../../../services/NegociosService';
import { mensajeError } from '../../../utils/errores';

export const useMisNegocios = () => {
  const [misNegocios, setMisNegocios] = useState([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchMisNegocios = useCallback((vigente = () => true) => (
    NegociosService.obtenerMisNegocios()
      .then(data => {
        if (vigente()) { setMisNegocios(data); setError(null); }
      })
      .catch(err => { if (vigente()) setError(mensajeError(err, 'No pudimos cargar tus negocios. Revisá tu conexión.')); })
      .finally(() => { if (vigente()) setIsLoading(false); })
  ), []);

  useEffect(() => {
    let vigente = true;
    fetchMisNegocios(() => vigente);
    return () => { vigente = false; };
  }, [fetchMisNegocios]);

  const reintentar = () => {
    setIsLoading(true);
    setError(null);
    return fetchMisNegocios();
  };

  const eliminarNegocio = async id => {
    try {
      await NegociosService.eliminarNegocio(id);
      setMisNegocios(prev => prev.filter(negocio => negocio.id !== id));
      await reintentar();
      return { success: true };
    } catch (err) {
      return { success: false, message: mensajeError(err, 'Error al eliminar el negocio.') };
    }
  };

  const guardarNegocio = async (formData, editingId) => {
    setIsSubmitting(true);
    try {
      if (editingId) await NegociosService.actualizarNegocio(editingId, formData);
      else await NegociosService.crearNegocio(formData);
      await reintentar();
      return { success: true };
    } catch (err) {
      return { success: false, message: mensajeError(err, 'Error al guardar el negocio.') };
    } finally { setIsSubmitting(false); }
  };

  return { misNegocios, isLoading, error, reintentar, isSubmitting, eliminarNegocio, guardarNegocio };
};
