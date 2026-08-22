import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useNegocioPublico } from '../hooks/useNegocioPublico';
import ModalReserva from '../components/ModalReserva';

const NegocioPerfil = () => {
  const { id } = useParams();
  
  // Extraemos toda la lógica a nuestro Custom Hook
  const { negocio, recursos, loading, error } = useNegocioPublico(id);

  // Estados exclusivos de la UI
  const [modalAbierto, setModalAbierto] = useState(false);
  const [servicioSeleccionado, setServicioSeleccionado] = useState(null);
  const [recursoSeleccionadoId, setRecursoSeleccionadoId] = useState(null);

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[calc(100vh-4rem)]">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[calc(100vh-4rem)] px-4">
        <svg className="h-16 w-16 text-gray-400 mb-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
        </svg>
        <h2 className="text-2xl font-bold text-gray-800 mb-2">¡Ups!</h2>
        <p className="text-gray-600 text-center mb-6">{error}</p>
        <Link to="/" className="bg-blue-600 text-white px-6 py-2 rounded-lg font-medium hover:bg-blue-700 transition-colors">
          Volver al inicio
        </Link>
      </div>
    );
  }

  return (
    <div className="bg-gray-50 min-h-[calc(100vh-4rem)] pb-12 relative">
      
      {/* HEADER DEL NEGOCIO */}
      <div className="bg-white border-b border-gray-200 pt-12 pb-8 px-4 sm:px-6 lg:px-8">
        <div className="max-w-4xl mx-auto flex flex-col sm:flex-row items-center sm:items-start gap-6">
          <div className="h-24 w-24 sm:h-32 sm:w-32 bg-blue-100 text-blue-600 rounded-full flex items-center justify-center text-4xl font-bold shadow-sm flex-shrink-0">
            {negocio?.nombre.charAt(0).toUpperCase()}
          </div>
          
          <div className="text-center sm:text-left flex-1">
            <h1 className="text-3xl sm:text-4xl font-extrabold text-gray-900 mb-2">
              {negocio?.nombre}
            </h1>
            <p className="text-gray-600 text-lg mb-4">
              {negocio?.descripcion || 'Sin descripción disponible.'}
            </p>
            
            <div className="flex flex-col sm:flex-row gap-3 text-sm text-gray-500 justify-center sm:justify-start">
              <span className="flex items-center justify-center sm:justify-start gap-1">
                📍 {negocio?.direccion}
              </span>
              <span className="hidden sm:inline">•</span>
              <span className="flex items-center justify-center sm:justify-start gap-1">
                📞 {negocio?.telefono}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 mt-8">
        
        {/* PASO 1: SELECCIONAR RECURSO */}
        {!recursoSeleccionadoId ? (
          <>
            <h2 className="text-2xl font-bold text-gray-900 mb-6">Elegí con quién o qué reservar</h2>
            {recursos.length > 0 ? (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {recursos.map(recurso => (
                  <button 
                    key={recurso.id}
                    onClick={() => setRecursoSeleccionadoId(recurso.id)}
                    className="bg-white p-6 rounded-xl shadow-sm border border-gray-200 hover:border-blue-500 hover:shadow-md transition-all text-left flex flex-col items-start group"
                  >
                    <h3 className="text-xl font-bold text-gray-900 group-hover:text-blue-600 transition-colors">{recurso.nombre}</h3>
                    {recurso.descripcion && <p className="text-sm text-gray-500 mt-2">{recurso.descripcion}</p>}
                    <span className="mt-4 text-sm font-semibold text-blue-600">Ver servicios &rarr;</span>
                  </button>
                ))}
              </div>
            ) : (
              <div className="text-center py-12 bg-white rounded-xl border border-gray-100">
                <h3 className="text-gray-500 font-medium">Este negocio aún no tiene opciones disponibles.</h3>
              </div>
            )}
          </>
        ) : (
          
          /* PASO 2: SELECCIONAR SERVICIO DEL RECURSO */
          <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden animate-fadeIn">
            <div className="bg-gray-50 px-6 py-4 border-b border-gray-100 flex items-center gap-4">
              <button 
                onClick={() => setRecursoSeleccionadoId(null)}
                className="p-2 hover:bg-gray-200 rounded-full transition-colors text-gray-500"
                title="Volver a recursos"
              >
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
              </button>
              <div>
                <h2 className="text-lg font-bold text-gray-900">
                  Servicios de {recursos.find(r => r.id === recursoSeleccionadoId)?.nombre}
                </h2>
              </div>
            </div>

            <div className="divide-y divide-gray-100">
              {recursos.find(r => r.id === recursoSeleccionadoId)?.servicios?.length > 0 ? (
                recursos.find(r => r.id === recursoSeleccionadoId).servicios.map(servicio => (
                  <div key={servicio.id} className="p-6 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 hover:bg-gray-50 transition-colors">
                    <div>
                      <h4 className="font-bold text-gray-900 text-lg">{servicio.nombre}</h4>
                      <div className="flex items-center gap-2 mt-1 text-sm text-gray-500">
                        <span>⏱ {servicio.duracionMinutos} min</span>
                        {servicio.descripcion && (
                          <><span>•</span><span>{servicio.descripcion}</span></>
                        )}
                      </div>
                    </div>
                    <div className="flex items-center gap-6 w-full sm:w-auto justify-between sm:justify-end mt-2 sm:mt-0">
                      <span className="text-xl font-extrabold text-blue-600">${servicio.precio}</span>
                      <button 
                        className="bg-blue-600 text-white hover:bg-blue-700 px-6 py-2 rounded-lg font-semibold transition-colors shadow-sm"
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
                <div className="p-6 text-sm text-gray-400 italic">No hay servicios cargados para esta opción.</div>
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
        />
      )}
      
    </div>
  );
};

export default NegocioPerfil;