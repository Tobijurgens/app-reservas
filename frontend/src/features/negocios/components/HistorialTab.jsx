import { useState, useEffect } from 'react';
import { useHistorialNegocio } from '../hooks/useHistorialNegocio';
import { useHistorialRecurso } from '../hooks/useHistorialRecurso';
import { TurnosService } from '../../../services/TurnosService';
import { esHoy } from '../../../utils/fechas';
import ModalDetalleTurno from './ModalDetalleTurno';
import ModalConfirmacion from '../../../components/ModalConfirmacion';

const HistorialTab = ({ negocioId, recursoId, recursos = [] }) => {
  
  const dataNegocio = useHistorialNegocio(negocioId);
  const dataRecurso = useHistorialRecurso(recursoId);

  const dataActiva = recursoId ? dataRecurso : dataNegocio;
  const { turnos, loading, paginaActual, setPaginaActual, totalPaginas, totalTurnos } = dataActiva;
  
  const [filtro, setFiltro] = useState('todos');
  const [filtroRecurso, setFiltroRecurso] = useState('todos');
  const [turnoDetalle, setTurnoDetalle] = useState(null);
  
  const [turnoACancelar, setTurnoACancelar] = useState(null);
  const [isCancelando, setIsCancelando] = useState(false);
  
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
      await TurnosService.cancelarTurno(turnoACancelar.id);
      
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
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 min-h-125 flex flex-col relative">
      <header className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 mb-6">
        <div>
          <h2 className="text-xl font-bold text-app-black">
            {recursoId ? 'Mi Historial de Turnos' : 'Historial Global del Negocio'}
          </h2>
          <p className="text-sm text-app-marineblue mt-1">Total de registros: {totalTurnos}</p>
        </div>
        
        <div className="flex flex-col sm:flex-row gap-3 w-full md:w-auto">
          {!recursoId && recursos.length > 0 && (
            <select 
              value={filtroRecurso}
              onChange={(e) => setFiltroRecurso(e.target.value)}
              className="bg-app-white border-none text-app-black text-sm rounded-xl focus:ring-2 focus:ring-app-marineblue block px-4 py-2 cursor-pointer font-medium shadow-sm"
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
            className="bg-app-white border-none text-app-black text-sm rounded-xl focus:ring-2 focus:ring-app-marineblue block px-4 py-2 cursor-pointer font-medium shadow-sm"
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
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-app-marineblue"></div>
        </div>
      ) : (
        <>
          <div className="flex-1 overflow-x-auto">
            <table className="w-full text-left text-sm text-app-marineblue">
              <thead className="text-xs text-app-black uppercase bg-app-white rounded-t-xl">
                <tr>
                  <th className="px-4 py-4 rounded-tl-xl">Fecha</th>
                  <th className="px-4 py-4">Hora</th>
                  <th className="px-4 py-4">Cliente</th>
                  <th className="px-4 py-4">Servicio</th>
                  {!recursoId && <th className="px-4 py-4">Recurso</th>}
                  <th className="px-4 py-4">Estado</th>
                  <th className="px-4 py-4 text-center rounded-tr-xl">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {turnosMostrados.length > 0 ? (
                  turnosMostrados.map((turno) => {
                    const fecha = new Date(turno.fechaHoraInicio);
                    const esCancelado = turno.estado === 'CANCELADO';
                    return (
                      <tr key={turno.id} className="border-b border-gray-100 hover:bg-app-white/50 transition-colors">
                        <td className="px-4 py-4 whitespace-nowrap font-semibold text-app-black">
                          {fecha.toLocaleDateString('es-AR')}
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap font-semibold">
                          {fecha.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit', hour12: false })} hs
                        </td>
                        <td className="px-4 py-4 font-semibold text-app-black">
                          {turno.nombreCliente} {turno.apellidoCliente}
                        </td>
                        <td className="px-4 py-4 font-semibold text-app-marineblue">
                          {turno.nombreServicio}
                        </td>

                        {!recursoId && <td className="px-4 py-4 font-semibold text-app-black">{turno.nombreRecurso}</td>}

                        <td className="px-4 py-4">
                          <span className={`px-3 py-1 rounded-full text-xs font-bold ${esCancelado ? 'bg-app-red text-app-white' : 'bg-app-marineblue text-white'}`}>
                            {turno.estado}
                          </span>
                        </td>
                        <td className="px-4 py-4 text-center">
                            <button 
                                onClick={() => setTurnoDetalle(turno)}
                                className="text-app-marineblue hover:text-app-black p-2 rounded-full hover:bg-white shadow-sm transition-colors inline-flex items-center justify-center border border-transparent hover:border-gray-200"
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
                    <td colSpan={recursoId ? "6" : "7"} className="px-4 py-12 text-center text-app-marineblue">
                      No hay registros para mostrar con los filtros seleccionados.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>

          {totalPaginas > 1 && (
            <div className="mt-6 pt-4 border-t border-gray-100 flex flex-col sm:flex-row justify-between items-center gap-4">
              <span className="text-sm text-app-marineblue">
                Página <span className="font-bold text-app-black">{paginaActual + 1}</span> de <span className="font-bold text-app-black">{totalPaginas}</span>
              </span>
              <div className="flex gap-2">
                <button
                  onClick={() => setPaginaActual(p => p - 1)}
                  disabled={paginaActual === 0}
                  className="px-4 py-2 bg-app-white text-app-marineblue rounded-xl text-sm font-medium hover:bg-gray-200 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  Anterior
                </button>
                <button
                  onClick={() => setPaginaActual(p => p + 1)}
                  disabled={paginaActual >= totalPaginas - 1}
                  className="px-4 py-2 bg-app-marineblue text-white rounded-xl text-sm font-medium hover:bg-app-black/90 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  Siguiente
                </button>
              </div>
            </div>
          )}
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
        onConfirm={handleCancelarTurno}
      />
    </div>
  );
};

export default HistorialTab;