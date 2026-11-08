import { useState, useEffect } from 'react';
import { TurnosService } from '../../../services/TurnosService';
import { useToast } from '../../../context/ToastContext';
import { mensajeError } from '../../../utils/errores';

export const useAgendaRecurso = recursoId => {
  const [resultado, setResultado] = useState(null);
  const [intento, setIntento] = useState(0);
  const { addToast } = useToast();

  useEffect(() => {
    if (!recursoId) return;
    let vigente = true;
    TurnosService.obtenerAgendaRecurso(recursoId)
      .then(turnos => { if (vigente) setResultado({ recursoId, intento, turnos, error: null }); })
      .catch(error => {
        if (vigente) setResultado({ recursoId, intento, turnos: [], error: mensajeError(error, 'No pudimos cargar esta agenda. Revisá tu conexión o tus permisos.') });
      });
    return () => { vigente = false; };
  }, [recursoId, intento]);

  const loading = Boolean(recursoId) && (resultado?.recursoId !== recursoId || resultado?.intento !== intento);
  const actual = !loading && resultado?.recursoId === recursoId ? resultado : null;

  const cancelarTurno = async idTurno => {
    try {
      await TurnosService.cancelarTurno(idTurno);
      setResultado(prev => prev?.recursoId === recursoId ? {
        ...prev, turnos: prev.turnos.map(turno => turno.id === idTurno ? { ...turno, estado: 'CANCELADO' } : turno),
      } : prev);
      return true;
    } catch (error) {
      addToast(mensajeError(error, 'Error al cancelar el turno.'), 'error');
      return false;
    }
  };

  return {
    turnos: actual?.turnos || [], loading, error: actual?.error || null,
    reintentar: () => setIntento(valor => valor + 1), cancelarTurno,
  };
};
