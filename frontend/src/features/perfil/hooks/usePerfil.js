import { useState, useEffect, useContext, useCallback } from 'react';
import { AuthContext } from '../../../context/AuthContext';
import { UsuariosService } from '../../../services/UsuariosService';
import { useToast } from '../../../context/ToastContext';

export const usePerfil = () => {
  const { user, logout } = useContext(AuthContext);
  const { addToast } = useToast();
  const [perfil, setPerfil] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchPerfil = useCallback(async () => {
    if (!user?.email) return;
    try {
      setLoading(true);
      const data = await UsuariosService.obtenerPorEmail(user.email);
      setPerfil(data);
    } catch (error) { console.error(error); } finally { setLoading(false); }
  }, [user?.email]);

  useEffect(() => { fetchPerfil(); }, [fetchPerfil]);

  const actualizarPerfil = async (formData) => {
    setIsSubmitting(true);
    try {
      await UsuariosService.actualizarPerfil(formData);
      await fetchPerfil();
      return { success: true };
    } catch (error) {
      return { success: false, message: error.response?.data?.message || 'Error' };
    } finally { setIsSubmitting(false); }
  };

  const eliminarCuenta = async () => {
    if (!perfil?.id) return false;
    try {
      setIsSubmitting(true);
      await UsuariosService.eliminarUsuario(perfil.id);
      addToast('Cuenta eliminada exitosamente.', 'success');
      logout();
      return true;
    } catch (error) { 
      addToast(error.response?.data?.message || 'Error al eliminar la cuenta', 'error'); 
      setIsSubmitting(false); 
      return false; 
    }
  };

  return { perfil, loading, isSubmitting, actualizarPerfil, eliminarCuenta };
};