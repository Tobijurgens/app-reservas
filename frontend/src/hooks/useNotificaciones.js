import { useState, useEffect, useCallback, useContext } from 'react';
import { NotificacionesService } from '../services/NotificacionesService';
import { AuthContext } from '../context/AuthContext';

export const useNotificaciones = () => {
  const { user } = useContext(AuthContext);
  const [notificaciones, setNotificaciones] = useState([]);
  const [noLeidas, setNoLeidas] = useState(0);
  const [isNotifOpen, setIsNotifOpen] = useState(false);

  const fetchNotificaciones = useCallback(async () => {
    if (!user?.email) return;
    try {
      const data = await NotificacionesService.obtenerMisNotificaciones(user.email);
      setNotificaciones(data);
      setNoLeidas(data.filter(n => !n.leida).length);
    } catch (error) {
      console.error('Error al traer notificaciones', error);
    }
  }, [user?.email]);

  useEffect(() => {
  fetchNotificaciones();

  const intervalId = setInterval(() => {
    if (document.visibilityState === 'visible') {
      fetchNotificaciones();
    }
  }, 60000);

  const handleVisibilityChange = () => {
    if (document.visibilityState === 'visible') {
      fetchNotificaciones();
    }
  };
  
  document.addEventListener("visibilitychange", handleVisibilityChange);

  return () => {
    clearInterval(intervalId);
    document.removeEventListener("visibilitychange", handleVisibilityChange);
  };
}, [fetchNotificaciones]);

  const marcarLeida = async (id, yaEstaLeida) => {
    if (yaEstaLeida || !user?.email) return;
    try {
      await NotificacionesService.marcarComoLeida(id, user.email);
      setNotificaciones(prev => prev.map(n => n.id === id ? { ...n, leida: true } : n));
      setNoLeidas(prev => Math.max(0, prev - 1));
    } catch (error) {
      console.error(error);
    }
  };

  const marcarTodas = async () => {
    if (!user?.email) return;
    try {
      await NotificacionesService.marcarTodasComoLeidas(user.email);
      setNotificaciones(prev => prev.map(n => ({ ...n, leida: true })));
      setNoLeidas(0);
    } catch (error) {
      console.error(error);
    }
  };

  return { 
    notificaciones, 
    noLeidas, 
    isNotifOpen, 
    setIsNotifOpen, 
    marcarLeida, 
    marcarTodas,
    fetchNotificaciones
  };
};