import { useState, useEffect, useCallback, useContext } from 'react';
import { AuthContext } from '../../../context/AuthContext';
import { RecursosService } from '../../../services/RecursosService';
import { ServiciosService } from '../../../services/ServiciosService';
import { mensajeError } from '../../../utils/errores';
import { esAsignacionActiva } from '../utils/asignaciones';

export const useGestionMiTrabajo = (recursoId) => {
  const { user } = useContext(AuthContext);
  const email = user?.email;
  const [datos, setDatos] = useState(null);

  const cargarRecurso = useCallback(async () => {
    const asignaciones = await RecursosService.obtenerMisAsignaciones();
    const recurso = asignaciones.find(asignacion => String(asignacion.id) === String(recursoId))
      || await RecursosService.obtenerPorId(recursoId);
    const esEmpleadoActivo = esAsignacionActiva(recurso, email);
    const esPropietario = recurso.propietario === true;
    const puedeAdministrar = esEmpleadoActivo || esPropietario;
    const servicios = puedeAdministrar && !esPropietario
      ? await ServiciosService.obtenerPorRecurso(recursoId).catch(() => [])
      : [];
    return { recursoId, email, recurso: { ...recurso, servicios }, puedeAdministrar, esPropietario };
  }, [recursoId, email]);

  useEffect(() => {
    if (!recursoId || !email) return;
    let vigente = true;
    cargarRecurso()
      .then(resultado => { if (vigente) setDatos(resultado); })
      .catch(error => {
        console.error(error);
        if (vigente) setDatos({ recursoId, email, recurso: null, puedeAdministrar: false });
      });
    return () => { vigente = false; };
  }, [cargarRecurso, recursoId, email]);

  const loading = !datos || datos.recursoId !== recursoId || datos.email !== email;
  const recurso = loading ? null : datos.recurso;
  const estadoAsociacion = recurso && recurso.emailUsuario === email ? recurso.estadoAsociacion : null;
  const puedeAdministrar = !loading && datos.puedeAdministrar;
  const esPropietario = !loading && datos.esPropietario;
  const fetchRecurso = async () => setDatos(await cargarRecurso());

  const guardarRecurso = async (formData) => {
    try {
      await RecursosService.actualizarRecurso(recursoId, formData);
      await fetchRecurso();
      return { success: true };
    } catch (error) { return { success: false, message: mensajeError(error, 'Error al guardar el recurso.') }; }
  };

  const guardarServicio = async (formData, servicioId) => {
    try {
      if (servicioId) await ServiciosService.actualizarServicio(servicioId, formData);
      else await ServiciosService.crearServicio({ ...formData, idRecurso: recursoId });
      await fetchRecurso();
      return { success: true };
    } catch (error) { return { success: false, message: mensajeError(error, 'Error al guardar el servicio.') }; }
  };

  const eliminarServicio = async servicioId => {
    try {
      await ServiciosService.eliminarServicio(servicioId);
      setDatos(prev => prev?.recursoId === recursoId ? {
        ...prev, recurso: { ...prev.recurso, servicios: prev.recurso.servicios.filter(servicio => servicio.id !== servicioId) },
      } : prev);
      return { success: true };
    } catch (error) {
      return { success: false, message: mensajeError(error, 'Error al eliminar el servicio.') };
    }
  };

  return { recurso, loading, estadoAsociacion, puedeAdministrar, esPropietario, guardarRecurso, guardarServicio, eliminarServicio };
};
