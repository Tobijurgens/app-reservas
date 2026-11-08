import { useState, useEffect } from 'react';
import { TurnosService } from '../../../services/TurnosService';
import { mensajeError } from '../../../utils/errores';

export const useReportesNegocio = (negocioId, fechaInicio, fechaFin, recursoId) => {
  const [resultado, setResultado] = useState(null);
  const [intento, setIntento] = useState(0);
  const rangoValido = Boolean(negocioId && fechaInicio && fechaFin && fechaInicio <= fechaFin);
  const clave = JSON.stringify([negocioId, fechaInicio, fechaFin, recursoId, intento]);

  useEffect(() => {
    if (!rangoValido) return;
    let vigente = true;
    const filtroRecurso = recursoId !== 'todos' ? recursoId : null;
    TurnosService.obtenerReportes(negocioId, `${fechaInicio}T00:00:00`, `${fechaFin}T23:59:59`, filtroRecurso)
      .then(reporte => { if (vigente) setResultado({ clave, reporte, error: null }); })
      .catch(error => {
        if (vigente) setResultado({ clave, reporte: null, error: mensajeError(error, 'No pudimos cargar el reporte. Revisá tu conexión.') });
      });
    return () => { vigente = false; };
  }, [negocioId, fechaInicio, fechaFin, recursoId, rangoValido, clave]);

  const loading = rangoValido && resultado?.clave !== clave;
  const errorRango = !fechaInicio || !fechaFin ? 'Completá ambas fechas.'
    : fechaInicio > fechaFin ? 'La fecha de inicio no puede ser posterior a la fecha de fin.' : null;
  return {
    reporte: rangoValido && !loading ? resultado?.reporte || null : null,
    loading, error: errorRango || (!loading && rangoValido ? resultado?.error : null),
    reintentar: () => setIntento(valor => valor + 1),
  };
};
