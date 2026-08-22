import { useState, useEffect } from 'react';
import api from '../../../services/api';

export const useMisAsignaciones = () => {
  const [asignaciones, setAsignaciones] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAsignaciones = async () => {
      try {
        const response = await api.get('/recursos/mis-asignaciones');
        setAsignaciones(response.data);
      } catch (error) {
        console.error('Error al cargar asignaciones:', error);
      } finally {
        setLoading(false);
      }
    };
    fetchAsignaciones();
  }, []);

  return { asignaciones, loading };
};