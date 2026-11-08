import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { useMisNegocios } from '../hooks/useMisNegocios';
import { useMisReservas } from '../hooks/useMisReservas';
import { esFuturo, esPasado } from '../../../utils/fechas';
import { useToast } from '../../../context/ToastContext';
import NegocioCard from '../components/NegocioCard';
import NegocioForm from '../components/NegocioForm';
import MisTrabajosList from '../components/MisTrabajosList';
import TurnoCard from '../components/TurnoCard';
import ModalDetalleTurno from '../components/ModalDetalleTurno';
import ModalConfirmacion from '../../../components/ModalConfirmacion';
import { SquarePen, Store} from 'lucide-react';

const Dashboard = () => {
  const location = useLocation();
  const navigate = useNavigate();
  const { addToast } = useToast();
  
  const [seleccionTab, setSeleccionTab] = useState(null);
  const activeTab = seleccionTab?.locationKey === location.key
    ? seleccionTab.tab : location.state?.tab || 'reservas';
  const setActiveTab = tab => setSeleccionTab({ locationKey: location.key, tab });
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingNegocio, setEditingNegocio] = useState(null);
  const [filtroReservas, setFiltroReservas] = useState('proximos');
  
  // Estados para modales
  const [turnoDetalle, setTurnoDetalle] = useState(null);
  const [turnoACancelar, setTurnoACancelar] = useState(null);
  const [negocioAEliminar, setNegocioAEliminar] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const { misNegocios, isLoading: loadingNegocios, isSubmitting, error: errorNegocios, reintentar: reintentarNegocios, eliminarNegocio, guardarNegocio } = useMisNegocios();
  const { reservas, loading: loadingReservas, error: errorReservas, reintentar: reintentarReservas, cancelarReserva } = useMisReservas();

  const handleAbrirCrear = () => {
    setEditingNegocio(null);
    setIsFormOpen(true);
  };

  const handleSubmitForm = async (formData) => {
    const res = await guardarNegocio(formData, editingNegocio?.id);
    if (res.success) {
      setIsFormOpen(false);
      addToast(editingNegocio ? 'Negocio actualizado.' : 'Negocio creado exitosamente.', 'success');
    } else {
      addToast(res.message, 'error');
    }
  };

  const handleConfirmarEliminarNegocio = async () => {
    setIsProcessing(true);
    const res = await eliminarNegocio(negocioAEliminar.id);
    setIsProcessing(false);
    if (res.success) {
      addToast('Negocio eliminado correctamente.', 'success');
      setNegocioAEliminar(null);
    } else {
      addToast(res.message, 'error');
    }
  };

  const turnosProximos = reservas.filter(t => esFuturo(t.fechaHoraInicio) && t.estado !== 'CANCELADO');
  const turnosHistorial = reservas.filter(t => esPasado(t.fechaHoraInicio) || t.estado === 'CANCELADO');
  const turnosAMostrar = filtroReservas === 'proximos' ? turnosProximos : turnosHistorial;

  const getTabClass = (tabName) => `whitespace-nowrap pb-4 px-1 border-b-2 font-medium text-sm transition-colors ${
    activeTab === tabName ? 'border-app-marineblue text-app-marineblue' : 'border-transparent text-app-marineblue hover:text-app-black'
  }`;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 sm:py-8">
      <header className="mb-6 sm:mb-8">
        <h1 className="text-2xl sm:text-3xl font-bold text-app-black">Mi Panel</h1>
        <p className="text-sm sm:text-base text-app-black mt-1">Gestioná tus turnos, trabajos y negocios desde acá.</p>
      </header>

      <nav className="border-b border-gray-200 mb-6 sm:mb-8 flex space-x-6 overflow-x-auto">
        <button onClick={() => { setActiveTab('reservas'); setIsFormOpen(false); }} className={getTabClass('reservas')}>Mis Reservas</button>
        <button onClick={() => { setActiveTab('trabajos'); setIsFormOpen(false); }} className={getTabClass('trabajos')}>Mis Trabajos</button>
        <button onClick={() => { setActiveTab('negocios'); setIsFormOpen(false); }} className={getTabClass('negocios')}>Mis Negocios</button>
      </nav>

      <main className="bg-white rounded-2xl shadow-sm border border-gray-100 p-4 sm:p-6 min-h-100">
        
        {activeTab === 'reservas' && (
          <div className="animate-fadeIn">
            <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
              <h2 className="text-xl font-bold text-app-black">
                {filtroReservas === 'proximos' ? 'Tus Próximos Turnos' : 'Historial de Turnos'}
              </h2>
              
              <select 
                value={filtroReservas}
                onChange={(e) => setFiltroReservas(e.target.value)}
                className="bg-app-white border-none text-app-black text-sm rounded-xl focus:ring-2 focus:ring-app-marineblue block px-4 py-2 cursor-pointer font-medium"
              >
                <option value="proximos">Turnos Próximos</option>
                <option value="historial">Historial (Pasados/Cancelados)</option>
              </select>
            </div>
            
            {loadingReservas ? (
              <div className="flex justify-center py-12"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-app-marineblue"></div></div>
            ) : errorReservas ? (
              <div role="alert" className="py-12 text-center text-sm">
                <p>{errorReservas}</p>
                <button onClick={reintentarReservas} className="mt-4 font-semibold text-app-marineblue hover:underline">Reintentar</button>
              </div>
            ) : turnosAMostrar.length > 0 ? (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-10">
                {turnosAMostrar.map(turno => (
                  <TurnoCard 
                    key={turno.id}
                    turno={turno}
                    mostrarNegocio={true}
                    mostrarRecurso={true}
                    mostrarContacto={false}
                    onVerDetalle={(t) => setTurnoDetalle(t)}
                    onAccion={
                      filtroReservas === 'proximos' 
                        ? (t) => setTurnoACancelar(t) 
                        : (t) => navigate(`/negocio/${t.aliasNegocio}`)
                    }
                    textoAccion={filtroReservas === 'proximos' ? "Cancelar turno" : "Volver a reservar"}
                    varianteAccion={filtroReservas === 'proximos' ? "peligro" : "neutro"}
                  />
                ))}
              </div>
            ) : (
              <div className="text-center py-16 bg-white rounded-2xl border border-dashed border-gray-200">
                <SquarePen className="h-12 w-12 mx-auto mb-3 " />
                <h3 className="text-app-marineblue font-medium text-lg">
                  {filtroReservas === 'proximos' 
                    ? 'No tenés turnos próximos programados' 
                    : 'Tu historial está vacío'}
                </h3>
                <p className="text-gray-400 text-sm mt-1">
                  {filtroReservas === 'proximos'
                    ? 'Buscá tu local de confianza y reservá un horario.'
                    : 'Cuando asistas o canceles un turno, aparecerá acá.'}
                </p>
              </div>
            )}
          </div>
        )}

        {activeTab === 'trabajos' && (
          <div className="animate-fadeIn">
            <header className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold text-app-black">Espacios de Trabajo Asignados</h2>
            </header>
            <MisTrabajosList />
          </div>
        )}

        {activeTab === 'negocios' && (
          <div className="animate-fadeIn">
            {!isFormOpen ? (
              <>
                <header className="flex justify-between items-center mb-6">
                  <h2 className="text-xl font-bold text-app-black">Tus Negocios</h2>
                  <button onClick={handleAbrirCrear} className="bg-app-marineblue hover:bg-app-black/90 text-white px-5 py-2 rounded-xl text-sm font-medium transition-colors shadow-sm duration-300">
                    + Crear Negocio
                  </button>
                </header>
                
                {loadingNegocios ? (
                  <div className="flex justify-center py-12"><div className="animate-spin rounded-full h-10 w-10 border-b-2 border-app-marineblue"></div></div>
                ) : errorNegocios ? (
                  <div role="alert" className="py-12 text-center text-sm">
                    <p>{errorNegocios}</p>
                    <button onClick={reintentarNegocios} className="mt-4 font-semibold text-app-marineblue hover:underline">Reintentar</button>
                  </div>
                ) : misNegocios.length > 0 ? (
                  <section className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                    {misNegocios.map(negocio => (
                      <NegocioCard 
                        key={negocio.id} 
                        negocio={negocio} 
                        onEliminar={(id, nombre) => setNegocioAEliminar({ id, nombre })} 
                        onEditar={(n) => { setEditingNegocio(n); setIsFormOpen(true); }} 
                      />
                    ))}
                  </section>
                ) : (
                  <div className="text-center py-16 bg-white rounded-2xl border border-dashed border-gray-200">
                    <Store className="h-12 w-12 mx-auto mb-3 text-app-black" />
                    <h3 className="text-app-marineblue font-medium text-lg">Aún no registraste ningún negocio</h3>
                    <p className="text-gray-400 text-sm mt-1">Creá tu primer negocio para empezar a gestionar tus turnos y tu staff.</p>
                  </div>
                )}
              </>
            ) : (
              <NegocioForm 
                key={editingNegocio?.id ?? 'nuevo'}
                initialData={editingNegocio} 
                onSubmit={handleSubmitForm} 
                onCancel={() => setIsFormOpen(false)} 
                isSubmitting={isSubmitting} 
              />
            )}
          </div>
        )}
      </main>

      <ModalDetalleTurno 
        turnoDetalle={turnoDetalle} 
        onClose={() => setTurnoDetalle(null)}
        onCancelarTurno={setTurnoACancelar}
        esDueño={false} 
      />

      <ModalConfirmacion 
        isOpen={!!turnoACancelar}
        titulo="¿Cancelar este turno?"
        mensaje={<>Estás a punto de cancelar el turno para <strong>{turnoACancelar?.nombreServicio}</strong>. Esta acción no se puede deshacer.</>}
        textoConfirmar="Sí, cancelar"
        esPeligroso={true}
        isConfirming={isProcessing}
        onClose={() => setTurnoACancelar(null)}
        onConfirm={async () => {
          setIsProcessing(true);
          const exito = await cancelarReserva(turnoACancelar.id);
          setIsProcessing(false);
          if (exito) setTurnoACancelar(null);
        }}
      />

      <ModalConfirmacion 
        isOpen={!!negocioAEliminar}
        titulo="Eliminar Negocio"
        mensaje={<>¿Estás seguro que querés eliminar el negocio <strong>{negocioAEliminar?.nombre}</strong>? Toda su información se perderá de forma permanente.</>}
        textoConfirmar="Eliminar definitivamente"
        esPeligroso={true}
        isConfirming={isProcessing}
        onClose={() => setNegocioAEliminar(null)}
        onConfirm={handleConfirmarEliminarNegocio}
      />
    </div>
  );
};

export default Dashboard;
