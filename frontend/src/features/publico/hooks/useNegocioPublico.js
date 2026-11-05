import { useState, useEffect } from 'react';
import { NegociosService } from '../../../services/NegociosService';
import { RecursosService } from '../../../services/RecursosService';
import { ServiciosService } from '../../../services/ServiciosService';

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
        const negocioData = await NegociosService.obtenerPorAlias(id);
        setNegocio(negocioData);

        const recursosData = await RecursosService.obtenerPorNegocio(negocioData.id);
        
        const recursosConServicios = await Promise.all(
          recursosData.map(async (recurso) => {
            try {
              const servicios = await ServiciosService.obtenerPorRecurso(recurso.id);
              return { ...recurso, servicios };
            } catch (err) {
              return { ...recurso, servicios: [] };
            }
          })
        );
        setRecursos(recursosConServicios);
      } catch (err) {
        setError(err.response?.data?.message || 'No pudimos encontrar este negocio o no está disponible.');
      } finally { setLoading(false); }
    };

    fetchData();
  }, [id]);

  return { negocio, recursos, loading, error };
};