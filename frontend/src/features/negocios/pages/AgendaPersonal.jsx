import { useState } from 'react';
import { useAgendaPersonal } from '../hooks/useAgendaPersonal';
import { esFuturo, esPasado, esHoy, ordenarPorFechaAsc, ordenarPorFechaDesc } from '../../../utils/fechas';
import TurnoCard from '../components/TurnoCard';
import ModalDetalleTurno from '../components/ModalDetalleTurno';
import ModalConfirmacion from '../../../components/ModalConfirmacion';
import { Store } from 'lucide-react';

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
  const [turnoDetalle, setTurnoDetalle] = useState(null);

  if (loading && recursosAsignados.length === 0) {
    return <div className="flex justify-center py-20"><div className="animate-spin rounded-full h-10 w-10 border-b-2 border-app-marineblue"></div></div>;
  }

  if (recursosAsignados.length === 0) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-12 text-center">
        <h2 className="text-2xl font-bold text-app-black mb-2">Mi Agenda de Trabajo</h2>
        <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 mt-6">
          <Store className="h-12 w-12 mx-auto mb-4 " />
          <p className="text-app-marineblue font-medium text-lg">No tenés ningún espacio de trabajo asignado.</p>
          <p className="text-sm text-gray-400 mt-2">Si trabajás en algún negocio, pedile al administrador que agregue tu email en la configuración.</p>
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
          <h1 className="text-2xl sm:text-3xl font-bold text-app-black">Mi Agenda</h1>
          <p className="text-sm text-app-marineblue mt-1">Revisá tus turnos programados.</p>
        </div>

        <select 
          value={recursoSeleccionado || ''} 
          onChange={(e) => setRecursoSeleccionado(Number(e.target.value))}
          className="bg-white border-none text-app-black text-sm rounded-xl focus:ring-2 focus:ring-app-marineblue block px-4 py-3 font-medium shadow-sm cursor-pointer"
        >
          <option value="" disabled>Seleccioná un negocio...</option>
          {recursosAsignados.map(r => (
            <option key={r.id} value={r.id}>
              {r.nombreNegocio}
            </option>
          ))}
        </select>
      </header>

      <main className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 min-h-100">
        {!recursoSeleccionado ? (
          <div className="text-center py-16 bg-app-white rounded-2xl border border-dashed border-gray-200">
            <Store className="h-12 w-12 mx-auto mb-3 " />
            <p className="text-app-marineblue font-medium text-lg">Seleccioná un negocio arriba</p>
            <p className="text-gray-400 text-sm mt-1">Para visualizar los turnos de la agenda.</p>
          </div>
        ) : loading ? (
           <div className="flex justify-center py-12"><div className="animate-spin rounded-full h-8 w-8 border-b-2 border-app-marineblue"></div></div>
        ) : (
          <>
            <div className="flex flex-col md:flex-row justify-between items-start md:items-center mb-6 gap-4 border-b border-gray-100 pb-6">
              <h2 className="text-xl font-bold text-app-black">
                {vistaActual === 'proximos' ? 'Próximos a atender' : 'Historial de Turnos'}
              </h2>

              <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4">
                {vistaActual === 'proximos' && (
                  <div className="flex items-center gap-2">
                    <label className="text-sm font-medium text-app-marineblue">Ver:</label>
                    <select 
                      value={filtroProximos}
                      onChange={(e) => setFiltroProximos(e.target.value)}
                      className="bg-app-white border-none text-app-black text-sm rounded-xl focus:ring-2 focus:ring-app-marineblue block px-4 py-2 cursor-pointer font-medium"
                    >
                      <option value="todos">Todos</option>
                      <option value="hoy">Solo hoy</option>
                    </select>
                  </div>
                )}

                <div className="bg-gray-100 p-1 rounded-xl flex gap-1 shadow-inner">
                  <button 
                    onClick={() => setVistaActual('proximos')}
                    className={`px-4 py-2 rounded-lg text-sm font-bold transition-colors ${vistaActual === 'proximos' ? 'bg-white text-app-black shadow-sm' : 'text-app-marineblue hover:text-app-black'}`}
                  >
                    Próximos
                  </button>
                  <button 
                    onClick={() => setVistaActual('historial')}
                    className={`px-4 py-2 rounded-lg text-sm font-bold transition-colors ${vistaActual === 'historial' ? 'bg-white text-app-black shadow-sm' : 'text-app-marineblue hover:text-app-black'}`}
                  >
                    Historial
                  </button>
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-5 mb-4">
              {vistaActual === 'proximos' && (
                turnosProximos.length > 0 ? (
                  turnosProximos.map(turno => (
                    <TurnoCard 
                      key={turno.id}
                      turno={turno}
                      mostrarNegocio={false}
                      mostrarRecurso={false}
                      mostrarContacto={true}
                      onVerDetalle={(t) => setTurnoDetalle(t)}
                      onAccion={(t) => setTurnoACancelar(t)}
                      textoAccion="Cancelar turno"
                      varianteAccion="peligro"
                    />
                  ))
                ) : (
                  <div className="col-span-full text-center py-12 bg-app-white rounded-2xl border border-dashed border-gray-200">
                    <p className="text-app-marineblue font-medium">
                      {filtroProximos === 'hoy' 
                        ? 'No tenés turnos programados para el resto del día de hoy.' 
                        : 'No tenés turnos próximos asignados.'}
                    </p>
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
                      mostrarRecurso={false}
                      mostrarContacto={true}
                      onVerDetalle={(t) => setTurnoDetalle(t)}
                      onAccion={turno.estado !== 'CANCELADO' ? (t) => setTurnoACancelar(t) : null}
                      textoAccion="Marcar Inasistencia / Cancelar"
                      varianteAccion="peligro"
                    />
                  ))
                ) : (
                  <div className="col-span-full text-center py-12 bg-gray-50 rounded-2xl border border-dashed border-gray-200">
                    <p className="text-gray-500 text-sm font-medium">El historial está vacío.</p>
                  </div>
                )
              )}
            </div>
          </>
        )}
      </main>

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

export default AgendaPersonal;