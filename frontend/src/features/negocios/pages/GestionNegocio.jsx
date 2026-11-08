import { useState, useContext } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useGestionNegocio } from '../hooks/useGestionNegocio';
import { AuthContext } from '../../../context/AuthContext'; 
import { useToast } from '../../../context/ToastContext';
import { NegociosService } from '../../../services/NegociosService';
import RecursoForm from '../components/RecursoForm';
import RecursoAccordion from '../components/RecursoAccordion';
import AgendaTab from '../components/AgendaTab';
import HistorialTab from '../components/HistorialTab';
import ReportesTab from '../components/ReportesTab';
import ModalConfirmacion from '../../../components/ModalConfirmacion';
import EstadoInvitacion from '../components/EstadoInvitacion';
import NegocioForm from '../components/NegocioForm';
import { DIAS_ES, ordenarHorariosPorDia } from '../../../utils/fechas'; 
import { Settings, CalendarDays, RotateCcwClock, ChartPie, ChevronDown, UserRoundPlus, ExternalLink, Copy, Edit} from 'lucide-react';

const GestionNegocio = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useContext(AuthContext); 
  const { addToast } = useToast();
  
  const { 
    negocio, 
    recursosTotales, 
    recursosActivos, 
    miAsignacion,
    puedeAdministrar,
    loading, 
    guardarRecurso, 
    eliminarRecurso, 
    guardarServicio, 
    eliminarServicio 
  } = useGestionNegocio(id);

  const emailLogueado = user?.email;
  const isOwner = negocio?.propietario === true;
  
  const miRecursoId = miAsignacion?.id ?? null;

  const [seleccion, setSeleccion] = useState(null);
  const pestañaActiva = seleccion?.id === id && seleccion.email === emailLogueado
    ? seleccion.pestaña : isOwner ? 'configuracion' : 'agenda';
  const setPestañaActiva = pestaña => setSeleccion({ id, email: emailLogueado, pestaña });
  const [isFormRecursoOpen, setIsFormRecursoOpen] = useState(false);
  const [isFormNegocioOpen, setIsFormNegocioOpen] = useState(false);
  const [editingRecurso, setEditingRecurso] = useState(null);
  const [recursoExpandidoId, setRecursoExpandidoId] = useState(null);
  const [mostrarHorarios, setMostrarHorarios] = useState(false);
  
  const [recursoAEliminar, setRecursoAEliminar] = useState(null);
  const [servicioAEliminar, setServicioAEliminar] = useState(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const handleCopiarLink = () => {
    const link = `${window.location.origin}/negocio/${negocio.alias}`;
    navigator.clipboard.writeText(link);
    addToast('¡Link copiado al portapapeles! Ya podés enviárselo a tus clientes.', 'success');
  };

  const handleRecursoSubmit = async (formData) => {
    const res = await guardarRecurso(formData, editingRecurso?.id);
    if (res.success) {
      setIsFormRecursoOpen(false);
      if (!editingRecurso && formData.emailUsuario) {
        addToast('Recurso creado. El usuario debe verificar su correo para aceptar la invitación.', 'success');
      } else {
        addToast('Recurso guardado correctamente.', 'success');
      }
    } else {
      addToast(res.message, 'error');
    }
  };

  const handleActualizarNegocio = async (formData) => {
    setIsProcessing(true);
    try {
      await NegociosService.actualizarNegocio(negocio.id, formData);
      addToast('Negocio actualizado exitosamente', 'success');
      setIsFormNegocioOpen(false);
      setTimeout(() => window.location.reload(), 1000);
    } catch (err) {
      addToast(err.response?.data?.message || 'Error al actualizar', 'error');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleConfirmarEliminarRecurso = async () => {
    setIsProcessing(true);
    const res = await eliminarRecurso(recursoAEliminar.id);
    setIsProcessing(false);
    if (res.success) {
      addToast('Recurso eliminado correctamente.', 'success');
      setRecursoAEliminar(null);
    } else addToast(res.message, 'error');
  };

  const handleConfirmarEliminarServicio = async () => {
    setIsProcessing(true);
    const res = await eliminarServicio(servicioAEliminar.id);
    setIsProcessing(false);
    if (res.success) {
      addToast('Servicio eliminado correctamente.', 'success');
      setServicioAEliminar(null);
    } else addToast(res.message, 'error');
  };

  if (loading) {
      return <div className="p-8 text-center flex justify-center"><div className="animate-spin rounded-full h-10 w-10 border-b-2 border-app-marineblue"></div></div>;
  }

  if (!negocio || !puedeAdministrar) {
    return (
      <div className="mx-auto max-w-xl px-4 py-12">
        <div className="rounded-2xl border border-gray-100 bg-white p-6 shadow-sm">
          <h1 className="text-xl font-bold text-app-black">Acceso al negocio</h1>
          {miAsignacion ? <EstadoInvitacion estado={miAsignacion.estadoAsociacion} />
            : <p className="mt-4 text-sm text-gray-500">No se pudo comprobar el acceso a este negocio.</p>}
          <Link to="/dashboard" state={{ tab: 'trabajos' }} className="mt-6 inline-block font-semibold text-app-marineblue hover:text-app-black">Volver a Mis Trabajos</Link>
        </div>
      </div>
    );
  }

  if (isFormNegocioOpen) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-fadeIn">
        <NegocioForm 
          key={negocio.id}
          initialData={negocio} 
          onSubmit={handleActualizarNegocio} 
          onCancel={() => setIsFormNegocioOpen(false)} 
          isSubmitting={isProcessing} 
        />
      </div>
    );
  }

  return (
    // Altura calculada para no superar la pantalla (vh) descontando el navbar superior
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex flex-col lg:h-[calc(100vh-80px)]">
      
      {/* HEADER estático */}
      <header className="flex items-center gap-4 mb-8 flex-shrink-0">
        <button onClick={() => navigate('/dashboard', { state: { tab: isOwner ? 'negocios' : 'trabajos' }})} className="text-app-marineblue hover:text-app-black bg-app-white p-3 rounded-xl transition-colors shadow-sm">
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
        </button>
        <div>
          <h1 className="text-2xl font-bold text-app-black">Gestionar: {negocio?.nombre}</h1>
          <p className="text-sm text-app-marineblue">agendapp.com/negocio/{negocio?.alias}</p>
        </div>
      </header>

      {/* CONTENEDOR GRID CON SCROLL INTERNO INDEPENDIENTE */}
      <div className="grid grid-cols-1 lg:grid-cols-4 gap-8 flex-1 min-h-0">
        
        {/* PANEL LATERAL */}
        <aside className="lg:col-span-1 space-y-6 lg:overflow-y-auto lg:h-full lg:pr-2 lg:pb-8 scrollbar-hide">
          <nav className="bg-white rounded-2xl shadow-sm border border-gray-100 p-3 flex flex-col gap-2">
            {isOwner && (
                <button onClick={() => setPestañaActiva('configuracion')} className={`flex items-center gap-2 text-left px-4 py-3 rounded-xl font-medium transition-colors ${pestañaActiva === 'configuracion' ? 'bg-app-white text-app-black' : 'text-app-marineblue hover:bg-gray-50'}`}>
                <Settings className="w-5 h-5 text-app-black" /> Configuración y Staff
                </button>
            )}
            <button onClick={() => setPestañaActiva('agenda')} className={`flex items-center gap-2 text-left px-4 py-3 rounded-xl font-medium transition-colors ${pestañaActiva === 'agenda' ? 'bg-app-white text-app-black' : 'text-app-marineblue hover:bg-gray-50'}`}>
              <CalendarDays className="w-5 h-5 text-app-black" /> {isOwner ? 'Ver Agendas' : 'Mi Agenda'}
            </button>
            <button onClick={() => setPestañaActiva('historial')} className={`flex items-center gap-2 text-left px-4 py-3 rounded-xl font-medium transition-colors ${pestañaActiva === 'historial' ? 'bg-app-white text-app-black' : 'text-app-marineblue hover:bg-gray-50'}`}>
              <RotateCcwClock className="w-5 h-5 text-app-black" /> {isOwner ? 'Historial de Turnos' : 'Mi Historial'}
            </button>
            <button onClick={() => setPestañaActiva('reportes')} className={`flex items-center gap-2 text-left px-4 py-3 rounded-xl font-medium transition-colors ${pestañaActiva === 'reportes' ? 'bg-app-white text-app-black' : 'text-app-marineblue hover:bg-gray-50'}`}>
              <ChartPie className="w-5 h-5 text-app-black" /> {isOwner ? 'Estadísticas y Reportes' : 'Mis Estadísticas'}
            </button>
          </nav>

          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6">
            <h2 className="font-bold text-app-black mb-4">Información Comercial</h2>
            <div className="space-y-3 text-sm text-app-black mb-4 border-b border-gray-100 pb-4">
              <p><strong className="text-app-marineblue">Dirección:</strong> {negocio?.direccion}</p>
              <p><strong className="text-app-marineblue">Teléfono:</strong> {negocio?.telefono}</p>
              {negocio?.emailContacto && <p><strong className="text-app-marineblue">Email:</strong> {negocio.emailContacto}</p>}
            </div>

            <div>
              <button onClick={() => setMostrarHorarios(!mostrarHorarios)} className="w-full flex justify-between items-center text-sm font-bold text-app-black hover:text-app-marineblue transition-colors py-2">
                <span>Horarios de Atención</span>
                <ChevronDown className={`w-5 h-5 transform transition-transform duration-200 ${mostrarHorarios ? 'rotate-180' : ''}`} />
              </button>

              {mostrarHorarios && (
                <ul className="space-y-2 text-sm text-app-marineblue mt-3 animate-fadeIn bg-app-white p-4 rounded-xl">
                  {negocio?.horarios?.length > 0 ? (
                    ordenarHorariosPorDia(negocio.horarios).map((h, idx) => (
                      <li key={idx} className="flex justify-between items-start py-2 border-b border-white last:border-0">
                        <span className="font-semibold text-app-black w-24 flex-shrink-0">{DIAS_ES[h.diaSemana]}</span>
                        <div className="text-right flex-1 flex flex-col items-end justify-center">
                          {h.activo ? (
                            <>
                              {/* NUEVA LÓGICA DE HORARIOS PARTIDOS CON FLEXBOX */}
                              {(h.horaInicioDescanso && h.horaFinDescanso) ? (
                                <div className="flex flex-col items-end gap-1 leading-tight">
                                  <span className="whitespace-nowrap">{h.horaInicio?.slice(0, 5)} - {h.horaInicioDescanso?.slice(0, 5)}</span>
                                  <span className="whitespace-nowrap">{h.horaFinDescanso?.slice(0, 5)} - {h.horaFin?.slice(0, 5)}</span>
                                </div>
                              ) : (
                                <span className="whitespace-nowrap">{h.horaInicio?.slice(0, 5)} - {h.horaFin?.slice(0, 5)}</span>
                              )}
                            </>
                          ) : <span className="text-gray-400 italic py-0.5">Cerrado</span>}
                        </div>
                      </li>
                    ))
                  ) : <li className="text-gray-400 italic">No hay horarios configurados.</li>}
                </ul>
              )}
            </div>

            <div className="mt-6 pt-6 border-t border-gray-100 flex flex-col gap-3">
              {isOwner && (
                <button onClick={() => setIsFormNegocioOpen(true)} className="w-full bg-app-marineblue hover:bg-app-black/90 text-white py-2.5 rounded-xl text-sm font-bold shadow-sm transition-colors flex items-center justify-center gap-2 duration-300">
                  <Edit className="w-4 h-4" /> Editar Negocio
                </button>
              )}
              <div className="flex gap-2">
                <Link to={`/negocio/${negocio.alias}`} target="_blank" className="flex-1 flex justify-center items-center gap-2 bg-app-white border border-gray-200 text-app-black hover:bg-app-black/20 py-2.5 rounded-xl text-sm font-bold transition-colors duration-300">
                  <ExternalLink className="w-4 h-4" /> Perfil Público
                </Link>
                <button onClick={handleCopiarLink} className="bg-app-white border border-gray-200 text-app-marineblue px-4 rounded-xl hover:bg-app-marineblue hover:text-white transition-colors duration-300" title="Copiar link al portapapeles">
                  <Copy className="w-5 h-5" />
                </button>
              </div>
            </div>

          </div>
        </aside>

        {/* CONTENIDO PRINCIPAL */}
        <section className="lg:col-span-3 lg:overflow-y-auto lg:h-full lg:pr-2 lg:pb-8">
          {(pestañaActiva === 'configuracion' && isOwner) && (
            <div className="animate-fadeIn">
              <header className="flex justify-between items-center mb-6">
                <h2 className="font-bold text-xl text-app-black">Recursos (Staff / Espacios)</h2>
                {!isFormRecursoOpen && (
                  <button onClick={() => { setEditingRecurso(null); setIsFormRecursoOpen(true); }} className="bg-app-marineblue text-white px-5 py-2.5 rounded-xl text-sm font-medium hover:bg-app-black/90 transition-colors shadow-sm">
                    + Nuevo Recurso
                  </button>
                )}
              </header>

              {isFormRecursoOpen && (
                <RecursoForm 
                  key={editingRecurso?.id ?? 'nuevo'}
                  initialData={editingRecurso} 
                  negocioHorarios={negocio?.horarios}
                  isOwner={isOwner}
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
                      onDeleteRecurso={(id, nombre) => setRecursoAEliminar({ id, nombre })}
                      onSaveServicio={async (formData, editingId, idRecurso) => {
                         const res = await guardarServicio(formData, editingId, idRecurso);
                         if (res.success) addToast('Servicio guardado exitosamente.', 'success');
                         else addToast(res.message, 'error');
                      }}
                      onDeleteServicio={(id, nombre) => setServicioAEliminar({ id, nombre })}
                    />
                  ))
                ) : !isFormRecursoOpen && (
                  <div className="text-center py-16 bg-white rounded-2xl border border-dashed border-gray-200">
                    <UserRoundPlus className="h-12 w-12 mx-auto mb-3 " />
                    <h3 className="text-app-marineblue font-medium text-lg">Aún no tenés recursos activos</h3>
                    <p className="text-gray-400 text-sm mt-1">Agregá uno para empezar a cargar servicios y habilitar tu agenda.</p>
                  </div>
                )}
              </div>
            </div>
          )}

          {pestañaActiva === 'agenda' && <div className="animate-fadeIn"><AgendaTab recursos={isOwner ? recursosActivos : recursosActivos.filter(r => r.id === miRecursoId)} /></div>}
          {pestañaActiva === 'historial' && <div className="animate-fadeIn"><HistorialTab negocioId={isOwner ? id : null} recursoId={isOwner ? null : miRecursoId} recursos={recursosTotales} /></div>}
          {pestañaActiva === 'reportes' && <div className="animate-fadeIn"><ReportesTab negocioId={id} recursos={recursosTotales} esOwner={isOwner} miRecursoId={miRecursoId} /></div>}
        </section>
      </div>

      <ModalConfirmacion 
        isOpen={!!recursoAEliminar}
        titulo="Eliminar Recurso"
        mensaje={<>¿Eliminar el recurso <strong>{recursoAEliminar?.nombre}</strong> y todos sus servicios?</>}
        textoConfirmar="Sí, eliminar"
        esPeligroso={true}
        isConfirming={isProcessing}
        onClose={() => setRecursoAEliminar(null)}
        onConfirm={handleConfirmarEliminarRecurso}
      />

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

export default GestionNegocio;
