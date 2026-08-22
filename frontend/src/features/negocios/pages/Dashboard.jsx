import { useState, useEffect } from 'react';
import { useLocation, Link, useNavigate } from 'react-router-dom';
import { useMisNegocios } from '../hooks/useMisNegocios';
import { useMisReservas } from '../hooks/useMisReservas';
import { esFuturo, esPasado } from '../../../utils/fechas';
import NegocioCard from '../components/NegocioCard';
import NegocioForm from '../components/NegocioForm';
import MisTrabajosList from '../components/MisTrabajosList';

const Dashboard = () => {
  const location = useLocation();
  const navigate = useNavigate();
  
  // 1. Inicializamos la pestaña leyendo el estado que envía el Navbar, 
  // o 'reservas' por defecto si el usuario entra directo a /dashboard
  const [activeTab, setActiveTab] = useState(location.state?.tab || 'reservas');
  
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingNegocio, setEditingNegocio] = useState(null);
  const [filtroReservas, setFiltroReservas] = useState('proximos');

  const { misNegocios, isLoading: loadingNegocios, isSubmitting, eliminarNegocio, guardarNegocio } = useMisNegocios();
  const { reservas, loading: loadingReservas, cancelarReserva } = useMisReservas();

  // 2. EL TRUCO PARA EL NAVBAR: Escuchamos si cambia la navegación proveniente del menú desplegable.
  // Reemplazamos el location.state para evitar que un F5 nos regrese a una pestaña que no queremos.
  useEffect(() => {
    if (location.state?.tab && location.state.tab !== activeTab) {
      setActiveTab(location.state.tab);
      // Limpiamos el estado de la ruta para no quedar "atrapados" en esa pestaña si recargamos la página
      navigate(location.pathname, { replace: true, state: {} });
    }
  }, [location, activeTab, navigate]);

  const handleAbrirCrear = () => {
    setEditingNegocio(null);
    setIsFormOpen(true);
  };

  const handleSubmitForm = async (formData) => {
    const res = await guardarNegocio(formData, editingNegocio?.id);
    if (res.success) setIsFormOpen(false);
    else alert(res.message);
  };

  const turnosProximos = reservas.filter(t => esFuturo(t.fechaHoraInicio) && t.estado !== 'CANCELADO');
  const turnosHistorial = reservas.filter(t => esPasado(t.fechaHoraInicio) || t.estado === 'CANCELADO');
  const turnosAMostrar = filtroReservas === 'proximos' ? turnosProximos : turnosHistorial;

  const getTabClass = (tabName) => `whitespace-nowrap pb-4 px-1 border-b-2 font-medium text-sm transition-colors ${
    activeTab === tabName ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'
  }`;

  const handleTabChange = (tabName) => {
    setActiveTab(tabName);
    setIsFormOpen(false);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
      <header className="mb-6 sm:mb-8">
        <h1 className="text-2xl sm:text-3xl font-bold text-gray-900">Mi Panel</h1>
        <p className="text-sm sm:text-base text-gray-600 mt-1">Gestioná tus turnos, trabajos y negocios desde acá.</p>
      </header>

      <nav className="border-b border-gray-200 mb-6 sm:mb-8 flex space-x-6 overflow-x-auto">
        <button onClick={() => handleTabChange('reservas')} className={getTabClass('reservas')}>Mis Reservas</button>
        <button onClick={() => handleTabChange('trabajos')} className={getTabClass('trabajos')}>Mis Trabajos</button>
        <button onClick={() => handleTabChange('negocios')} className={getTabClass('negocios')}>Mis Negocios</button>
      </nav>

      <main className="bg-white rounded-xl shadow-sm border border-gray-100 p-4 sm:p-6 min-h-[400px]">
        
        {/* PESTAÑA: MIS RESERVAS (CLIENTE) */}
        {activeTab === 'reservas' && (
          <div className="animate-fadeIn">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
              <h2 className="text-xl font-bold text-gray-800">
                {filtroReservas === 'proximos' ? 'Tus Próximos Turnos' : 'Historial de Turnos'}
              </h2>
              
              <select 
                value={filtroReservas}
                onChange={(e) => setFiltroReservas(e.target.value)}
                className="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block px-4 py-2 cursor-pointer font-medium"
              >
                <option value="proximos">Turnos Próximos</option>
                <option value="historial">Historial (Pasados/Cancelados)</option>
              </select>
            </div>
            
            {loadingReservas ? (
              <div className="flex justify-center py-12"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div></div>
            ) : turnosAMostrar.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-10">
                {turnosAMostrar.map(turno => {
                  const esCancelado = turno.estado === 'CANCELADO';
                  const esPasadoTurno = esPasado(turno.fechaHoraInicio) && !esCancelado;
                  const fecha = new Date(turno.fechaHoraInicio);

                  return (
                    <article key={turno.id} className={`p-5 rounded-xl border flex flex-col justify-between transition-all ${
                      esCancelado ? 'bg-red-50 border-red-100 opacity-80' : 
                      esPasadoTurno ? 'bg-gray-50 border-gray-200' : 
                      'bg-white border-blue-200 shadow-sm hover:shadow-md'
                    }`}>
                      <div>
                        <div className="flex justify-between items-start mb-3">
                          <span className={`text-xs font-bold px-2 py-1 rounded-md ${
                            esCancelado ? 'bg-red-100 text-red-700' : 
                            esPasadoTurno ? 'bg-gray-200 text-gray-700' : 
                            'bg-blue-100 text-blue-800'
                          }`}>
                            {turno.estado}
                          </span>
                          <span className={`text-lg font-extrabold ${esCancelado || esPasadoTurno ? 'text-gray-500' : 'text-gray-900'}`}>
                            ${turno.precio}
                          </span>
                        </div>
                        
                        <h3 className={`font-bold text-xl ${esCancelado || esPasadoTurno ? 'text-gray-700' : 'text-gray-900'}`}>
                          {turno.nombreServicio}
                        </h3>
                        
                        <div className="mt-3 space-y-1.5 text-sm text-gray-600">
                          <p className="flex items-center gap-2">
                            <span className="text-lg">📅</span> 
                            <span className="font-medium">
                              {fecha.toLocaleDateString('es-AR', { weekday: 'short', day: 'numeric', month: 'short' })} - {fecha.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' })} hs
                            </span>
                          </p>
                          <p className="flex items-center gap-2">
                            <span className="text-lg">🏪</span> 
                            <span className="truncate">{turno.nombreNegocio}</span>
                          </p>
                          <p className="flex items-center gap-2">
                            <span className="text-lg">👤</span> 
                            <span className="truncate">{turno.nombreRecurso}</span>
                          </p>
                        </div>
                      </div>

                      <div className="mt-5 pt-4 border-t border-gray-100 flex flex-col gap-2">
                        {filtroReservas === 'proximos' ? (
                          <button 
                            onClick={() => cancelarReserva(turno.id)}
                            className="w-full text-sm text-red-600 hover:text-white border border-red-600 hover:bg-red-600 px-4 py-2 rounded-lg transition-colors font-medium text-center"
                          >
                            Cancelar turno
                          </button>
                        ) : (
                          <Link 
                            to={`/negocio/${turno.aliasNegocio}`}
                            className="w-full text-sm text-blue-600 hover:bg-blue-50 border border-blue-200 px-4 py-2 rounded-lg transition-colors font-medium text-center block"
                          >
                            Volver a reservar
                          </Link>
                        )}
                      </div>
                    </article>
                  );
                })}
              </div>
            ) : (
              <div className="text-center py-12 bg-gray-50 rounded-lg border border-dashed border-gray-300">
                <p className="text-gray-500 font-medium">
                  {filtroReservas === 'proximos' 
                    ? 'No tenés turnos próximos programados.' 
                    : 'Tu historial está vacío.'}
                </p>
              </div>
            )}
          </div>
        )}

        {/* PESTAÑA: MIS TRABAJOS (EMPLEADO) */}
        {activeTab === 'trabajos' && (
          <div className="animate-fadeIn">
            <header className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold text-gray-800">Espacios de Trabajo Asignados</h2>
            </header>
            <MisTrabajosList />
          </div>
        )}

        {/* PESTAÑA: MIS NEGOCIOS (DUEÑO) */}
        {activeTab === 'negocios' && (
          <div className="animate-fadeIn">
            {!isFormOpen ? (
              <>
                <header className="flex justify-between items-center mb-6">
                  <h2 className="text-xl font-bold text-gray-800">Tus Negocios</h2>
                  <button onClick={handleAbrirCrear} className="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg text-sm transition-colors">
                    + Crear Negocio
                  </button>
                </header>
                
                {loadingNegocios ? (
                  <div className="flex justify-center py-12"><div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600"></div></div>
                ) : misNegocios.length > 0 ? (
                  <section className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {misNegocios.map(negocio => (
                      <NegocioCard 
                        key={negocio.id} 
                        negocio={negocio} 
                        onEliminar={eliminarNegocio} 
                        onEditar={(n) => { setEditingNegocio(n); setIsFormOpen(true); }} 
                      />
                    ))}
                  </section>
                ) : (
                  <div className="text-center py-10 bg-gray-50 rounded-lg border border-dashed border-gray-300">
                    <h3 className="text-sm font-semibold text-gray-900">Aún no registraste ningún negocio</h3>
                  </div>
                )}
              </>
            ) : (
              <NegocioForm 
                initialData={editingNegocio} 
                onSubmit={handleSubmitForm} 
                onCancel={() => setIsFormOpen(false)} 
                isSubmitting={isSubmitting} 
              />
            )}
          </div>
        )}
      </main>
    </div>
  );
};

export default Dashboard;