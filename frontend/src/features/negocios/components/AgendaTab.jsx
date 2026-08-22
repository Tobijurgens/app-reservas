import { useState, useEffect } from 'react';
import { useAgendaRecurso } from '../hooks/useAgendaRecurso';
import { esFuturo, esPasado, esHoy, ordenarPorFechaAsc, ordenarPorFechaDesc } from '../../../utils/fechas';

const AgendaTab = ({ recursos }) => {
  const [recursoSeleccionadoId, setRecursoSeleccionadoId] = useState('');
  
  // AUTO-SELECCIÓN: Si el array trae 1 solo recurso (ej. Empleado), lo seleccionamos por defecto
  useEffect(() => {
    if (recursos.length === 1 && !recursoSeleccionadoId) {
      setRecursoSeleccionadoId(recursos[0].id);
    }
  }, [recursos, recursoSeleccionadoId]);

  const { turnos, loading, cancelarTurno } = useAgendaRecurso(recursoSeleccionadoId);

  const [vistaActual, setVistaActual] = useState('proximos');
  const [filtroProximos, setFiltroProximos] = useState('todos');
  const [turnoACancelar, setTurnoACancelar] = useState(null);
  const [isCancelando, setIsCancelando] = useState(false);

  if (recursos.length === 0) {
    return (
      <div className="text-center py-16 bg-white rounded-xl border border-dashed border-gray-300">
        <span className="text-4xl block mb-3">👥</span>
        <p className="text-gray-600 font-medium text-lg">No hay recursos configurados</p>
        <p className="text-gray-400 text-sm mt-1">Primero agregá empleados o espacios para ver sus agendas.</p>
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
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 min-h-[500px] relative">
      
      <header className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6 pb-6 border-b border-gray-100">
        <div>
          <h2 className="text-xl font-bold text-gray-800">Agendas de Turnos</h2>
          <p className="text-sm text-gray-500 mt-1">
            {recursos.length > 1 ? 'Seleccioná un recurso para ver sus reservas.' : 'Revisá tus reservas programadas.'}
          </p>
        </div>
        
        {/* MAGIA DE UI: Ocultamos el select si es solo un empleado */}
        {recursos.length > 1 ? (
          <select 
            value={recursoSeleccionadoId} 
            onChange={(e) => setRecursoSeleccionadoId(Number(e.target.value))}
            className="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block px-4 py-2 font-medium cursor-pointer"
          >
            <option value="" disabled>Seleccioná un recurso...</option>
            {recursos.map(r => (
              <option key={r.id} value={r.id}>{r.nombre}</option>
            ))}
          </select>
        ) : recursos.length === 1 ? (
          <div className="bg-blue-50 border border-blue-200 text-blue-800 text-sm rounded-lg px-4 py-2 font-medium shadow-sm">
            👤 {recursos[0].nombre}
          </div>
        ) : null}
      </header>

      {!recursoSeleccionadoId ? (
        <div className="text-center py-16 bg-gray-50 rounded-lg border border-dashed border-gray-300">
          <span className="text-4xl block mb-3">📅</span>
          <p className="text-gray-600 font-medium text-lg">Seleccioná un recurso arriba</p>
          <p className="text-gray-400 text-sm mt-1">Para visualizar y gestionar los turnos de su agenda.</p>
        </div>
      ) : loading ? (
        <div className="flex justify-center py-12"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div></div>
      ) : (
        <>
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
            <div className="flex gap-2">
              <button 
                onClick={() => setVistaActual('proximos')}
                className={`px-4 py-1.5 rounded-full text-sm font-medium transition-colors ${vistaActual === 'proximos' ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}
              >
                Próximos
              </button>
              <button 
                onClick={() => setVistaActual('historial')}
                className={`px-4 py-1.5 rounded-full text-sm font-medium transition-colors ${vistaActual === 'historial' ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-600 hover:bg-gray-200'}`}
              >
                Historial
              </button>
            </div>

            {vistaActual === 'proximos' && (
              <select 
                value={filtroProximos}
                onChange={(e) => setFiltroProximos(e.target.value)}
                className="bg-gray-50 border border-gray-300 text-gray-900 text-xs rounded-lg block px-3 py-1.5 cursor-pointer"
              >
                <option value="todos">Ver: Todos</option>
                <option value="hoy">Ver: Solo hoy</option>
              </select>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {vistaActual === 'proximos' && (
              turnosProximos.length > 0 ? (
                turnosProximos.map(turno => {
                  const fecha = new Date(turno.fechaHoraInicio);
                  return (
                    <article key={turno.id} className="p-4 rounded-xl border bg-white border-blue-200 shadow-sm flex flex-col justify-between">
                      <div>
                        <div className="flex justify-between items-start mb-2">
                          <span className="text-xs font-bold px-2 py-0.5 rounded bg-blue-100 text-blue-800">{turno.estado}</span>
                          <span className="text-xs font-semibold text-gray-500">
                            {fecha.toLocaleDateString('es-AR', { weekday: 'short', day: 'numeric', month: 'short' })}
                          </span>
                        </div>
                        <h3 className="font-bold text-gray-900">{turno.nombreServicio}</h3>
                        
                        <div className="mt-3 space-y-1 text-sm text-gray-600">
                          <p>🕒 {fecha.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' })} hs</p>
                          <p>👤 {turno.nombreCliente} {turno.apellidoCliente}</p>
                          {turno.telefonoCliente && <p>📱 {turno.telefonoCliente}</p>}
                        </div>
                      </div>
                      <div className="mt-4 pt-3 border-t border-gray-100">
                        <button 
                          onClick={() => setTurnoACancelar(turno)}
                          className="w-full text-xs text-red-600 hover:bg-red-50 border border-red-200 px-3 py-1.5 rounded-lg transition-colors font-medium"
                        >
                          Cancelar
                        </button>
                      </div>
                    </article>
                  );
                })
              ) : (
                <div className="col-span-full text-center py-8 bg-blue-50 rounded-lg border border-dashed border-blue-200">
                  <p className="text-blue-800 text-sm font-medium">No hay turnos próximos asignados a este recurso.</p>
                </div>
              )
            )}

            {vistaActual === 'historial' && (
              turnosHistorial.length > 0 ? (
                turnosHistorial.map(turno => {
                  const fecha = new Date(turno.fechaHoraInicio);
                  const esCancelado = turno.estado === 'CANCELADO';
                  return (
                    <article key={turno.id} className={`p-3 rounded-xl border ${esCancelado ? 'bg-red-50 border-red-100' : 'bg-gray-50 border-gray-200'}`}>
                      <div className="flex items-center gap-2 mb-1">
                        <span className={`text-[10px] font-bold px-1.5 py-0.5 rounded ${esCancelado ? 'bg-red-100 text-red-700' : 'bg-gray-200 text-gray-700'}`}>{turno.estado}</span>
                        <span className="text-xs font-semibold text-gray-500">
                          {fecha.toLocaleDateString('es-AR')} - {fecha.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit' })} hs
                        </span>
                      </div>
                      <h3 className="font-bold text-gray-700 text-sm">{turno.nombreServicio}</h3>
                      <p className="text-xs text-gray-500 mt-1">👤 {turno.nombreCliente} {turno.apellidoCliente}</p>
                    </article>
                  );
                })
              ) : (
                <div className="col-span-full text-center py-8 bg-gray-50 rounded-lg border border-dashed border-gray-200">
                  <p className="text-gray-500 text-sm">El historial está vacío.</p>
                </div>
              )
            )}
          </div>
        </>
      )}

      {turnoACancelar && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-xl max-w-sm w-full p-6 shadow-2xl">
            <h3 className="text-lg font-bold text-gray-900 text-center mb-2">¿Cancelar turno?</h3>
            <p className="text-sm text-gray-500 text-center mb-6">
              Cancelarás el turno de {turnoACancelar.nombreCliente}. Acción irreversible.
            </p>
            <div className="flex gap-3">
              <button onClick={() => setTurnoACancelar(null)} className="flex-1 py-2 text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg font-medium">Volver</button>
              <button 
                onClick={async () => {
                  setIsCancelando(true);
                  const exito = await cancelarTurno(turnoACancelar.id);
                  setIsCancelando(false);
                  if (exito) setTurnoACancelar(null);
                }}
                disabled={isCancelando}
                className="flex-1 py-2 bg-red-600 text-white hover:bg-red-700 rounded-lg font-medium"
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

export default AgendaTab;