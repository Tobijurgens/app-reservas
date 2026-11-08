import { useState, useEffect } from 'react';
import { NegociosService } from '../../../services/NegociosService';
import { RecursosService } from '../../../services/RecursosService';
import { ServiciosService } from '../../../services/ServiciosService';
import { mensajeError } from '../../../utils/errores';

export const useNegocioPublico = id => {
  const [resultado, setResultado] = useState(null);
  const [intento, setIntento] = useState(0);

  useEffect(() => {
    if (!id) return;
    let vigente = true;
    const cargar = async () => {
      try {
        const negocio = await NegociosService.obtenerPorAlias(id);
        const recursos = await RecursosService.obtenerPorNegocio(negocio.id);
        const completos = await Promise.all(recursos.map(async recurso => ({
          ...recurso, servicios: await ServiciosService.obtenerPorRecurso(recurso.id),
        })));
        if (vigente) setResultado({ id, intento, negocio, recursos: completos, error: null });
      } catch (error) {
        if (vigente) setResultado({
          id, intento, negocio: null, recursos: [],
          error: mensajeError(error, 'No pudimos cargar este negocio. Revisá el alias y tu conexión.'),
        });
      }
    };
    cargar();
    return () => { vigente = false; };
  }, [id, intento]);

  const loading = Boolean(id) && (resultado?.id !== id || resultado?.intento !== intento);
  const actual = !loading && resultado?.id === id ? resultado : null;
  return {
    negocio: actual?.negocio || null, recursos: actual?.recursos || [], loading,
    error: actual?.error || null, reintentar: () => setIntento(valor => valor + 1),
  };
};
