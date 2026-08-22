import { useState, useEffect } from 'react';
import { useHistorialNegocio } from '../hooks/useHistorialNegocio';
import { useHistorialRecurso } from '../hooks/useHistorialRecurso';
import { esHoy } from '../../../utils/fechas';
import api from '../../../services/api'; 

const HistorialTab = ({ negocioId, recursoId, recursos = [] }) => {
  
  const dataNegocio = useHistorialNegocio(negocioId);
  const dataRecurso = useHistorialRecurso(recursoId);

  const dataActiva = recursoId ? dataRecurso : dataNegocio;
  const { turnos, loading, paginaActual, setPaginaActual, totalPaginas, totalTurnos } = dataActiva;
  
  const [filtro, setFiltro] = useState('todos');
  const [filtroRecurso, setFiltroRecurso] = useState('todos');
  const [turnoDetalle, setTurnoDetalle] = useState(null);
  
  // ESTADOS PARA CANCELACIÓN
  const [turnoACancelar, setTurnoACancelar] = useState(null);
  const [isCancelando, setIsCancelando] = useState(false);
  
  // Estado local para turnos mostrados (permite actualización optimista)
  const [turnosLocales, setTurnosLocales] = useState([]);

  useEffect(() => {
     setTurnosLocales(turnos || []);
  }, [turnos]);


  const obtenerLimiteDias = (dias) => {
    const d = new Date();
    d.setHours(0, 0, 0, 0);
    d.setDate(d.getDate() - dias);
    return d;
  };

  const turnosMostrados = (turnosLocales).filter(t => {
    if (filtroRecurso !== 'todos' && t.nombreRecurso !== filtroRecurso) return false;

    if (filtro === 'hoy') return esHoy(t.fechaHoraInicio);
    
    const fecha = new Date(t.fechaHoraInicio);
    if (filtro === '7dias') return fecha >= obtenerLimiteDias(7);
    if (filtro === '30dias') return fecha >= obtenerLimiteDias(30);
    
    return true;
  });

  const handleCancelarTurno = async () => {
    if (!turnoACancelar) return;
    setIsCancelando(true);
    
    try {
      await api.delete(`/turnos/${turnoACancelar.id}`);
      
      // Actualización optimista: marcamos el turno como cancelado localmente
      setTurnosLocales(prevTurnos => 
        prevTurnos.map(t => 
          t.id === turnoACancelar.id ? { ...t, estado: 'CANCELADO' } : t
        )
      );
      
      setTurnoACancelar(null);
    } catch (error) {
      alert(error.response?.data?.message || 'Error al cancelar el turno.');
    } finally {
      setIsCancelando(false);
    }
  };


  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 min-h-[500px] flex flex-col relative">
      <header className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-6">
        <div>
          <h2 className="text-xl font-bold text-gray-800">
            {recursoId ? 'Mi Historial de Turnos' : 'Historial Global del Negocio'}
          </h2>
          <p className="text-sm text-gray-500 mt-1">Total de registros históricos: {totalTurnos}</p>
        </div>
        
        <div className="flex flex-col sm:flex-row gap-3 w-full md:w-auto">
          {!recursoId && recursos.length > 0 && (
            <select 
              value={filtroRecurso}
              onChange={(e) => setFiltroRecurso(e.target.value)}
              className="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block px-3 py-2 cursor-pointer font-medium"
            >
              <option value="todos">Todos los recursos</option>
              {recursos.map(r => (
                <option key={r.id} value={r.nombre}>{r.nombre}</option>
              ))}
            </select>
          )}

          <select 
            value={filtro}
            onChange={(e) => setFiltro(e.target.value)}
            className="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block px-3 py-2 cursor-pointer font-medium"
          >
            <option value="todos">Fechas: Completo</option>
            <option value="hoy">Fechas: Solo Hoy</option>
            <option value="7dias">Fechas: Últimos 7 días</option>
            <option value="30dias">Fechas: Últimos 30 días</option>
          </select>
        </div>
      </header>

      {loading ? (
        <div className="flex-1 flex justify-center items-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      ) : (
        <>
          <div className="flex-1 overflow-x-auto">
            <table className="w-full text-left text-sm text-gray-500">
              <thead className="text-xs text-gray-700 uppercase bg-gray-50 border-b border-gray-200">
                <tr>
                  <th className="px-4 py-3">Fecha</th>
                  <th className="px-4 py-3">Hora</th>
                  <th className="px-4 py-3">Cliente</th>
                  <th className="px-4 py-3">Servicio</th>
                  {!recursoId && <th className="px-4 py-3">Recurso</th>}
                  <th className="px-4 py-3">Estado</th>
                  <th className="px-4 py-3 text-center">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {turnosMostrados.length > 0 ? (
                  turnosMostrados.map((turno) => {
                    const fecha = new Date(turno.fechaHoraInicio);
                    const esCancelado = turno.estado === 'CANCELADO';
                    return (
                      <tr key={turno.id} className="border-b border-gray-100 hover:bg-gray-50 transition-colors">
                        <td className="px-4 py-3 whitespace-nowrap font-semibold text-gray-900">
                          {fecha.toLocaleDateString('es-AR')}
                        </td>
                        <td className="px-4 py-3 whitespace-nowrap text-gray-600">
                          {fecha.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit', hour12: false })} hs
                        </td>
                        <td className="px-4 py-3 font-medium text-gray-900">
                          {turno.nombreCliente} {turno.apellidoCliente}
                        </td>
                        <td className="px-4 py-3">{turno.nombreServicio}</td>
                        
                        {!recursoId && <td className="px-4 py-3">{turno.nombreRecurso}</td>}
                        
                        <td className="px-4 py-3">
                          <span className={`px-2 py-1 rounded text-[10px] font-bold ${esCancelado ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`}>
                            {turno.estado}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-center">
                            {/* Dejamos solo el botón de detalles en la tabla */}
                            <button 
                                onClick={() => setTurnoDetalle(turno)}
                                className="text-blue-500 hover:text-blue-700 p-1.5 rounded-full hover:bg-blue-50 transition-colors inline-flex items-center justify-center"
                                title="Ver detalles y opciones"
                            >
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                            </button>
                        </td>
                      </tr>
                    );
                  })
                ) : (
                  <tr>
                    <td colSpan={recursoId ? "6" : "7"} className="px-4 py-8 text-center text-gray-500 italic">
                      No hay registros para mostrar con los filtros seleccionados.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {totalPaginas > 1 && (
            <div className="mt-6 pt-4 border-t border-gray-100 flex flex-col sm:flex-row justify-between items-center gap-4">
              <span className="text-sm text-gray-600">
                Página <span className="font-bold text-gray-900">{paginaActual + 1}</span> de <span className="font-bold text-gray-900">{totalPaginas}</span>
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => setPaginaActual(p => p - 1)}
                  disabled={paginaActual === 0}
                  className="px-4 py-2 border border-gray-300 rounded-lg text-sm font-medium hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Anterior
                </button>
                <button
                  onClick={() => setPaginaActual(p => p + 1)}
                  disabled={paginaActual >= totalPaginas - 1}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Siguiente
                </button>
              </div>
            </div>
          )}
        </>
      )}

      {/* Modal de Detalle con el botón de Cancelar integrado */}
      {turnoDetalle && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50 animate-fadeIn">
          <div className="bg-white rounded-xl max-w-md w-full shadow-2xl overflow-hidden">
            <div className={`px-6 py-4 flex justify-between items-center border-b ${
              turnoDetalle.estado === 'CANCELADO' ? 'bg-red-50 border-red-100' : 'bg-blue-50 border-blue-100'
            }`}>
              <h3 className="font-bold text-gray-900 text-lg flex items-center gap-2">
                <span>📋</span> Ficha del Turno
              </h3>
              <button onClick={() => setTurnoDetalle(null)} className="text-gray-400 hover:text-gray-600 hover:bg-gray-200/50 p-1 rounded-md">
                <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
              </button>
            </div>
            <div className="p-6 space-y-5">
              <div className="flex justify-between items-center pb-4 border-b border-gray-100">
                <div>
                  <span className="text-xs text-gray-500 block mb-1">Estado actual</span>
                  <span className={`px-2 py-1 rounded text-xs font-bold ${
                    turnoDetalle.estado === 'CANCELADO' ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'
                  }`}>{turnoDetalle.estado}</span>
                </div>
                <div className="text-right">
                  <span className="text-xs text-gray-500 block mb-1">Precio</span>
                  <span className="font-extrabold text-blue-600 text-lg">${turnoDetalle.precio}</span>
                </div>
              </div>
              <div className="grid grid-cols-3 gap-3">
                <div className="bg-gray-50 p-3 rounded-lg border border-gray-100">
                  <span className="text-xs text-gray-500 block mb-1">Día</span>
                  <p className="font-medium text-gray-900 text-sm">{new Date(turnoDetalle.fechaHoraInicio).toLocaleDateString('es-AR')}</p>
                </div>
                <div className="bg-gray-50 p-3 rounded-lg border border-gray-100">
                  <span className="text-xs text-gray-500 block mb-1">Hora</span>
                  <p className="font-medium text-gray-900 text-sm">{new Date(turnoDetalle.fechaHoraInicio).toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit', hour12: false })} hs</p>
                </div>
                <div className="bg-gray-50 p-3 rounded-lg border border-gray-100">
                  <span className="text-xs text-gray-500 block mb-1">Atendido por</span>
                  <p className="font-medium text-gray-900 text-sm truncate" title={turnoDetalle.nombreRecurso}>{turnoDetalle.nombreRecurso}</p>
                </div>
              </div>
              <div className="space-y-3 pt-4 border-t border-gray-100">
                <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider">Cliente y Servicio</h4>
                <div className="flex items-center gap-2 text-sm text-gray-700"><span className="w-5 text-center">🏷️</span><span className="font-medium">{turnoDetalle.nombreServicio}</span></div>
                <div className="flex items-center gap-2 text-sm text-gray-700"><span className="w-5 text-center">👤</span><span className="font-medium">{turnoDetalle.nombreCliente} {turnoDetalle.apellidoCliente}</span></div>
                {turnoDetalle.telefonoCliente && (
                  <div className="flex items-center gap-2 text-sm text-gray-700">
                    <span className="w-5 text-center">📱</span>
                    <a href={`https://wa.me/${turnoDetalle.telefonoCliente.replace(/\D/g, '')}`} target="_blank" rel="noreferrer" className="text-blue-600 hover:underline">{turnoDetalle.telefonoCliente}</a>
                  </div>
                )}
              </div>

              {/* Botón de cancelar turno integrado en la ficha */}
              {turnoDetalle.estado !== 'CANCELADO' && (
                <div className="pt-4 border-t border-gray-100 flex justify-end">
                  <button 
                    onClick={() => {
                      setTurnoACancelar(turnoDetalle);
                      setTurnoDetalle(null); // Cerramos la ficha para abrir la confirmación
                    }}
                    className="px-4 py-2 w-full sm:w-auto bg-red-50 text-red-600 hover:bg-red-100 hover:text-red-700 rounded-lg text-sm font-bold transition-colors flex items-center justify-center gap-2 border border-red-200"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
                    </svg>
                    Marcar Inasistencia / Cancelar
                  </button>
                </div>
              )}

            </div>
          </div>
        </div>
      )}

      {/* Modal de Confirmación de Cancelación */}
      {turnoACancelar && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50 animate-fadeIn">
          <div className="bg-white rounded-xl max-w-sm w-full p-6 shadow-2xl">
            <div className="flex items-center justify-center w-12 h-12 rounded-full bg-red-100 mx-auto mb-4">
              <svg className="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>

            <h3 className="text-lg font-bold text-gray-900 text-center mb-2">¿Cancelar este turno?</h3>
            <p className="text-sm text-gray-500 text-center mb-6">
              Estás a punto de cancelar el turno de <strong>{turnoACancelar.nombreCliente} {turnoACancelar.apellidoCliente}</strong>. Esta acción no se puede deshacer.
            </p>

            <div className="flex gap-3">
              <button 
                onClick={() => setTurnoACancelar(null)}
                disabled={isCancelando}
                className="flex-1 py-2 text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg font-medium transition-colors"
              >
                No, volver
              </button>
              <button 
                onClick={handleCancelarTurno}
                disabled={isCancelando}
                className={`flex-1 py-2 rounded-lg font-medium transition-colors ${
                  isCancelando ? 'bg-red-400 text-white cursor-not-allowed' : 'bg-red-600 text-white hover:bg-red-700'
                }`}
              >
                {isCancelando ? 'Cancelando...' : 'Sí, cancelar'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default HistorialTab;