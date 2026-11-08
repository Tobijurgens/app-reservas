import { useState, useContext } from 'react';
import { AuthContext } from '../../../context/AuthContext';
import { useMisAsignaciones } from './useMisAsignaciones';
import { useAgendaRecurso } from './useAgendaRecurso';
import { esAsignacionActiva } from '../utils/asignaciones';

export const useAgendaPersonal = () => {
  const { user } = useContext(AuthContext);
  const { asignaciones, loading: loadingAsignaciones, error: errorAsignaciones, reintentar: recargarAsignaciones } = useMisAsignaciones();
  const [seleccion, setSeleccion] = useState(null);
  const puedeConsultar = recurso => recurso.propietario === true || esAsignacionActiva(recurso, user?.email);
  const recursosAsignados = asignaciones.filter(puedeConsultar);
  const asignacionesNoActivas = asignaciones.filter(recurso => !puedeConsultar(recurso));
  const recursoSeleccionado = !loadingAsignaciones && !errorAsignaciones && recursosAsignados.some(recurso => recurso.id === seleccion)
    ? seleccion : null;
  const agenda = useAgendaRecurso(recursoSeleccionado);

  return {
    recursosAsignados, asignacionesNoActivas, recursoSeleccionado, setRecursoSeleccionado: setSeleccion,
    turnos: agenda.turnos, loading: loadingAsignaciones || agenda.loading,
    error: errorAsignaciones || agenda.error,
    reintentar: errorAsignaciones ? recargarAsignaciones : agenda.reintentar,
    cancelarTurno: agenda.cancelarTurno,
  };
};
