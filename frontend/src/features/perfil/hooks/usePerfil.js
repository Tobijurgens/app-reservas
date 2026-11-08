import { useState, useEffect, useContext } from 'react';
import { AuthContext } from '../../../context/AuthContext';
import { UsuariosService } from '../../../services/UsuariosService';
import { useToast } from '../../../context/ToastContext';
import { mensajeError } from '../../../utils/errores';

export const usePerfil = () => {
  const { user, logout } = useContext(AuthContext);
  const { addToast } = useToast();
  const [datos, setDatos] = useState(null);
  const [intento, setIntento] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const token = user?.token;

  useEffect(() => {
    if (!token) return;
    let vigente = true;
    UsuariosService.obtenerMiPerfil()
      .then(perfil => { if (vigente) setDatos({ token, intento, perfil, error: null }); })
      .catch(error => {
        if (vigente) setDatos({ token, intento, perfil: null, error: mensajeError(error, 'No pudimos cargar tu perfil. Revisá tu conexión.') });
      });
    return () => { vigente = false; };
  }, [token, intento]);

  const loading = Boolean(token) && (datos?.token !== token || datos?.intento !== intento);
  const perfil = !loading && datos && datos.token === token ? datos.perfil : null;
  const error = !loading && datos && datos.token === token ? datos.error : null;
  const reintentar = () => setIntento(valor => valor + 1);

  const actualizarPerfil = async formData => {
    setIsSubmitting(true);
    try {
      const actualizado = await UsuariosService.actualizarPerfil(formData);
      setDatos({ token, intento, perfil: actualizado, error: null });
      return { success: true, perfil: actualizado };
    } catch (error) {
      return { success: false, message: mensajeError(error, 'Error al actualizar el perfil.') };
    } finally { setIsSubmitting(false); }
  };

  const eliminarCuenta = async () => {
    if (!perfil?.id) return false;
    setIsSubmitting(true);
    try {
      await UsuariosService.eliminarUsuario(perfil.id);
      addToast('Cuenta eliminada exitosamente.', 'success');
      logout();
      return true;
    } catch (error) {
      addToast(mensajeError(error, 'Error al eliminar la cuenta.'), 'error');
      return false;
    } finally { setIsSubmitting(false); }
  };

  return { perfil, loading, error, reintentar, isSubmitting, actualizarPerfil, eliminarCuenta };
};
