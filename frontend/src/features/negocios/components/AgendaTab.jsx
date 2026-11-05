import { useState, useEffect } from 'react';
import { useAgendaRecurso } from '../hooks/useAgendaRecurso';
import { esFuturo, esPasado, esHoy, ordenarPorFechaAsc, ordenarPorFechaDesc } from '../../../utils/fechas';
import TurnoCard from './TurnoCard';
import ModalDetalleTurno from './ModalDetalleTurno';
import ModalConfirmacion from '../../../components/ModalConfirmacion';
import { CalendarDays, UserGroup, User } from 'lucide-react';

const AgendaTab = ({ recursos }) => {
  const [recursoSeleccionadoId, setRecursoSeleccionadoId] = useState('');
  
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
  const [turnoDetalle, setTurnoDetalle] = useState(null);

  if (recursos.length === 0) {
    return (
      <div className="text-center py-16 bg-white rounded-2xl border border-dashed border-gray-200">
        <span className="text-4xl block mb-3"><UserGroup className="h-12 w-12 mx-auto" /></span>
        <p className="text-app-marineblue font-medium text-lg">No hay recursos configurados</p>
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
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 min-h-125 relative">
      
      <header className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6 pb-6 border-b border-gray-100">
        <div>
          <h2 className="text-xl font-bold text-app-black">Agendas de Turnos</h2>
          <p className="text-sm text-app-marineblue mt-1">
            {recursos.length > 1 ? 'Seleccioná un recurso para ver sus reservas.' : 'Revisá tus reservas programadas.'}
          </p>
        </div>
        
        {recursos.length > 1 ? (
          <select 
            value={recursoSeleccionadoId} 
            onChange={(e) => setRecursoSeleccionadoId(Number(e.target.value))}
            className="bg-app-white border-none text-app-black text-sm rounded-xl focus:ring-2 focus:ring-app-marineblue block px-4 py-3 font-medium cursor-pointer"
          >
            <option value="" disabled>Seleccioná un recurso...</option>
            {recursos.map(r => (
              <option key={r.id} value={r.id}>{r.nombre}</option>
            ))}
          </select>
        ) : recursos.length === 1 ? (
          <div className="bg-app-white text-app-marineblue text-sm rounded-xl px-4 py-3 font-medium shadow-sm flex items-center">
            <User className="h-5 w-5 mr-2 shrink-0" />
            {recursos[0].nombre}
          </div>
        ) : null}
      </header>

      {!recursoSeleccionadoId ? (
        <div className="text-center py-16 bg-app-white rounded-2xl border border-dashed border-gray-200">
          <span className="text-4xl block mb-3"><CalendarDays className="h-12 w-12 mx-auto" /></span>
          <p className="text-app-marineblue font-medium text-lg">Seleccioná un recurso arriba</p>
          <p className="text-gray-400 text-sm mt-1">Para visualizar y gestionar los turnos de su agenda.</p>
        </div>
      ) : loading ? (
        <div className="flex justify-center py-12"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-app-marineblue"></div></div>
      ) : (
        <>
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
            <div className="flex gap-2">
              <button 
                onClick={() => setVistaActual('proximos')}
                className={`px-5 py-2 rounded-xl text-sm font-medium transition-colors ${vistaActual === 'proximos' ? 'bg-app-marineblue text-white shadow-sm' : 'bg-gray-50 text-app-marineblue hover:bg-gray-100'}`}
              >
                Próximos
              </button>
              <button 
                onClick={() => setVistaActual('historial')}
                className={`px-5 py-2 rounded-xl text-sm font-medium transition-colors ${vistaActual === 'historial' ? 'bg-app-marineblue text-white shadow-sm' : 'bg-gray-50 text-app-marineblue hover:bg-gray-100'}`}
              >
                Historial
              </button>
            </div>

            {vistaActual === 'proximos' && (
              <select 
                value={filtroProximos}
                onChange={(e) => setFiltroProximos(e.target.value)}
                className="bg-gray-50 border-none text-app-black text-xs rounded-xl block px-4 py-2 cursor-pointer focus:ring-2 focus:ring-app-marineblue"
              >
                <option value="todos">Ver: Todos</option>
                <option value="hoy">Ver: Solo hoy</option>
              </select>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {vistaActual === 'proximos' && (
              turnosProximos.length > 0 ? (
                turnosProximos.map(turno => (
                  <TurnoCard 
                    key={turno.id}
                    turno={turno}
                    mostrarNegocio={false}
                    mostrarRecurso={recursos.length > 1}
                    mostrarContacto={true}
                    onVerDetalle={(t) => setTurnoDetalle(t)}
                    onAccion={(t) => setTurnoACancelar(t)}
                    textoAccion="Cancelar Turno"
                    varianteAccion="peligro"
                  />
                ))
              ) : (
                <div className="col-span-full text-center py-10 bg-app-white rounded-2xl border border-dashed border-gray-200">
                  <p className="text-app-marineblue text-sm font-medium">No hay turnos próximos asignados a este recurso.</p>
                </div>
              )
            )}

            {vistaActual === 'historial' && (
              turnosHistorial.length > 0 ? (
                turnosHistorial.map(turno => (
                  <TurnoCard 
                    key={turno.id}
                    turno={turno}
                    mostrarNegocio={false}
                    mostrarRecurso={recursos.length > 1}
                    mostrarContacto={true}
                    onVerDetalle={(t) => setTurnoDetalle(t)}
                    onAccion={turno.estado !== 'CANCELADO' ? (t) => setTurnoACancelar(t) : null}
                    textoAccion="Marcar Inasistencia / Cancelar"
                    varianteAccion="peligro"
                  />
                ))
              ) : (
                <div className="col-span-full text-center py-10 bg-gray-50 rounded-2xl border border-dashed border-gray-200">
                  <p className="text-gray-500 text-sm">El historial está vacío.</p>
                </div>
              )
            )}
          </div>
        </>
      )}

      <ModalDetalleTurno 
        turnoDetalle={turnoDetalle} 
        onClose={() => setTurnoDetalle(null)}
        onCancelarTurno={setTurnoACancelar}
        esDueño={true} 
      />

      <ModalConfirmacion 
        isOpen={!!turnoACancelar}
        titulo="¿Cancelar este turno?"
        mensaje={<>Estás a punto de cancelar el turno para <strong>{turnoACancelar?.nombreServicio}</strong> de <strong>{turnoACancelar?.nombreCliente} {turnoACancelar?.apellidoCliente}</strong>. Esta acción no se puede deshacer.</>}
        textoConfirmar="Sí, cancelar"
        esPeligroso={true}
        isConfirming={isCancelando}
        onClose={() => setTurnoACancelar(null)}
        onConfirm={async () => {
          setIsCancelando(true);
          const exito = await cancelarTurno(turnoACancelar.id);
          setIsCancelando(false);
          if (exito) setTurnoACancelar(null);
        }}
      />
    </div>
  );
};

export default AgendaTab;