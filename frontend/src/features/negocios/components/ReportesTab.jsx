import { useState } from 'react';
import { useReportesNegocio } from '../hooks/useReportesNegocio';
import { Star, ChartPie, UserGroup } from 'lucide-react';
import { fechaCalendario, hoyEnNegocio } from '../../publico/utils/fechasReserva';

const ReportesTab = ({ negocioId, recursos = [], esOwner = true, miRecursoId = null }) => {
  const hoyStr = fechaCalendario(hoyEnNegocio());
  const FECHA_HISTORICA = '2020-01-01';
  
  const getFechaDiasAtras = (dias) => {
    const d = hoyEnNegocio();
    d.setDate(d.getDate() - dias);
    return fechaCalendario(d);
  };

  const [filtroRapido, setFiltroRapido] = useState('completo'); 
  const [fechaInicio, setFechaInicio] = useState(FECHA_HISTORICA);
  const [fechaFin, setFechaFin] = useState(hoyStr);
  const [recursoFiltroId, setRecursoFiltroId] = useState(esOwner ? 'todos' : miRecursoId); 

  const { reporte, loading, error, reintentar } = useReportesNegocio(negocioId, fechaInicio, fechaFin, recursoFiltroId);

  const handleFiltroRapidoChange = (e) => {
    const tipo = e.target.value;
    setFiltroRapido(tipo);
    
    if (tipo === 'completo') {
      setFechaInicio(FECHA_HISTORICA); setFechaFin(hoyStr);
    } else if (tipo === 'hoy') {
      setFechaInicio(hoyStr); setFechaFin(hoyStr);
    } else if (tipo === '7dias') {
      setFechaInicio(getFechaDiasAtras(7)); setFechaFin(hoyStr);
    } else if (tipo === '30dias') {
      setFechaInicio(getFechaDiasAtras(30)); setFechaFin(hoyStr);
    }
  };

  let degCompletados = 0;
  let degPendientes = 0;
  
  if (reporte && reporte.totalTurnos > 0) {
    degCompletados = (reporte.turnosCompletados * 360.0) / reporte.totalTurnos;
    degPendientes = (reporte.turnosPendientes * 360.0) / reporte.totalTurnos;
  }

  return (
    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 flex flex-col">
      <header className="flex flex-col md:flex-row justify-between items-start gap-6 mb-8 pb-6 border-b border-gray-100">
        <div>
          <h2 className="text-xl font-bold text-app-black">
            {esOwner ? 'Rendimiento del Negocio' : 'Mi Rendimiento Personal'}
          </h2>
          <p className="text-sm text-app-marineblue mt-1">Seleccioná los filtros para analizar las métricas.</p>
        </div>
        
        <div className="flex flex-col gap-3 w-full md:w-72 shrink-0">
          {esOwner ? (
            recursos.length > 0 && (
              <select 
                value={recursoFiltroId}
                onChange={(e) => setRecursoFiltroId(e.target.value)}
                className="bg-app-white border-none text-app-black text-sm rounded-xl focus:ring-2 focus:ring-app-marineblue block w-full px-4 py-3 cursor-pointer font-medium shadow-sm"
              >
                <option value="todos">Todos los Recursos</option>
                {recursos.filter(r => r.activo).length > 0 && (
                  <optgroup label="Activos">
                    {recursos.filter(r => r.activo).map(r => (
                      <option key={r.id} value={r.id}>{r.nombre}</option>
                    ))}
                  </optgroup>
                )}
                {recursos.filter(r => !r.activo).length > 0 && (
                  <optgroup label="Inactivos (Historial)">
                    {recursos.filter(r => !r.activo).map(r => (
                      <option key={r.id} value={r.id}>{r.nombre}</option>
                    ))}
                  </optgroup>
                )}
              </select>
            )
          ) : (
            <div className="bg-app-white text-app-marineblue text-sm rounded-xl px-4 py-3 font-medium flex items-center justify-center shadow-sm">
              <span className="flex items-center">
                <UserGroup className="h-5 w-5 mr-2 shrink-0" /> 
                Viendo mis estadísticas
              </span>
            </div>
          )}

          <select 
            value={filtroRapido}
            onChange={handleFiltroRapidoChange}
            className="bg-app-white border-none text-app-black text-sm rounded-xl focus:ring-2 focus:ring-app-marineblue block w-full px-4 py-3 cursor-pointer font-medium shadow-sm"
          >
            <option value="completo">Período: Histórico Completo</option>
            <option value="hoy">Período: Solo Hoy</option>
            <option value="7dias">Período: Últimos 7 Días</option>
            <option value="30dias">Período: Últimos 30 Días</option>
            <option value="manual">Período: Rango Manual...</option>
          </select>

          <div className={`flex flex-col gap-3 bg-gray-50 p-4 rounded-xl border border-gray-100 transition-opacity shadow-sm ${filtroRapido === 'manual' ? 'opacity-100' : 'opacity-50 grayscale cursor-not-allowed'}`}>
            <div className="flex items-center justify-between gap-2 border-b border-gray-200 pb-2">
              <span className="text-app-marineblue text-xs font-bold uppercase w-12">Desde</span>
              <input 
                type="date" 
                value={fechaInicio} 
                disabled={filtroRapido !== 'manual'}
                onChange={(e) => setFechaInicio(e.target.value)}
                className="flex-1 bg-transparent text-sm font-medium text-app-black outline-none cursor-pointer disabled:cursor-not-allowed text-right"
              />
            </div>
            <div className="flex items-center justify-between gap-2">
              <span className="text-app-marineblue text-xs font-bold uppercase w-12">Hasta</span>
              <input 
                type="date" 
                value={fechaFin} 
                disabled={filtroRapido !== 'manual'}
                onChange={(e) => setFechaFin(e.target.value)}
                className="flex-1 bg-transparent text-sm font-medium text-app-black outline-none cursor-pointer disabled:cursor-not-allowed text-right"
              />
            </div>
          </div>
        </div>
      </header>

      {loading ? (
        <div className="flex-1 flex justify-center items-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-app-marineblue"></div>
        </div>
      ) : error ? (
        <div role="alert" className="py-12 text-center text-sm">
          <p>{error}</p>
          {fechaInicio && fechaFin && fechaInicio <= fechaFin && (
            <button onClick={reintentar} className="mt-4 font-semibold text-app-marineblue hover:underline">Reintentar</button>
          )}
        </div>
      ) : !reporte || reporte.totalTurnos === 0 ? (
        <div className="text-center py-16 bg-app-white rounded-2xl border border-dashed border-gray-200">
          <span className="text-4xl block mb-3"><ChartPie className="h-12 w-12 mx-auto" /></span>
          <p className="text-app-marineblue font-medium text-lg">No hay datos para esta selección</p>
          <p className="text-gray-400 text-sm mt-1">Probá ampliando el rango de fechas.</p>
        </div>
      ) : (
        <div className="space-y-8 animate-fadeIn">
          <div className="bg-gray-50 border border-gray-100 rounded-2xl p-6 flex flex-col md:flex-row items-center gap-10">
            <div className="relative w-56 h-56 rounded-full flex items-center justify-center shadow-sm"
                 style={{
                   background: `conic-gradient(
                     #6bbe56 0deg ${degCompletados}deg,
                     #e0cf35 ${degCompletados}deg ${degCompletados + degPendientes}deg,
                     #8b2929 ${degCompletados + degPendientes}deg 360deg
                   )`
                 }}>
              <div className="w-40 h-40 bg-gray-50 rounded-full flex flex-col items-center justify-center shadow-inner">
                <span className="text-4xl font-extrabold text-app-black">{reporte.totalTurnos}</span>
                <span className="text-xs text-app-marineblue uppercase tracking-widest mt-1">Reservas</span>
              </div>
            </div>

            <div className="flex-1 w-full space-y-5">
              <div className="bg-white p-5 rounded-xl border border-gray-100 shadow-sm flex justify-between items-center">
                <span className="text-app-marineblue font-bold uppercase tracking-wider text-sm">Ingresos Reales</span>
                <span className="text-2xl font-bold text-app-black">${Number(reporte.ingresosTotales).toLocaleString('es-AR')}</span>
              </div>

              <div className="space-y-3">
                <div className="flex items-center justify-between p-4 bg-white rounded-xl border border-gray-100 shadow-sm">
                  <div className="flex items-center gap-3">
                    <span className="w-4 h-4 rounded-full bg-app-green shadow-sm"></span>
                    <span className="font-medium text-app-black">Completados</span>
                  </div>
                  <span className="font-bold text-app-black text-lg">{reporte.turnosCompletados}</span>
                </div>

                <div className="flex items-center justify-between p-4 bg-white rounded-xl border border-gray-100 shadow-sm">
                  <div className="flex items-center gap-3">
                    <span className="w-4 h-4 rounded-full bg-app-yellow shadow-sm"></span>
                    <span className="font-medium text-app-black">Pendientes</span>
                  </div>
                  <span className="font-bold text-app-black text-lg">{reporte.turnosPendientes}</span>
                </div>

                <div className="flex items-center justify-between p-4 bg-white rounded-xl border border-gray-100 shadow-sm">
                  <div className="flex items-center gap-3">
                    <span className="w-4 h-4 rounded-full bg-app-red shadow-sm"></span>
                    <div className="flex flex-col">
                      <span className="font-medium text-app-black">Cancelados</span>
                      {(esOwner && recursoFiltroId === 'todos') && (
                        <span className="text-[11px] text-gray-400 font-medium mt-0.5">Tasa del {reporte.tasaCancelacionPorcentaje}%</span>
                      )}
                    </div>
                  </div>
                  <span className="font-bold text-app-black text-lg">{reporte.turnosCancelados}</span>
                </div>
              </div>
            </div>
          </div>

          {(esOwner && recursoFiltroId === 'todos') && (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              <div>
                <h3 className="font-bold text-app-black mb-4 flex items-center gap-2"><span><Star className="h-5 w-5" /></span> Servicios más solicitados</h3>
                <ul className="space-y-3">
                  {Object.entries(reporte.serviciosPopulares || {})
                    .sort((a, b) => b[1] - a[1])
                    .map(([nombre, cantidad], idx) => (
                      <li key={idx} className="flex justify-between items-center p-4 bg-gray-50 rounded-xl border border-gray-100">
                        <span className="font-medium text-app-black">{nombre}</span>
                        <span className="bg-white px-3 py-1 rounded-full text-sm font-bold text-app-marineblue border border-gray-200 shadow-sm">{cantidad} turnos</span>
                      </li>
                  ))}
                </ul>
              </div>

              <div>
                <h3 className="font-bold text-app-black mb-4 flex items-center gap-2"><span><UserGroup className="h-5 w-5" /></span> Rendimiento del Staff</h3>
                <ul className="space-y-3">
                  {Object.entries(reporte.recursosPopulares || {})
                    .sort((a, b) => b[1] - a[1])
                    .map(([nombre, cantidad], idx) => (
                      <li key={idx} className="flex justify-between items-center p-4 bg-gray-50 rounded-xl border border-gray-100">
                        <span className="font-medium text-app-black">{nombre}</span>
                        <span className="bg-white px-3 py-1 rounded-full text-sm font-bold text-app-marineblue border border-gray-200 shadow-sm">{cantidad} turnos</span>
                      </li>
                  ))}
                </ul>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default ReportesTab;
