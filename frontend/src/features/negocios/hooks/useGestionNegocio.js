import { useState, useEffect, useCallback, useContext } from 'react';
import { AuthContext } from '../../../context/AuthContext';
import { NegociosService } from '../../../services/NegociosService';
import { RecursosService } from '../../../services/RecursosService';
import { ServiciosService } from '../../../services/ServiciosService';
import { esAsignacionActiva, obtenerAsignacion } from '../utils/asignaciones';

export const useGestionNegocio = (idNegocio) => {
  const { user } = useContext(AuthContext);
  const email = user?.email;
  const [datos, setDatos] = useState(null);

  const cargarDatos = useCallback(async () => {
    let negocio;
    try {
      negocio = await NegociosService.obtenerPorId(idNegocio);
    } catch (error) {
      if (error.response?.status !== 403) throw error;
      const asignaciones = await RecursosService.obtenerMisAsignaciones();
      const propiasDelNegocio = asignaciones.filter(recurso => String(recurso.negocioId) === String(idNegocio));
      return { idNegocio, email, negocio: null, puedeAdministrar: false, miAsignacion: obtenerAsignacion(propiasDelNegocio, email) };
    }
    const recursosTotales = await RecursosService.obtenerTodosPorNegocio(idNegocio);
    const esPropietario = negocio.propietario === true;
    const miAsignacion = obtenerAsignacion(recursosTotales, email);
    const puedeAdministrar = esPropietario || esAsignacionActiva(miAsignacion, email);
    // El listado completo ya incluye el estado y los recursos activos.
    const recursosPermitidos = puedeAdministrar ? recursosTotales.filter(recurso => (
      recurso.activo === true && (esPropietario || recurso.id === miAsignacion.id)
    )) : [];
    const recursosActivos = await Promise.all(recursosPermitidos.map(async recurso => ({
      ...recurso,
      servicios: await ServiciosService.obtenerPorRecurso(recurso.id).catch(() => [])
    })));
    return { idNegocio, email, negocio, recursosTotales, recursosActivos, miAsignacion, puedeAdministrar };
  }, [idNegocio, email]);

  useEffect(() => {
    if (!idNegocio || !email) return;
    let vigente = true;
    cargarDatos()
      .then(resultado => { if (vigente) setDatos(resultado); })
      .catch(error => {
        console.error('Error al cargar datos:', error);
        if (vigente) setDatos({ idNegocio, email, negocio: null, puedeAdministrar: false });
      });
    return () => { vigente = false; };
  }, [cargarDatos, idNegocio, email]);

  const loading = !datos || datos.idNegocio !== idNegocio || datos.email !== email;
  const negocio = loading ? null : datos.negocio;
  const recursosTotales = loading ? [] : datos.recursosTotales || [];
  const recursosActivos = loading ? [] : datos.recursosActivos || [];
  const miAsignacion = loading ? null : datos.miAsignacion;
  const puedeAdministrar = !loading && datos.puedeAdministrar;
  const fetchData = async () => setDatos(await cargarDatos());

  const guardarRecurso = async (formData, editingId) => {
    try {
      const payload = { ...formData, idNegocio: Number(idNegocio), emailUsuario: formData.emailUsuario?.trim() || null };
      if (editingId) await RecursosService.actualizarRecurso(editingId, payload);
      else await RecursosService.crearRecurso(payload);
      
      await fetchData();
      return { success: true };
    } catch (err) {
      return { success: false, message: err.response?.data?.message || 'Error al guardar.' };
    }
  };

  const eliminarRecurso = async (id) => {
    try {
      await RecursosService.eliminarRecurso(id);
      await fetchData();
      return { success: true };
    } catch (err) { return { success: false, message: err.response?.data?.message || 'Error al eliminar' }; }
  };

  const guardarServicio = async (formData, editingId, idRecurso) => {
    try {
      const payload = { ...formData, precio: Number(formData.precio), duracionMinutos: Number(formData.duracionMinutos), idRecurso };
      if (editingId) await ServiciosService.actualizarServicio(editingId, payload);
      else await ServiciosService.crearServicio(payload);
      
      await fetchData();
      return { success: true };
    } catch (err) { return { success: false, message: err.response?.data?.message || 'Error.' }; }
  };

  const eliminarServicio = async (id) => {
    try {
      await ServiciosService.eliminarServicio(id);
      await fetchData();
      return { success: true };
    } catch (err) { return { success: false, message: err.response?.data?.message || 'Error al eliminar' }; }
  };

  return { negocio, recursosTotales, recursosActivos, miAsignacion, puedeAdministrar, loading, guardarRecurso, eliminarRecurso, guardarServicio, eliminarServicio };
};
