import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useGestionMiTrabajo } from '../hooks/useGestionMiTrabajo';
import RecursoForm from '../components/RecursoForm';
import RecursoAccordion from '../components/RecursoAccordion';
import AgendaTab from '../components/AgendaTab';
import HistorialTab from '../components/HistorialTab';
import ReportesTab from '../components/ReportesTab'; // <-- Importamos la pestaña de reportes

const GestionMiTrabajo = () => {
  const { id } = useParams();
  const { recurso, loading, guardarRecurso, guardarServicio, eliminarServicio } = useGestionMiTrabajo(id);
  
  const [pestañaActiva, setPestañaActiva] = useState('configuracion');
  const [isFormRecursoOpen, setIsFormRecursoOpen] = useState(false);

  const handleRecursoSubmit = async (formData) => {
    const res = await guardarRecurso(formData);
    if (res.success) setIsFormRecursoOpen(false);
    else alert(res.message);
  };

  if (loading) return <div className="p-8 text-center flex justify-center"><div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600"></div></div>;
  if (!recurso) return <div className="p-8 text-center text-gray-500">No se encontró tu espacio de trabajo.</div>;

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <header className="flex items-center gap-4 mb-8">
        <Link to="/dashboard" state={{ tab: 'mistrabajos' }} className="text-gray-500 hover:text-gray-700 bg-gray-100 p-2 rounded-full transition-colors">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
        </Link>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Mi Espacio: {recurso.nombre}</h1>
          <p className="text-sm text-gray-500">En: {recurso.nombreNegocio || 'Negocio'}</p>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        <aside className="lg:col-span-1 space-y-6">
          <nav className="bg-white rounded-xl shadow-sm border border-gray-100 p-3 flex flex-col gap-1">
            <button onClick={() => setPestañaActiva('configuracion')} className={`text-left px-4 py-3 rounded-lg font-medium transition-colors ${pestañaActiva === 'configuracion' ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:bg-gray-50'}`}>⚙️ Mis Servicios y Horarios</button>
            <button onClick={() => setPestañaActiva('agenda')} className={`text-left px-4 py-3 rounded-lg font-medium transition-colors ${pestañaActiva === 'agenda' ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:bg-gray-50'}`}>📅 Mi Agenda</button>
            <button onClick={() => setPestañaActiva('historial')} className={`text-left px-4 py-3 rounded-lg font-medium transition-colors ${pestañaActiva === 'historial' ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:bg-gray-50'}`}>📊 Mi Historial</button>
            <button onClick={() => setPestañaActiva('reportes')} className={`text-left px-4 py-3 rounded-lg font-medium transition-colors ${pestañaActiva === 'reportes' ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:bg-gray-50'}`}>📈 Mis Estadísticas</button>
          </nav>
        </aside>

        <section className="lg:col-span-3">
          {pestañaActiva === 'configuracion' && (
            <div className="animate-fadeIn">
              <header className="flex justify-between items-center mb-6">
                <h2 className="font-bold text-xl text-gray-800">Mi Configuración</h2>
              </header>

              {isFormRecursoOpen && (
                <RecursoForm initialData={recurso} onSubmit={handleRecursoSubmit} onCancel={() => setIsFormRecursoOpen(false)} />
              )}

              <div className="space-y-4">
                <RecursoAccordion 
                  recurso={recurso}
                  isExpanded={true} 
                  onToggle={() => {}} 
                  onEditRecurso={() => setIsFormRecursoOpen(true)}
                  onDeleteRecurso={() => alert('Acción denegada: Solo el dueño del negocio puede dar de baja este recurso.')}
                  onSaveServicio={guardarServicio}
                  onDeleteServicio={eliminarServicio}
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

          {/* NUEVA PESTAÑA DE REPORTES PARA EL EMPLEADO */}
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
    </div>
  );
};

export default GestionMiTrabajo;