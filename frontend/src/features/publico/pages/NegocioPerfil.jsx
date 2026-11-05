import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useNegocioPublico } from '../hooks/useNegocioPublico';
import ModalReserva from '../components/ModalReserva';
import { MapPinned, Phone, Clock, ChevronDown, FaceSlightlyFrowning } from 'lucide-react';
import { DIAS_ES, ordenarHorariosPorDia } from '../../../utils/fechas'; // Ruta a tu archivo de utilidades

const NegocioPerfil = () => {
  const { id } = useParams();
  const { negocio, recursos, loading, error } = useNegocioPublico(id);

  const [modalAbierto, setModalAbierto] = useState(false);
  const [servicioSeleccionado, setServicioSeleccionado] = useState(null);
  const [recursoSeleccionadoId, setRecursoSeleccionadoId] = useState(null);
  
  const [mostrarHorarios, setMostrarHorarios] = useState(false);

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[calc(100vh-4rem)] bg-app-white">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-app-marineblue"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[calc(100vh-4rem)] px-4 bg-app-white">
        <span className="text-5xl mb-8"><FaceSlightlyFrowning /></span>
        <h2 className="text-2xl font-bold text-app-black mb-2">¡Ups!</h2>
        <p className="text-app-black text-center mb-6">{error}</p>
        <Link to="/" className="bg-app-marineblue text-white px-6 py-3 rounded-xl font-medium hover:bg-app-black/90 transition-colors shadow-sm duration-300">
          Volver al inicio
        </Link>
      </div>
    );
  }

  return (
    <div className="bg-app-white min-h-[calc(100vh-4rem)] pb-12 relative">
      
      <div className="bg-white border-b border-gray-100 pt-12 pb-8 px-4 sm:px-6 lg:px-8 shadow-sm">
        <div className="max-w-4xl mx-auto flex flex-col sm:flex-row items-center sm:items-start gap-6">
          <div className="h-24 w-24 sm:h-32 sm:w-32 bg-app-marineblue text-white rounded-full flex items-center justify-center text-5xl font-bold shadow-md shrink-0">
            {negocio?.nombre.charAt(0).toUpperCase()}
          </div>
          
          <div className="text-center sm:text-left flex-1">
            <h1 className="text-3xl sm:text-4xl font-extrabold text-app-marineblue mb-2">
              {negocio?.nombre}
            </h1>
            <p className="text-app-black text-lg mb-4">
              {negocio?.descripcion }
            </p>
            
            <div className="flex flex-col sm:flex-row gap-3 text-sm text-app-black/70 justify-center sm:justify-start font-medium">
              <span className="flex items-center justify-center sm:justify-start gap-2">
                <MapPinned className="w-5 h-5" /> {negocio?.direccion}
              </span>
              <span className="hidden sm:inline text-black-300">•</span>
              <span className="flex items-center justify-center sm:justify-start gap-2">
                <Phone className="w-5 h-5" /> {negocio?.telefono}
              </span>
            </div>

            <div className="mt-5 relative inline-block">
              <button 
                onClick={() => setMostrarHorarios(!mostrarHorarios)}
                className="inline-flex items-center gap-2 text-sm font-bold text-app-marineblue hover:text-app-black transition-colors bg-app-white px-4 py-2 rounded-xl border border-gray-100"
              >
                <Clock className="w-4 h-4" />
                Horarios de Atención
                <ChevronDown className={`w-4 h-4 transition-transform ${mostrarHorarios ? 'rotate-180' : ''}`} />
              </button>

              {mostrarHorarios && (
                <div className="absolute top-full left-1/2 sm:left-0 -translate-x-1/2 sm:translate-x-0 mt-2 w-72 sm:w-80 z-50">
                  <ul className="w-full space-y-2 text-sm text-app-marineblue animate-fadeIn bg-white border border-gray-100 p-4 rounded-xl shadow-xl">
                    {negocio?.horarios?.length > 0 ? (
                      ordenarHorariosPorDia(negocio.horarios).map((h, idx) => (
                        <li key={idx} className="flex justify-between items-start py-2 border-b border-gray-100 last:border-0">
                          <span className="font-semibold text-app-black w-24 flex-shrink-0">
                            {DIAS_ES[h.diaSemana] || h.diaSemana}
                          </span>
                          <div className="text-right flex-1 flex flex-col items-end justify-center">
                            {h.activo ? (
                              <>
                                {/* NUEVA LÓGICA DE HORARIOS PARTIDOS CON FLEXBOX Y WHITESPACE-NOWRAP */}
                                {(h.horaInicioDescanso && h.horaFinDescanso) ? (
                                  <div className="flex flex-col items-end gap-1 leading-tight">
                                    <span className="whitespace-nowrap font-medium">
                                      {h.horaInicio?.slice(0, 5)} - {h.horaInicioDescanso?.slice(0, 5)}
                                    </span>
                                    <span className="whitespace-nowrap font-medium">
                                      {h.horaFinDescanso?.slice(0, 5)} - {h.horaFin?.slice(0, 5)}
                                    </span>
                                  </div>
                                ) : (
                                  <span className="whitespace-nowrap font-medium">
                                    {h.horaInicio?.slice(0, 5)} - {h.horaFin?.slice(0, 5)}
                                  </span>
                                )}
                              </>
                            ) : (
                              <span className="text-gray-400 italic py-0.5">Cerrado</span>
                            )}
                          </div>
                        </li>
                      ))
                    ) : (
                      <li className="text-gray-400 italic text-center py-2">No hay horarios configurados.</li>
                    )}
                  </ul>
                </div>
              )}
            </div>

          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 mt-10">
        
        {!recursoSeleccionadoId ? (
          <div className="animate-fadeIn">
            <h2 className="text-2xl font-bold text-app-black mb-6">Elegí con quién o qué reservar</h2>
            {recursos.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
                {recursos.map(recurso => (
                  <button 
                    key={recurso.id}
                    onClick={() => setRecursoSeleccionadoId(recurso.id)}
                    className="bg-white p-6 rounded-2xl shadow-sm border border-transparent hover:border-app-marineblue hover:shadow-md transition-all text-left flex flex-col items-start group"
                  >
                    <h3 className="text-xl font-bold text-app-black transition-colors">{recurso.nombre}</h3>
                    {recurso.descripcion && <p className="text-sm text-app-black/70 mt-2">{recurso.descripcion}</p>}
                    <span className="mt-5 text-sm font-bold text-app-marineblue bg-app-white px-4 py-2 rounded-xl hover:bg-app-marineblue hover:text-white transition-colors">
                      Ver servicios
                    </span>
                  </button>
                ))}
              </div>
            ) : (
              <div className="text-center py-16 bg-white rounded-2xl shadow-sm border border-gray-100">
                <h3 className="text-app-marineblue font-medium">Este negocio aún no tiene opciones disponibles.</h3>
              </div>
            )}
          </div>
        ) : (
          
          <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden animate-fadeIn">
            <div className="bg-app-white px-6 py-5 border-b border-gray-100 flex items-center gap-4">
              <button 
                onClick={() => setRecursoSeleccionadoId(null)}
                className="p-2 hover:bg-white rounded-full transition-colors text-app-marineblue shadow-sm"
                title="Volver a recursos"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
              </button>
              <div>
                <h2 className="text-lg font-bold text-app-black">
                  Servicios de {recursos.find(r => r.id === recursoSeleccionadoId)?.nombre}
                </h2>
              </div>
            </div>

            <div className="divide-y divide-gray-100">
              {recursos.find(r => r.id === recursoSeleccionadoId)?.servicios?.length > 0 ? (
                recursos.find(r => r.id === recursoSeleccionadoId).servicios.map(servicio => (
                  <div key={servicio.id} className="p-6 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 hover:bg-gray-50 transition-colors">
                    <div>
                      <h4 className="font-bold text-app-marineblue text-lg">{servicio.nombre}</h4>
                      <div className="flex items-center gap-2 mt-1 text-sm text-app-black">
                        <span className="flex items-center gap-1.5">
                          <Clock className="w-4 h-4" /> 
                          <span>{servicio.duracionMinutos} min</span>
                        </span>
                        {servicio.descripcion && (
                          <>
                            <span>•</span>
                            <span>{servicio.descripcion}</span>
                          </>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-6 w-full sm:w-auto justify-between sm:justify-end mt-4 sm:mt-0">
                      <span className="text-2xl font-bold text-app-black">${Number(servicio.precio).toLocaleString('es-AR')}</span>
                      <button 
                        className="bg-app-marineblue text-white hover:bg-app-black/90 px-6 py-3 rounded-xl font-bold transition-colors shadow-sm"
                        onClick={() => {
                          setServicioSeleccionado(servicio);
                          setModalAbierto(true);
                        }}
                      >
                        Reservar
                      </button>
                    </div>
                  </div>
                ))
              ) : (
                <div className="p-10 text-center text-sm text-app-marineblue italic">No hay servicios cargados para esta opción.</div>
              )}
            </div>
          </div>
        )}
      </div>

      {servicioSeleccionado && (
        <ModalReserva 
          isOpen={modalAbierto} 
          onClose={() => setModalAbierto(false)} 
          servicio={servicioSeleccionado} 
          recursoId={recursoSeleccionadoId}
          recursoHorarios={negocio?.horarios} 
        />
      )}
    </div>
  );
};

export default NegocioPerfil;