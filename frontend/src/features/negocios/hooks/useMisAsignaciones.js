import { useState, useEffect, useCallback } from 'react';
import { RecursosService } from '../../../services/RecursosService';
import { mensajeError } from '../../../utils/errores';

export const useMisAsignaciones = () => {
  const [asignaciones, setAsignaciones] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const cargar = useCallback((vigente = () => true) => (
    RecursosService.obtenerMisAsignaciones()
      .then(data => {
        if (vigente()) { setAsignaciones(data); setError(null); }
      })
      .catch(err => { if (vigente()) setError(mensajeError(err, 'No pudimos cargar tus trabajos. Revisá tu conexión.')); })
      .finally(() => { if (vigente()) setLoading(false); })
  ), []);

  useEffect(() => {
    let vigente = true;
    cargar(() => vigente);
    return () => { vigente = false; };
  }, [cargar]);

  const reintentar = () => {
    setLoading(true);
    setError(null);
    return cargar();
  };

  return { asignaciones, loading, error, reintentar };
};
