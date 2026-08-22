import { useState, useContext, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { AuthContext } from '../../../context/AuthContext';

const Home = () => {
  const [negocioId, setNegocioId] = useState('');
  const navigate = useNavigate();
  const { user } = useContext(AuthContext);

  // Si ya tiene sesión, pasa de largo al Dashboard
  useEffect(() => {
    if (user) {
      navigate('/dashboard', { replace: true });
    }
  }, [user, navigate]);

  const handleBuscarNegocio = (e) => {
    e.preventDefault();
    if (negocioId.trim()) {
      navigate(`/negocio/${negocioId.trim()}`);
    }
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-[calc(100vh-4rem)] bg-gradient-to-b from-gray-50 to-gray-100 px-4 sm:px-6 lg:px-8">
      
      {/* Sección Hero */}
      <div className="text-center max-w-3xl mb-12">
        <h1 className="text-4xl font-extrabold text-gray-900 sm:text-5xl md:text-6xl tracking-tight mb-6">
          Todos tus turnos y negocios en <span className="text-blue-600">un solo lugar</span>
        </h1>
        <p className="mt-3 max-w-md mx-auto text-lg text-gray-500 sm:text-xl md:mt-5 md:max-w-2xl">
          Buscá a tu profesional favorito para reservar tu próxima cita, o creá tu cuenta gratuita para empezar a ofrecer tus propios servicios.
        </p>
      </div>

      {/* Tarjetas de Acción */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8 w-full max-w-4xl">
        
        {/* Acción 1: Buscar para reservar (Cliente sin cuenta) */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-8 flex flex-col items-center text-center hover:shadow-md transition-shadow">
          <div className="bg-green-100 text-green-600 p-4 rounded-full mb-6">
            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
            </svg>
          </div>
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Quiero sacar un turno</h2>
          <p className="text-gray-600 mb-6 flex-1">
            ¿Tenés el código o enlace del local? Ingresalo acá para ver sus servicios y elegir tu horario.
          </p>
          
          <form onSubmit={handleBuscarNegocio} className="w-full flex flex-col gap-3">
            <input
              type="text"
              placeholder="Ej: mi-barberia o ID"
              value={negocioId}
              onChange={(e) => setNegocioId(e.target.value)}
              className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500"
              required
            />
            <button 
              type="submit"
              className="w-full bg-green-600 text-white font-semibold py-3 px-4 rounded-lg hover:bg-green-700 transition-colors"
            >
              Buscar negocio
            </button>
          </form>
        </div>

        {/* Acción 2: Crear cuenta unificada */}
        <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-8 flex flex-col items-center text-center hover:shadow-md transition-shadow">
          <div className="bg-blue-100 text-blue-600 p-4 rounded-full mb-6">
            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path>
            </svg>
          </div>
          <h2 className="text-2xl font-bold text-gray-900 mb-2">Sumate a la plataforma</h2>
          <p className="text-gray-600 mb-6 flex-1">
            Creá tu cuenta única. Podrás administrar los turnos que pediste o registrar tu propio local para recibir clientes.
          </p>
          <div className="flex flex-col w-full gap-3">
            <Link to="/register" className="w-full bg-blue-600 text-white font-semibold py-3 px-4 rounded-lg hover:bg-blue-700 transition-colors">
              Crear mi cuenta
            </Link>
            <Link to="/login" className="w-full bg-blue-50 text-blue-700 font-semibold py-3 px-4 rounded-lg hover:bg-blue-100 transition-colors">
              Ya tengo cuenta
            </Link>
          </div>
        </div>

      </div>
    </div>
  );
};

export default Home;