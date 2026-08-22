import { useState } from 'react';
import { useAgendaPersonal } from '../hooks/useAgendaPersonal';
import { esFuturo, esPasado, esHoy, ordenarPorFechaAsc, ordenarPorFechaDesc } from '../../../utils/fechas';

const AgendaPersonal = () => {
  const { 
    recursosAsignados, 
    recursoSeleccionado, 
    setRecursoSeleccionado, 
    turnos, 
    loading,
    cancelarTurno 
  } = useAgendaPersonal();

  const [vistaActual, setVistaActual] = useState('proximos');
  const [filtroProximos, setFiltroProximos] = useState('todos');
  
  const [turnoACancelar, setTurnoACancelar] = useState(null);
  const [isCancelando, setIsCancelando] = useState(false);

  if (loading && recursosAsignados.length === 0) {
    return <div className="flex justify-center py-20"><div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600"></div></div>;
  }

  if (recursosAsignados.length === 0) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-12 text-center">
        <h2 className="text-2xl font-bold text-gray-800 mb-2">Mi Agenda de Trabajo</h2>
        <div className="bg-gray-50 p-8 rounded-xl border border-dashed border-gray-300 mt-6">
          <p className="text-gray-500 font-medium">No tenés ningún recurso o espacio de trabajo asignado a tu cuenta en este momento.</p>
          <p className="text-sm text-gray-400 mt-2">Si trabajás en algún negocio, pedile al administrador que agregue tu email en la configuración del recurso.</p>
        </div>
      </div>
    );
  }

  const turnosProximos = turnos
    .filter(t => esFuturo(t.fechaHoraInicio) && t.estado !== 'CANCELADO' && (filtroProximos === 'todos' || esHoy(t.fechaHoraInicio)))
    .sort(ordenarPorFechaAsc);

  const turnosHistorial = turnos
    .filter(t => esPasado(t.fechaHoraInicio) || t.estado === 'CANCELADO')
    .sort(ordenarPorFechaDesc);

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8 relative">
      <header className="mb-8 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-900">Mi Agenda</h1>
          <p className="text-sm text-gray-600 mt-1">Revisá tus turnos programados.</p>
        </div>

        <select 
          value={recursoSeleccionado || ''} 
          onChange={(e) => setRecursoSeleccionado(Number(e.target.value))}
          className="bg-white border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block px-4 py-2 font-medium shadow-sm cursor-pointer"
        >
          <option value="" disabled>Seleccioná un negocio...</option>
          {recursosAsignados.map(r => (
            <option key={r.id} value={r.id}>
              {r.nombreNegocio}
            </option>
          ))}
        </select>
      </header>

      <main className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 min-h-[400px]">
        {!recursoSeleccionado ? (
          <div className="text-center py-16 bg-gray-50 rounded-lg border border-dashed border-gray-300">
            <span className="text-4xl block mb-3">🏪</span>
            <p className="text-gray-600 font-medium text-lg">Seleccioná un negocio arriba</p>
            <p className="text-gray-400 text-sm mt-1">Para visualizar los turnos de la agenda.</p>
          </div>
        ) : loading ? (
           <div className="flex justify-center py-12"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div></div>
        ) : (
          <>
            <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-6 gap-4 border-b border-gray-100 pb-4">
              <h2 className="text-xl font-bold text-gray-800">
                {vistaActual === 'proximos' ? 'Próximos a atender' : 'Historial de Turnos'}
              </h2>

              <div className="flex flex-col sm:flex-row items-start sm:items-center gap-3">
                {vistaActual === 'proximos' && (
                  <div className="flex items-center gap-2">
                    <label className="text-sm font-medium text-gray-600">Ver:</label>
                    <select 
                      value={filtroProximos}
                      onChange={(e) => setFiltroProximos(e.target.value)}
                      className="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block px-3 py-1.5 cursor-pointer font-medium"
                    >
                      <option value="todos">Todos</option>
                      <option value="hoy">Solo hoy</option>
                    </select>
                  </div>
                )}

                <select 
                  value={vistaActual}
                  onChange={(e) => setVistaActual(e.target.value)}
                  className="bg-white border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block px-4 py-2 cursor-pointer font-medium shadow-sm"
                >
                  <option value="proximos">Turnos Próximos</option>
                  <option value="historial">Historial / Cancelados</option>
                </select>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-4">
              {vistaActual === 'proximos' && (
                turnosProximos.length > 0 ? (
                  turnosProximos.map(turno => {
                    const fecha = new Date(turno.fechaHoraInicio);
                    return (
                      <article key={turno.id} className="p-5 rounded-xl border bg-white border-blue-200 shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between">
                        <div>
                          <div className="flex justify-between items-start mb-3">
                            <span className="text-xs font-bold px-2 py-1 rounded-md bg-blue-100 text-blue-800">
                              {turno.estado}
                            </span>
                            <span className="text-sm font-semibold text-gray-600">
                              {fecha.toLocaleDateString('es-AR', { weekday: 'long', day: 'numeric', month: 'short' })}
                            </span>
                          </div>

                          <h3 className="font-bold text-gray-900 text-xl">{turno.nombreServicio}</h3>
                          <p className="text-lg font-extrabold text-blue-600 mt-1">${turno.precio}</p>

                          <div className="mt-4 pt-4 border-t border-gray-100 space-y-2 text-sm text-gray-600">
                            <p className="flex items-center gap-2">
                              <span className="text-lg">🕒</span> 
                              <span className="font-medium text-gray-900">
                                {fecha.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' })} hs
                              </span>
                            </p>
                            <p className="flex items-center gap-2">
                              <span className="text-lg">👤</span> 
                              <span className="font-medium">{turno.nombreCliente} {turno.apellidoCliente}</span>
                            </p>
                            <p className="flex items-center gap-2">
                              <span className="text-lg">📱</span> 
                              <span>{turno.telefonoCliente || 'No especificado'}</span>
                            </p>
                          </div>
                        </div>

                        <div className="mt-5 pt-3 border-t border-gray-100">
                          <button 
                            onClick={() => setTurnoACancelar(turno)}
                            className="w-full text-sm text-red-600 hover:text-white border border-red-600 hover:bg-red-600 px-4 py-2 rounded-lg transition-colors font-medium text-center"
                          >
                            Cancelar turno
                          </button>
                        </div>
                      </article>
                    );
                  })
                ) : (
                  <div className="col-span-full text-center py-10 bg-blue-50 rounded-lg border border-dashed border-blue-200">
                    <p className="text-blue-800 font-medium">
                      {filtroProximos === 'hoy' 
                        ? 'No tenés turnos programados para el resto del día de hoy.' 
                        : 'No tenés turnos próximos asignados.'}
                    </p>
                  </div>
                )
              )}

              {vistaActual === 'historial' && (
                turnosHistorial.length > 0 ? (
                  turnosHistorial.map(turno => {
                    const fecha = new Date(turno.fechaHoraInicio);
                    const esCancelado = turno.estado === 'CANCELADO';
                    return (
                      <article key={turno.id} className={`p-4 rounded-xl border flex flex-col justify-between ${esCancelado ? 'bg-red-50 border-red-100' : 'bg-gray-50 border-gray-200'}`}>
                        <div>
                            <div className="flex items-center gap-2 mb-2">
                            <span className={`text-xs font-bold px-2 py-1 rounded-md ${esCancelado ? 'bg-red-100 text-red-700' : 'bg-gray-200 text-gray-700'}`}>
                                {turno.estado}
                            </span>
                            <span className="text-sm font-semibold text-gray-500">
                                {fecha.toLocaleDateString('es-AR')} - {fecha.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' })} hs
                            </span>
                            </div>
                            <h3 className="font-bold text-gray-700">{turno.nombreServicio}</h3>
                            <p className="text-sm font-medium text-gray-600 mt-2">
                            👤 {turno.nombreCliente} {turno.apellidoCliente}
                            </p>
                        </div>
                        {/* NUEVO: Botón de cancelar en el historial si no está cancelado */}
                        {!esCancelado && (
                            <div className="mt-4 pt-3 border-t border-gray-100">
                                <button 
                                onClick={() => setTurnoACancelar(turno)}
                                className="w-full text-xs text-red-600 hover:bg-red-50 border border-red-200 px-3 py-1.5 rounded-lg transition-colors font-medium text-center"
                                >
                                Marcar como Inasistencia / Cancelar
                                </button>
                            </div>
                        )}
                      </article>
                    );
                  })
                ) : (
                  <div className="col-span-full text-center py-10 bg-gray-50 rounded-lg border border-dashed border-gray-200">
                    <p className="text-gray-500 text-sm">El historial está vacío.</p>
                  </div>
                )
              )}

            </div>
          </>
        )}
      </main>

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
                onClick={async () => {
                  setIsCancelando(true);
                  const exito = await cancelarTurno(turnoACancelar.id);
                  setIsCancelando(false);
                  if (exito) setTurnoACancelar(null);
                }}
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

export default AgendaPersonal;