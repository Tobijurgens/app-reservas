import { BrowserRouter, Routes, Route } from 'react-router-dom';
import ProtectedRoute from '../components/layout/ProtectedRoute';
import Navbar from '../components/layout/Navbar';

// Páginas Públicas
import Home from '../features/publico/pages/Home';
import NegocioPerfil from '../features/publico/pages/NegocioPerfil';

// Páginas de Auth
import Login from '../features/auth/pages/Login';
import Register from '../features/auth/pages/Register';

// Páginas Privadas (Features)
import Dashboard from '../features/negocios/pages/Dashboard';
import GestionNegocio from '../features/negocios/pages/GestionNegocio';
import AgendaPersonal from '../features/negocios/pages/AgendaPersonal';
import GestionMiTrabajo from '../features/negocios/pages/GestionMiTrabajo';
import PerfilUsuario from '../features/perfil/pages/PerfilUsuario'; // <-- NUEVO IMPORT

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
          
          {/* Rutas Privadas */}
          <Route element={<ProtectedRoute />}>
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/dashboard/negocio/:id" element={<GestionNegocio />} />
            <Route path="/dashboard/mi-trabajo/:id" element={<GestionMiTrabajo />} />
            <Route path="/mi-agenda" element={<AgendaPersonal />} />
            
            {/* NUEVA RUTA PROTEGIDA: Perfil del Usuario */}
            <Route path="/perfil" element={<PerfilUsuario />} />
          </Route>
        </Routes>
      </main>
    </BrowserRouter>
  );
};

export default AppRouter;