import { useState, useEffect } from 'react';
import api from '../../../services/api';

export const useNegocioPublico = (id) => {
  const [negocio, setNegocio] = useState(null);
  const [recursos, setRecursos] = useState([]); 
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!id) return;

    const fetchData = async () => {
      try {
        setLoading(true);
        const resNegocio = await api.get(`/negocios/publico/${id}`);
        const negocioData = resNegocio.data;
        setNegocio(negocioData);

        const resRecursos = await api.get(`/recursos/negocio/${negocioData.id}`);
        const recursosData = resRecursos.data;

        // Ojo de Arquitecto: Esto genera el problema N+1 (muchas peticiones al backend).
        // Para este proyecto está perfecto, pero tenelo en cuenta por si te lo preguntan en una entrevista técnica.
        const recursosConServicios = await Promise.all(
          recursosData.map(async (recurso) => {
            try {
              const resServicios = await api.get(`/servicios/recurso/${recurso.id}`);
              return { ...recurso, servicios: resServicios.data };
            } catch (err) {
              return { ...recurso, servicios: [] };
            }
          })
        );

        setRecursos(recursosConServicios);

      } catch (err) {
        setError(
          err.response?.data?.message || 
          'No pudimos encontrar este negocio o no está disponible.'
        );
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, [id]);

  return { negocio, recursos, loading, error };
};