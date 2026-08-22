import { useState, useEffect, useContext, useCallback } from 'react';
import api from '../../../services/api';
import { AuthContext } from '../../../context/AuthContext';

export const usePerfil = () => {
  const { user, logout } = useContext(AuthContext);
  const [perfil, setPerfil] = useState(null);
  const [loading, setLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const fetchPerfil = useCallback(async () => {
    if (!user?.email) return;
    try {
      setLoading(true);
      // Usamos tu endpoint por email
      const response = await api.get(`/usuarios/email/${user.email}`);
      setPerfil(response.data);
    } catch (error) {
      console.error('Error al cargar el perfil:', error);
    } finally {
      setLoading(false);
    }
  }, [user?.email]);

  useEffect(() => {
    fetchPerfil();
  }, [fetchPerfil]);

  const actualizarPerfil = async (formData) => {
    setIsSubmitting(true);
    try {
      // El backend recibe ActualizarPerfilRequest (nombre, apellido, telefono)
      await api.put('/usuarios/perfil', formData);
      await fetchPerfil(); // Recargamos para tener los datos frescos
      return { success: true };
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.message || 'Error al actualizar el perfil' 
      };
    } finally {
      setIsSubmitting(false);
    }
  };

  const eliminarCuenta = async () => {
    if (!perfil?.id) return false;
    
    try {
      setIsSubmitting(true);
      await api.delete(`/usuarios/${perfil.id}`);
      logout(); // Si se elimina, lo deslogueamos automáticamente
      return true;
    } catch (error) {
      alert(error.response?.data?.message || 'Error al eliminar la cuenta');
      setIsSubmitting(false);
      return false;
    }
  };

  return { perfil, loading, isSubmitting, actualizarPerfil, eliminarCuenta };
};