import { useContext } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';

const ProtectedRoute = () => {
  const { user, loading } = useContext(AuthContext);

  // Mientras verifica el token en localStorage, mostramos un loading
  if (loading) {
    return <div className="flex h-screen items-center justify-center">Cargando...</div>;
  }

  // Si no hay usuario logueado, lo redirigimos al login
  if (!user) {
    return <Navigate to="/login" replace />;
  }

  // Si está logueado, renderizamos la ruta hija (Outlet)
  return <Outlet />;
};

export default ProtectedRoute;