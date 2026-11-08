import { useState, useContext, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { AuthContext } from '../../../context/AuthContext';
import { Store, ChartPie, UserGroup, Bell, Search} from 'lucide-react';

const Home = () => {
  const [negocioId, setNegocioId] = useState('');
  const navigate = useNavigate();
  const { user } = useContext(AuthContext);

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
    <div className="flex flex-col min-h-[calc(100vh-4rem)] bg-white overflow-hidden pb-10">
      
      {/* 1. HERO SECTION BOXED */}
      <div className="max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 mt-6 sm:mt-10">
        <section className="bg-app-white rounded-[2.5rem] px-4 sm:px-6 py-20 lg:py-24 flex flex-col items-center relative">
          <div className="text-center max-w-4xl relative z-10">
            <span className="text-app-marineblue font-bold tracking-wider uppercase text-sm mb-4 block">
              Plataforma de Gestión Multitenant
            </span>
            <h1 className="text-5xl sm:text-6xl lg:text-7xl font-black text-app-black tracking-tight mb-8 leading-tight">
              Tus turnos y tu negocio en <span className="text-app-marineblue relative whitespace-nowrap">
                un solo lugar
                <svg className="absolute -bottom-2 left-0 w-full h-3 text-app-ligthblue" fill="currentColor" viewBox="0 0 100 10" preserveAspectRatio="none">
                  <polygon points="0,0 100,0 100,10" />
                </svg>
              </span>
            </h1>
            <p className="max-w-2xl mx-auto text-xl text-app-marineblue leading-relaxed mb-12">
              AgendApp centraliza tus reservas, automatiza recordatorios y mide tu rendimiento. 
              Modernizá la forma en que interactuás con tus clientes hoy mismo.
            </p>
          </div>

          {/* TARJETAS DE ACCIÓN - SUPERPUESTAS AL BORDE INFERIOR */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8 w-full max-w-5xl relative z-10 -mb-40">
            <div className="bg-white rounded-3xl shadow-xl shadow-app-marineblue/10 border border-gray-100 p-8 md:p-10 flex flex-col items-center text-center transform transition-transform hover:-translate-y-1">
              <div className="bg-app-marineblue/10 text-app-marineblue p-5 rounded-2xl mb-6">
                <Search className="h-10 w-10" />
              </div>
              <h2 className="text-2xl font-bold text-app-black mb-3">Soy Cliente</h2>
              <p className="text-app-marineblue mb-8 flex-1 text-lg">
                Buscá a tu profesional, elegí el servicio y reservá tu horario en segundos.
              </p>
              <form onSubmit={handleBuscarNegocio} className="w-full flex flex-col gap-4">
                <input
                  type="text"
                  aria-label="Alias del negocio"
                  placeholder="Ingresá el alias del negocio"
                  value={negocioId}
                  onChange={(e) => setNegocioId(e.target.value)}
                  className="w-full px-5 py-4 bg-gray-50 border-none rounded-xl focus:outline-none focus:ring-2 focus:ring-app-marineblue text-app-black font-medium"
                  required
                />
                <p className="text-sm text-app-marineblue">Usá el alias que aparece al final del enlace del negocio.</p>
                <button type="submit" className="w-full bg-app-marineblue text-white font-bold py-4 px-6 rounded-xl hover:bg-app-black/90 transition-colors shadow-md duration-300">
                  Buscar disponibilidad
                </button>
              </form>
            </div>

            <div className="bg-white rounded-3xl shadow-xl shadow-app-marineblue/10 border border-gray-100 p-8 md:p-10 flex flex-col items-center text-center transform transition-transform hover:-translate-y-1">
              <div className="bg-app-marineblue/10 text-app-marineblue p-5 rounded-2xl mb-6">
                <Store className="h-10 w-10" />
              </div>
              <h2 className="text-2xl font-bold text-app-black mb-3">Soy Profesional</h2>
              <p className="text-app-marineblue mb-8 flex-1 text-lg">
                Registrá tu local, gestioná a tu staff y dejá que la agenda se llene sola.
              </p>
              <div className="flex flex-col w-full gap-4">
                <Link to="/register" className="w-full bg-app-black/90 text-white font-bold py-4 px-6 rounded-xl hover:bg-app-marineblue transition-colors shadow-md duration-300">
                  Crear mi cuenta gratis
                </Link>
                <Link to="/login" className="w-full bg-white text-app-marineblue border-2 border-gray-200 font-bold py-4 px-6 rounded-xl hover:bg-app-black/10 transition-colors duration-300">
                  Ingresar a mi panel
                </Link>
              </div>
            </div>
          </div>
        </section>
      </div>

      <div className="h-40 bg-white"></div>

      {/* 2. BENEFICIOS / FEATURES */}
      <div className="max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 mt-16 mb-16">
        <div className="text-center mb-16">
          <h2 className="text-3xl sm:text-4xl font-black text-app-black mb-4">Todo lo que necesitás para crecer</h2>
          <p className="text-xl text-app-marineblue max-w-2xl mx-auto">Nuestra arquitectura te garantiza una plataforma rápida, segura y siempre disponible.</p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          <div className="p-8 rounded-4xl bg-gray-50 border border-gray-100 hover:border-app-marineblue transition-colors">
            <div className="w-14 h-14 bg-white rounded-xl shadow-sm flex items-center justify-center text-2xl mb-6 border border-gray-100"><Bell className="h-6 w-6" /></div>
            <h3 className="text-xl font-bold text-app-black mb-3 text-center">Notificaciones al instante</h3>
            <p className="text-app-marineblue leading-relaxed">Reducí las inasistencias drásticamente. Cada reserva y cancelación dispara alertas automáticas para mantener a tus clientes informados.</p>
          </div>
          <div className="p-8 rounded-4xl bg-gray-50 border border-gray-100 hover:border-app-marineblue transition-colors">
            <div className="w-14 h-14 bg-white rounded-xl shadow-sm flex items-center justify-center text-2xl mb-6 border border-gray-100"><UserGroup className="h-6 w-6" /></div>
            <h3 className="text-xl font-bold text-app-black mb-3 text-center">Staff y Espacios</h3>
            <p className="text-app-marineblue leading-relaxed">Administrá múltiples agendas simultáneas. Asigná horarios, descansos y servicios personalizados a cada miembro de tu equipo.</p>
          </div>
          <div className="p-8 rounded-4xl bg-gray-50 border border-gray-100 hover:border-app-marineblue transition-colors">
            <div className="w-14 h-14 bg-white rounded-xl shadow-sm flex items-center justify-center text-2xl mb-6 border border-gray-100"><ChartPie className="h-6 w-6" /></div>
            <h3 className="text-xl font-bold text-app-black mb-3 text-center">Métricas Detalladas</h3>
            <p className="text-app-marineblue leading-relaxed">Tomá decisiones basadas en datos. Visualizá tus ingresos reales, tasa de cancelaciones y descubrí cuáles son tus servicios más rentables.</p>
          </div>
        </div>
      </div>

      {/* 3. CASOS DE USO BOXED */}
      <div className="max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8 mb-20">
        <section className="bg-app-black text-white rounded-[2.5rem] p-10 sm:p-16 lg:p-20 flex flex-col lg:flex-row items-center gap-16">
          <div className="flex-1">
            <h2 className="text-3xl sm:text-4xl font-black mb-6 leading-tight">Se adapta a tu ritmo de trabajo</h2>
            <p className="text-lg text-gray-400 mb-8 leading-relaxed">
              No importa si trabajás solo o si gestionás un equipo grande. AgendApp está diseñado con una infraestructura sólida que escala con tu negocio.
            </p>
            <ul className="space-y-4">
              <li className="flex items-center gap-4 text-xl font-medium text-gray-200">
                <span className="text-app-ligthblue">✓</span> Barberías y Peluquerías
              </li>
              <li className="flex items-center gap-4 text-xl font-medium text-gray-200">
                <span className="text-app-ligthblue">✓</span> Consultorios Médicos
              </li>
              <li className="flex items-center gap-4 text-xl font-medium text-gray-200">
                <span className="text-app-ligthblue">✓</span> Centros de Estética
              </li>
              <li className="flex items-center gap-4 text-xl font-medium text-gray-200">
                <span className="text-app-ligthblue">✓</span> Canchas y Espacios Deportivos
              </li>
            </ul>
          </div>
          <div className="flex-1 w-full bg-linear-to-tr from-app-marineblue to-app-marineblue rounded-3xl p-1 shadow-2xl">
            <div className="bg-app-black rounded-[22px] p-8 h-full flex flex-col justify-center border border-gray-800">
              <div className="flex justify-between items-center mb-8 pb-6 border-b border-gray-800">
                <div>
                  <h4 className="text-xl font-bold">Corte Clásico + Barba</h4>
                  <p className="text-gray-400 mt-1">Duración: 45 min</p>
                </div>
                <span className="text-2xl font-black text-app-marineblue">$8.500</span>
              </div>
              <div className="flex justify-between items-center mb-8 pb-6 border-b border-gray-800">
                <div>
                  <h4 className="text-xl font-bold">Limpieza Facial</h4>
                  <p className="text-gray-400 mt-1">Duración: 60 min</p>
                </div>
                <span className="text-2xl font-black text-app-marineblue">$12.000</span>
              </div>
              <div className="w-full bg-app-marineblue text-app-black font-bold py-4 rounded-xl text-center shadow-md">
                Visualización de tus clientes
              </div>
            </div>
          </div>
        </section>
      </div>

      {/* 4. FOOTER BOXED Y SUTIL */}
      <div className="max-w-7xl mx-auto w-full px-4 sm:px-6 lg:px-8">
        <footer className="bg-gray-50 rounded-3xl py-12 text-center border border-gray-100">
          <div className="flex flex-col items-center">
            <span className="text-2xl font-black text-app-marineblue mb-3">AgendApp</span>
            <p className="text-app-marineblue font-medium">Llevá la gestión de tus turnos al siguiente nivel.</p>
            <div className="mt-8 text-sm text-gray-400">
              &copy; {new Date().getFullYear()} AgendApp. Todos los derechos reservados.
            </div>
          </div>
        </footer>
      </div>

    </div>
  );
};

export default Home;
