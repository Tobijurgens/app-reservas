import { useState, useEffect } from 'react';
import { RecursosService } from '../../../services/RecursosService';

export const useMisAsignaciones = () => {
  const [asignaciones, setAsignaciones] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAsignaciones = async () => {
      try {
        const data = await RecursosService.obtenerMisAsignaciones();
        setAsignaciones(data);
      } catch (error) { console.error('Error al cargar asignaciones:', error); } 
      finally { setLoading(false); }
    };
    fetchAsignaciones();
  }, []);

  return { asignaciones, loading };
};