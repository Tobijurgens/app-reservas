import { useState, useEffect, useContext } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useGestionNegocio } from '../hooks/useGestionNegocio';
import { AuthContext } from '../../../context/AuthContext'; 
import RecursoForm from '../components/RecursoForm';
import RecursoAccordion from '../components/RecursoAccordion';
import AgendaTab from '../components/AgendaTab';
import HistorialTab from '../components/HistorialTab';
import ReportesTab from '../components/ReportesTab';

const DIAS_ES = {
  MONDAY: 'Lunes', TUESDAY: 'Martes', WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves', FRIDAY: 'Viernes', SATURDAY: 'Sábado', SUNDAY: 'Domingo'
};

const ORDEN_DIAS = {
  MONDAY: 1, TUESDAY: 2, WEDNESDAY: 3,
  THURSDAY: 4, FRIDAY: 5, SATURDAY: 6, SUNDAY: 7
};

const GestionNegocio = () => {
  const { id } = useParams();
  
  const { user } = useContext(AuthContext); 
  
  const { 
    negocio, 
    recursosTotales, 
    recursosActivos, 
    loading, 
    guardarRecurso, 
    eliminarRecurso, 
    guardarServicio, 
    eliminarServicio 
  } = useGestionNegocio(id);

  const emailLogueado = user?.email;

  // CORRECCIÓN CLAVE: Ahora leemos 'emailOwner' tal como lo manda el nuevo DTO
  const isOwner = negocio && (emailLogueado === negocio?.emailOwner); 
  
  let miRecursoId = null;
  if (!isOwner && recursosTotales.length > 0 && emailLogueado) {
      const miRecurso = recursosTotales.find(r => r.emailUsuario === emailLogueado);
      if (miRecurso) {
          miRecursoId = miRecurso.id;
      }
  }

  // Iniciamos la pestaña vacía para no apresurar a React
  const [pestañaActiva, setPestañaActiva] = useState(''); 
  
  const [isFormRecursoOpen, setIsFormRecursoOpen] = useState(false);
  const [editingRecurso, setEditingRecurso] = useState(null);
  const [recursoExpandidoId, setRecursoExpandidoId] = useState(null);
  const [mostrarHorarios, setMostrarHorarios] = useState(false);

  // EFECTO: Cuando el negocio termina de cargar, decidimos la pestaña
  useEffect(() => {
    if (!loading && negocio) {
      // Usamos el nuevo nombre 'emailOwner'
      const esDueño = emailLogueado === negocio?.emailOwner;
      setPestañaActiva(esDueño ? 'configuracion' : 'agenda');
    }
  }, [loading, negocio, emailLogueado]);

  const handleAbrirCrearRecurso = () => {
    setEditingRecurso(null);
    setIsFormRecursoOpen(true);
  };

  const handleRecursoSubmit = async (formData) => {
    const res = await guardarRecurso(formData, editingRecurso?.id);
    if (res.success) setIsFormRecursoOpen(false);
    else alert(res.message);
  };

  if (loading || !pestañaActiva) {
      return <div className="p-8 text-center flex justify-center"><div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600"></div></div>;
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      <header className="flex items-center gap-4 mb-8">
        <Link to="/dashboard" state={{ tab: 'negocios' }} className="text-gray-500 hover:text-gray-700 bg-gray-100 p-2 rounded-full transition-colors">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
        </Link>
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Gestionar: {negocio?.nombre}</h1>
          <p className="text-sm text-gray-500">tuapp.com/negocio/{negocio?.alias}</p>
        </div>
      </header>

      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
        
        <aside className="lg:col-span-1 space-y-6">
          <nav className="bg-white rounded-xl shadow-sm border border-gray-100 p-3 flex flex-col gap-1">
            
            {isOwner && (
                <button
                onClick={() => setPestañaActiva('configuracion')}
                className={`text-left px-4 py-3 rounded-lg font-medium transition-colors ${
                    pestañaActiva === 'configuracion' ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:bg-gray-50'
                }`}
                >
                ⚙️ Configuración y Staff
                </button>
            )}

            <button
              onClick={() => setPestañaActiva('agenda')}
              className={`text-left px-4 py-3 rounded-lg font-medium transition-colors ${
                pestañaActiva === 'agenda' ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:bg-gray-50'
              }`}
            >
              📅 {isOwner ? 'Ver Agendas' : 'Mi Agenda'}
            </button>
            <button
              onClick={() => setPestañaActiva('historial')}
              className={`text-left px-4 py-3 rounded-lg font-medium transition-colors ${
                pestañaActiva === 'historial' ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:bg-gray-50'
              }`}
            >
              📊 {isOwner ? 'Historial de Turnos' : 'Mi Historial'}
            </button>
            <button
              onClick={() => setPestañaActiva('reportes')}
              className={`text-left px-4 py-3 rounded-lg font-medium transition-colors ${
                pestañaActiva === 'reportes' ? 'bg-blue-50 text-blue-700' : 'text-gray-600 hover:bg-gray-50'
              }`}
            >
              📈 {isOwner ? 'Estadísticas y Reportes' : 'Mis Estadísticas'}
            </button>
          </nav>

          <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6">
            <h2 className="font-bold text-gray-800 mb-4">Información Comercial</h2>
            <div className="space-y-3 text-sm text-gray-600 mb-4 border-b border-gray-100 pb-4">
              <p><strong>Dirección:</strong> {negocio?.direccion}</p>
              <p><strong>Teléfono:</strong> {negocio?.telefono}</p>
              {negocio?.emailContacto && (
                <p><strong>Email:</strong> {negocio.emailContacto}</p>
              )}
            </div>

            <div>
              <button 
                onClick={() => setMostrarHorarios(!mostrarHorarios)}
                className="w-full flex justify-between items-center text-sm font-bold text-gray-800 hover:text-blue-600 transition-colors py-2"
              >
                <span>Horarios de Atención</span>
                <svg className={`w-5 h-5 transform transition-transform duration-200 ${mostrarHorarios ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
                </svg>
              </button>

              {mostrarHorarios && (
                <ul className="space-y-2 text-sm text-gray-600 mt-3 animate-fadeIn bg-gray-50 p-3 rounded-lg border border-gray-100">
                  {negocio?.horarios?.length > 0 ? (
                    [...negocio.horarios]
                      .sort((a, b) => ORDEN_DIAS[a.diaSemana] - ORDEN_DIAS[b.diaSemana])
                      .map((h, idx) => (
                        <li key={idx} className="flex justify-between items-start py-1 border-b border-gray-200 last:border-0">
                          <span className="font-medium w-24">{DIAS_ES[h.diaSemana]}</span>
                          <div className="text-right flex-1">
                            {h.activo ? (
                              <>
                                <span className="block">{h.horaInicio} - {h.horaFin}</span>
                                {(h.horaInicioDescanso && h.horaFinDescanso) && (
                                  <span className="block text-xs text-gray-400 mt-0.5">
                                    Corta: {h.horaInicioDescanso} a {h.horaFinDescanso}
                                  </span>
                                )}
                              </>
                            ) : (
                              <span className="text-gray-400 italic">Cerrado</span>
                            )}
                          </div>
                        </li>
                      ))
                  ) : (
                    <li className="text-gray-400 italic">No hay horarios configurados.</li>
                  )}
                </ul>
              )}
            </div>
          </div>
        </aside>

        <section className="lg:col-span-3">
          
          {(pestañaActiva === 'configuracion' && isOwner) && (
            <div className="animate-fadeIn">
              <header className="flex justify-between items-center mb-6">
                <h2 className="font-bold text-xl text-gray-800">Recursos (Staff / Espacios)</h2>
                {!isFormRecursoOpen && (
                  <button onClick={handleAbrirCrearRecurso} className="bg-blue-600 text-white px-4 py-2 rounded-lg text-sm font-medium hover:bg-blue-700 transition-colors">
                    + Nuevo Recurso
                  </button>
                )}
              </header>

              {isFormRecursoOpen && (
                <RecursoForm 
                  initialData={editingRecurso} 
                  negocioHorarios={negocio?.horarios}
                  onSubmit={handleRecursoSubmit} 
                  onCancel={() => setIsFormRecursoOpen(false)} 
                />
              )}

              <div className="space-y-4">
                {recursosActivos.length > 0 ? (
                  recursosActivos.map(recurso => (
                    <RecursoAccordion 
                      key={recurso.id}
                      recurso={recurso}
                      isExpanded={recursoExpandidoId === recurso.id}
                      onToggle={() => setRecursoExpandidoId(recursoExpandidoId === recurso.id ? null : recurso.id)}
                      onEditRecurso={(r) => { setEditingRecurso(r); setIsFormRecursoOpen(true); }}
                      onDeleteRecurso={eliminarRecurso}
                      onSaveServicio={guardarServicio}
                      onDeleteServicio={eliminarServicio}
                    />
                  ))
                ) : !isFormRecursoOpen && (
                  <div className="text-center py-10 bg-white rounded-xl border border-dashed border-gray-300 text-gray-500 text-sm">
                    Aún no tenés recursos activos (Ej: Empleados, Canchas, Consultorios).<br/>Agregá uno para empezar a cargar servicios.
                  </div>
                )}
              </div>
            </div>
          )}

          {pestañaActiva === 'agenda' && (
            <div className="animate-fadeIn">
              <AgendaTab recursos={isOwner ? recursosActivos : recursosActivos.filter(r => r.id === miRecursoId)} />
            </div>
          )}

          {pestañaActiva === 'historial' && (
            <div className="animate-fadeIn">
              <HistorialTab 
                negocioId={isOwner ? id : null} 
                recursoId={isOwner ? null : miRecursoId} 
                recursos={recursosTotales} 
              />
            </div>
          )}

          {pestañaActiva === 'reportes' && (
            <div className="animate-fadeIn">
              <ReportesTab 
                  negocioId={id} 
                  recursos={recursosTotales} 
                  esOwner={isOwner} 
                  miRecursoId={miRecursoId} 
              />
            </div>
          )}
          
        </section>
      </div>
    </div>
  );
};

export default GestionNegocio;