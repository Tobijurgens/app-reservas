import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useGestionMiTrabajo } from '../hooks/useGestionMiTrabajo';
import { useToast } from '../../../context/ToastContext';
import RecursoForm from '../components/RecursoForm';
import RecursoAccordion from '../components/RecursoAccordion';
import AgendaTab from '../components/AgendaTab';
import HistorialTab from '../components/HistorialTab';
import ReportesTab from '../components/ReportesTab';
import ModalConfirmacion from '../../../components/ModalConfirmacion';
import { Settings, CalendarDays, RotateCcwClock, ChartPie} from 'lucide-react';

const GestionMiTrabajo = () => {
  const { id } = useParams();
  const { recurso, loading, guardarRecurso, guardarServicio, eliminarServicio } = useGestionMiTrabajo(id);
  const { addToast } = useToast();
  
  const [pestañaActiva, setPestañaActiva] = useState('configuracion');
  const [isFormRecursoOpen, setIsFormRecursoOpen] = useState(false);

  // Estados para el modal de eliminar servicio
  const [servicioAEliminar, setServicioAEliminar] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const handleRecursoSubmit = async (formData) => {
    const res = await guardarRecurso(formData);
    if (res.success) {
      setIsFormRecursoOpen(false);
      addToast('Información actualizada correctamente', 'success');
    } else {
      addToast(res.message, 'error');
    }
  };

  const handleConfirmarEliminarServicio = async () => {
    setIsProcessing(true);
    await eliminarServicio(servicioAEliminar.id);
    setIsProcessing(false);
    setServicioAEliminar(null);
    addToast('Servicio eliminado correctamente.', 'success');
  };

  if (loading) return <div className="p-8 text-center flex justify-center"><div className="animate-spin rounded-full h-10 w-10 border-b-2 border-app-marineblue"></div></div>;
  if (!recurso) return <div className="p-8 text-center text-app-marineblue">No se encontró tu espacio de trabajo.</div>;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <header className="flex items-center gap-4 mb-8">
        <Link to="/dashboard" state={{ tab: 'trabajos' }} className="text-app-marineblue hover:text-app-black bg-app-white p-3 rounded-xl transition-colors shadow-sm">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
        </Link>
        <div>
          <h1 className="text-2xl font-bold text-app-black">Mi Espacio: {recurso.nombre}</h1>
          <p className="text-sm text-app-marineblue">En: {recurso.nombreNegocio || 'Negocio'}</p>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        <aside className="lg:col-span-1 space-y-6">
          <nav className="bg-white rounded-2xl shadow-sm border border-gray-100 p-3 flex flex-col gap-2">
            <button onClick={() => setPestañaActiva('configuracion')} className={`flex items-center gap-2 text-left px-4 py-3 rounded-xl font-medium transition-colors ${pestañaActiva === 'configuracion' ? 'bg-app-white text-app-marineblue' : 'text-app-black hover:bg-gray-50'}`}><Settings className="w-5 h-5" /> Mis Servicios y horarios</button>
            <button onClick={() => setPestañaActiva('agenda')} className={`flex items-center gap-2 text-left px-4 py-3 rounded-xl font-medium transition-colors ${pestañaActiva === 'agenda' ? 'bg-app-white text-app-marineblue' : 'text-app-black hover:bg-gray-50'}`}><CalendarDays className="w-5 h-5"/> Mi Agenda</button>
            <button onClick={() => setPestañaActiva('historial')} className={`flex items-center gap-2 text-left px-4 py-3 rounded-xl font-medium transition-colors ${pestañaActiva === 'historial' ? 'bg-app-white text-app-marineblue' : 'text-app-black hover:bg-gray-50'}`}><RotateCcwClock className="w-5 h-5"/> Mi Historial</button>
            <button onClick={() => setPestañaActiva('reportes')} className={`flex items-center gap-2 text-left px-4 py-3 rounded-xl font-medium transition-colors ${pestañaActiva === 'reportes' ? 'bg-app-white text-app-marineblue' : 'text-app-black hover:bg-gray-50'}`}><ChartPie className="w-5 h-5"/> Mis Estadísticas</button>
          </nav>
        </aside>

        <section className="lg:col-span-3">
          {pestañaActiva === 'configuracion' && (
            <div className="animate-fadeIn">
              <header className="flex justify-between items-center mb-6">
                <h2 className="font-bold text-xl text-app-black">Mi Configuración</h2>
              </header>

              {isFormRecursoOpen && (
                <RecursoForm initialData={recurso} onSubmit={handleRecursoSubmit} onCancel={() => setIsFormRecursoOpen(false)} isOwner={false} />
              )}

              <div className="space-y-4">
                <RecursoAccordion 
                  recurso={recurso}
                  isExpanded={true} 
                  onToggle={() => {}} 
                  onEditRecurso={() => setIsFormRecursoOpen(true)}
                  onDeleteRecurso={null} // Le pasamos null para que el acordeón maneje el Toast de error nativamente
                  onSaveServicio={async (formData, editingId, idRecurso) => {
                     const res = await guardarServicio(formData, editingId, idRecurso);
                     if (res.success) addToast('Servicio guardado exitosamente.', 'success');
                     else addToast('Error al guardar el servicio', 'error');
                  }}
                  onDeleteServicio={(id, nombre) => setServicioAEliminar({ id, nombre })}
                />
              </div>
            </div>
          )}

          {pestañaActiva === 'agenda' && (
            <div className="animate-fadeIn">
              <AgendaTab recursos={[recurso]} />
            </div>
          )}

          {pestañaActiva === 'historial' && (
            <div className="animate-fadeIn">
              <HistorialTab recursoId={recurso.id} />
            </div>
          )}

          {pestañaActiva === 'reportes' && (
            <div className="animate-fadeIn">
              <ReportesTab 
                negocioId={recurso.negocioId} 
                recursos={[recurso]} 
                esOwner={false} 
                miRecursoId={recurso.id} 
              />
            </div>
          )}
        </section>
      </div>

      {/* Modal de confirmación para eliminar servicio */}
      <ModalConfirmacion 
        isOpen={!!servicioAEliminar}
        titulo="Eliminar Servicio"
        mensaje={<>¿Estás seguro de eliminar el servicio <strong>{servicioAEliminar?.nombre}</strong>?</>}
        textoConfirmar="Sí, eliminar"
        esPeligroso={true}
        isConfirming={isProcessing}
        onClose={() => setServicioAEliminar(null)}
        onConfirm={handleConfirmarEliminarServicio}
      />
    </div>
  );
};

export default GestionMiTrabajo;