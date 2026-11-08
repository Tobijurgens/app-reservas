import { useState, useEffect, useCallback, useContext, useRef } from 'react';
import { NotificacionesService } from '../services/NotificacionesService';
import { AuthContext } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { mensajeError } from '../utils/errores';

export const useNotificaciones = () => {
  const { user } = useContext(AuthContext);
  const { addToast } = useToast();
  const token = user?.token;
  const [datos, setDatos] = useState(null);
  const [isNotifOpen, setIsNotifOpen] = useState(false);
  const secuencia = useRef({ valor: 0 });
  const lecturasEnCurso = useRef(new Set());
  const todasEnCurso = useRef(new Set());
  const notificaciones = datos && datos.token === token ? datos.notificaciones : [];
  const noLeidas = notificaciones.filter(notificacion => !notificacion.leida).length;

  const fetchNotificaciones = useCallback(() => {
    if (!token || todasEnCurso.current.has(token)
      || [...lecturasEnCurso.current].some(clave => clave.startsWith(`${token}:`))) return;
    const solicitud = ++secuencia.current.valor;
    return NotificacionesService.obtenerMisNotificaciones()
      .then(notificaciones => {
        if (solicitud === secuencia.current.valor && localStorage.getItem('token') === token) {
          setDatos({ token, notificaciones });
        }
      })
      .catch(() => {
        // Nunca registrar el objeto Axios: puede contener Authorization.
        console.error('No se pudieron actualizar las notificaciones.');
      });
  }, [token]);

  useEffect(() => {
    const control = secuencia.current;
    fetchNotificaciones();
    const refrescarSiVisible = () => {
      if (document.visibilityState === 'visible') fetchNotificaciones();
    };
    const intervalId = setInterval(refrescarSiVisible, 60000);
    document.addEventListener('visibilitychange', refrescarSiVisible);
    return () => {
      ++control.valor;
      clearInterval(intervalId);
      document.removeEventListener('visibilitychange', refrescarSiVisible);
    };
  }, [fetchNotificaciones]);

  const marcarLeida = async (id, yaEstaLeida) => {
    const clave = `${token}:${id}`;
    if (yaEstaLeida || !token || lecturasEnCurso.current.has(clave) || todasEnCurso.current.has(token)) return;
    lecturasEnCurso.current.add(clave);
    ++secuencia.current.valor;
    try {
      await NotificacionesService.marcarComoLeida(id);
      if (localStorage.getItem('token') !== token) return;
      setDatos(prev => prev?.token === token ? {
        ...prev, notificaciones: prev.notificaciones.map(notificacion => notificacion.id === id ? { ...notificacion, leida: true } : notificacion),
      } : prev);
    } catch (error) {
      if (localStorage.getItem('token') === token) addToast(mensajeError(error, 'No pudimos marcar la notificación como leída.'), 'error');
    } finally {
      lecturasEnCurso.current.delete(clave);
    }
  };

  const marcarTodas = async () => {
    if (!token || todasEnCurso.current.has(token)) return;
    todasEnCurso.current.add(token);
    ++secuencia.current.valor;
    try {
      await NotificacionesService.marcarTodasComoLeidas();
      if (localStorage.getItem('token') !== token) return;
      setDatos(prev => prev?.token === token ? {
        ...prev, notificaciones: prev.notificaciones.map(notificacion => ({ ...notificacion, leida: true })),
      } : prev);
    } catch (error) {
      if (localStorage.getItem('token') === token) addToast(mensajeError(error, 'No pudimos marcar las notificaciones como leídas.'), 'error');
    } finally {
      todasEnCurso.current.delete(token);
    }
  };

  return { notificaciones, noLeidas, isNotifOpen, setIsNotifOpen, marcarLeida, marcarTodas, fetchNotificaciones };
};
