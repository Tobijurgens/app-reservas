import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import ProtectedRoute from '../components/layout/ProtectedRoute';
import Navbar from '../components/layout/Navbar';

// Páginas Públicas
import Home from '../features/publico/pages/Home';
import NegocioPerfil from '../features/publico/pages/NegocioPerfil';

// Páginas de Auth
import Login from '../features/auth/pages/Login';
import Register from '../features/auth/pages/Register';
import VerificarCuenta from '../features/auth/pages/VerificarCuenta';
import ProcesarInvitacion from '../features/auth/pages/ProcesarInvitacion';

// Páginas Privadas (Features)
import Dashboard from '../features/negocios/pages/Dashboard';
import GestionNegocio from '../features/negocios/pages/GestionNegocio';
import AgendaPersonal from '../features/negocios/pages/AgendaPersonal';
import GestionMiTrabajo from '../features/negocios/pages/GestionMiTrabajo';
import PerfilUsuario from '../features/perfil/pages/PerfilUsuario';

const AppRouter = () => {
  return (
    <BrowserRouter>
      <Navbar />
      <main className="min-h-[calc(100vh-4rem)] bg-gray-50">
        <Routes>
          {/* Rutas Públicas */}
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/negocio/:id" element={<NegocioPerfil />} />
          <Route path="/verificar" element={<VerificarCuenta />} />
          <Route path="/invitacion/:accion" element={<ProcesarInvitacion />} />
          
          {/* Rutas Privadas */}
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/dashboard/negocio/:id" element={<GestionNegocio />} />
            <Route path="/dashboard/mi-trabajo/:id" element={<GestionMiTrabajo />} />
            <Route path="/mi-agenda" element={<AgendaPersonal />} />
            <Route path="/perfil" element={<PerfilUsuario />} />
          </Route>
          <Route path="*" element={
            <div className="mx-auto max-w-xl px-4 py-16 text-center">
              <h1 className="text-2xl font-bold text-app-black">Página no encontrada</h1>
              <p className="mt-3 text-sm text-gray-600">El enlace no existe o la dirección no es correcta.</p>
              <Link to="/" className="mt-6 inline-block rounded-xl bg-app-marineblue px-6 py-3 font-semibold text-white">Volver al inicio</Link>
            </div>
          } />
        </Routes>
      </main>
    </BrowserRouter>
  );
};

export default AppRouter;
