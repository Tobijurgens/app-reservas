import { useState } from 'react';
import { useReportesNegocio } from '../hooks/useReportesNegocio';

// AHORA RECIBE esOwner y miRecursoId
const ReportesTab = ({ negocioId, recursos = [], esOwner = true, miRecursoId = null }) => {
  const hoyStr = new Date().toISOString().split('T')[0];
  const FECHA_HISTORICA = '2020-01-01';
  
  const getFechaDiasAtras = (dias) => {
    const d = new Date();
    d.setDate(d.getDate() - dias);
    return d.toISOString().split('T')[0];
  };

  const [filtroRapido, setFiltroRapido] = useState('completo'); 
  const [fechaInicio, setFechaInicio] = useState(FECHA_HISTORICA);
  const [fechaFin, setFechaFin] = useState(hoyStr);
  
  // Si es dueño, por defecto muestra "todos". Si es empleado, muestra solo su ID.
  const [recursoFiltroId, setRecursoFiltroId] = useState(esOwner ? 'todos' : miRecursoId); 

  const { reporte, loading } = useReportesNegocio(negocioId, fechaInicio, fechaFin, recursoFiltroId);

  const handleFiltroRapidoChange = (e) => {
    const tipo = e.target.value;
    setFiltroRapido(tipo);
    
    if (tipo === 'completo') {
      setFechaInicio(FECHA_HISTORICA);
      setFechaFin(hoyStr);
    } else if (tipo === 'hoy') {
      setFechaInicio(hoyStr);
      setFechaFin(hoyStr);
    } else if (tipo === '7dias') {
      setFechaInicio(getFechaDiasAtras(7));
      setFechaFin(hoyStr);
    } else if (tipo === '30dias') {
      setFechaInicio(getFechaDiasAtras(30));
      setFechaFin(hoyStr);
    }
  };

  let degCompletados = 0;
  let degPendientes = 0;
  
  if (reporte && reporte.totalTurnos > 0) {
    degCompletados = (reporte.turnosCompletados * 360.0) / reporte.totalTurnos;
    degPendientes = (reporte.turnosPendientes * 360.0) / reporte.totalTurnos;
  }

  return (
    <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 flex flex-col">
      <header className="flex flex-col md:flex-row justify-between items-start gap-6 mb-8 pb-6 border-b border-gray-100">
        <div>
          <h2 className="text-xl font-bold text-gray-800">
            {esOwner ? 'Rendimiento del Negocio' : 'Mi Rendimiento Personal'}
          </h2>
          <p className="text-sm text-gray-500 mt-1">Seleccioná los filtros para analizar las métricas.</p>
        </div>
        
        <div className="flex flex-col gap-3 w-full md:w-72 shrink-0">
          
          {/* CONTROL DE VISTA SEGÚN ROL */}
          {esOwner ? (
            recursos.length > 0 && (
              <select 
                value={recursoFiltroId}
                onChange={(e) => setRecursoFiltroId(e.target.value)}
                className="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full px-3 py-2.5 cursor-pointer font-medium shadow-sm"
              >
                <option value="todos">Todos los Recursos</option>
                
                {recursos.filter(r => r.activo).length > 0 && (
                  <optgroup label="🟢 Activos">
                    {recursos.filter(r => r.activo).map(r => (
                      <option key={r.id} value={r.id}>{r.nombre}</option>
                    ))}
                  </optgroup>
                )}

                {recursos.filter(r => !r.activo).length > 0 && (
                  <optgroup label="🔴 Inactivos (Historial)">
                    {recursos.filter(r => !r.activo).map(r => (
                      <option key={r.id} value={r.id}>{r.nombre}</option>
                    ))}
                  </optgroup>
                )}
              </select>
            )
          ) : (
            <div className="bg-blue-50 border border-blue-200 text-blue-800 text-sm rounded-lg px-3 py-2.5 font-medium flex items-center justify-center shadow-sm">
              <span>👤 Viendo mis estadísticas</span>
            </div>
          )}

          <select 
            value={filtroRapido}
            onChange={handleFiltroRapidoChange}
            className="bg-gray-50 border border-gray-300 text-gray-900 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full px-3 py-2.5 cursor-pointer font-medium shadow-sm"
          >
            <option value="completo">Período: Histórico Completo</option>
            <option value="hoy">Período: Solo Hoy</option>
            <option value="7dias">Período: Últimos 7 Días</option>
            <option value="30dias">Período: Últimos 30 Días</option>
            <option value="manual">Período: Rango Manual...</option>
          </select>

          <div className={`flex flex-col gap-3 bg-gray-50 p-3 rounded-lg border border-gray-200 transition-opacity shadow-sm ${filtroRapido === 'manual' ? 'opacity-100' : 'opacity-50 grayscale cursor-not-allowed'}`}>
            <div className="flex items-center justify-between gap-2 border-b border-gray-200 pb-2">
              <span className="text-gray-500 text-xs font-bold uppercase w-12">Desde</span>
              <input 
                type="date" 
                value={fechaInicio} 
                disabled={filtroRapido !== 'manual'}
                onChange={(e) => setFechaInicio(e.target.value)}
                className="flex-1 bg-transparent text-sm font-medium text-gray-900 outline-none cursor-pointer disabled:cursor-not-allowed text-right"
              />
            </div>
            <div className="flex items-center justify-between gap-2">
              <span className="text-gray-500 text-xs font-bold uppercase w-12">Hasta</span>
              <input 
                type="date" 
                value={fechaFin} 
                disabled={filtroRapido !== 'manual'}
                onChange={(e) => setFechaFin(e.target.value)}
                className="flex-1 bg-transparent text-sm font-medium text-gray-900 outline-none cursor-pointer disabled:cursor-not-allowed text-right"
              />
            </div>
          </div>
        </div>
      </header>

      {loading ? (
        <div className="flex-1 flex justify-center items-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
        </div>
      ) : !reporte || reporte.totalTurnos === 0 ? (
        <div className="text-center py-16 bg-gray-50 rounded-lg border border-dashed border-gray-300">
          <span className="text-4xl block mb-3">📉</span>
          <p className="text-gray-600 font-medium text-lg">No hay datos para esta selección</p>
          <p className="text-gray-400 text-sm mt-1">Probá ampliando el rango de fechas.</p>
        </div>
      ) : (
        <div className="space-y-8 animate-fadeIn">
          {/* CÓDIGO DEL GRÁFICO, LEYENDA Y RANKINGS (SE MANTIENE IGUAL, OMITIDO POR BREVEDAD, DEJÁ EL QUE YA TENÍAS) */}
          <div className="bg-gray-50 border border-gray-200 rounded-xl p-6 flex flex-col md:flex-row items-center gap-10">
            <div className="relative w-56 h-56 rounded-full flex items-center justify-center shadow-sm"
                 style={{
                   background: `conic-gradient(
                     #22c55e 0deg ${degCompletados}deg,
                     #eab308 ${degCompletados}deg ${degCompletados + degPendientes}deg,
                     #ef4444 ${degCompletados + degPendientes}deg 360deg
                   )`
                 }}>
              <div className="w-40 h-40 bg-gray-50 rounded-full flex flex-col items-center justify-center shadow-inner">
                <span className="text-4xl font-extrabold text-gray-800">{reporte.totalTurnos}</span>
                <span className="text-xs text-gray-500 uppercase tracking-widest mt-1">Reservas</span>
              </div>
            </div>

            <div className="flex-1 w-full space-y-5">
              <div className="bg-white p-4 rounded-lg border border-blue-100 shadow-sm flex justify-between items-center">
                <span className="text-blue-700 font-bold uppercase tracking-wider text-sm">Ingresos Reales</span>
                <span className="text-2xl font-extrabold text-blue-600">${reporte.ingresosTotales}</span>
              </div>

              <div className="space-y-3">
                <div className="flex items-center justify-between p-3 bg-white rounded-lg border border-gray-100 shadow-sm">
                  <div className="flex items-center gap-3">
                    <span className="w-4 h-4 rounded-full bg-green-500 shadow-sm"></span>
                    <span className="font-medium text-gray-700">Completados</span>
                  </div>
                  <span className="font-bold text-gray-900 text-lg">{reporte.turnosCompletados}</span>
                </div>

                <div className="flex items-center justify-between p-3 bg-white rounded-lg border border-gray-100 shadow-sm">
                  <div className="flex items-center gap-3">
                    <span className="w-4 h-4 rounded-full bg-yellow-500 shadow-sm"></span>
                    <span className="font-medium text-gray-700">Pendientes</span>
                  </div>
                  <span className="font-bold text-gray-900 text-lg">{reporte.turnosPendientes}</span>
                </div>

                <div className="flex items-center justify-between p-3 bg-white rounded-lg border border-gray-100 shadow-sm">
                  <div className="flex items-center gap-3">
                    <span className="w-4 h-4 rounded-full bg-red-500 shadow-sm"></span>
                    <div className="flex flex-col">
                      <span className="font-medium text-gray-700">Cancelados</span>
                      {(esOwner && recursoFiltroId === 'todos') && (
                        <span className="text-[11px] text-gray-400 font-medium mt-0.5">Tasa del {reporte.tasaCancelacionPorcentaje}%</span>
                      )}
                    </div>
                  </div>
                  <span className="font-bold text-gray-900 text-lg">{reporte.turnosCancelados}</span>
                </div>
              </div>
            </div>
          </div>

          {(esOwner && recursoFiltroId === 'todos') && (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              <div>
                <h3 className="font-bold text-gray-800 mb-4 flex items-center gap-2"><span>⭐</span> Servicios más solicitados</h3>
                <ul className="space-y-3">
                  {Object.entries(reporte.serviciosPopulares || {})
                    .sort((a, b) => b[1] - a[1])
                    .map(([nombre, cantidad], idx) => (
                      <li key={idx} className="flex justify-between items-center p-3 bg-gray-50 rounded-lg border border-gray-100">
                        <span className="font-medium text-gray-700">{nombre}</span>
                        <span className="bg-white px-3 py-1 rounded-full text-sm font-bold text-gray-900 border border-gray-200">{cantidad} turnos</span>
                      </li>
                  ))}
                </ul>
              </div>

              <div>
                <h3 className="font-bold text-gray-800 mb-4 flex items-center gap-2"><span>👥</span> Rendimiento del Staff</h3>
                <ul className="space-y-3">
                  {Object.entries(reporte.recursosPopulares || {})
                    .sort((a, b) => b[1] - a[1])
                    .map(([nombre, cantidad], idx) => (
                      <li key={idx} className="flex justify-between items-center p-3 bg-gray-50 rounded-lg border border-gray-100">
                        <span className="font-medium text-gray-700">{nombre}</span>
                        <span className="bg-white px-3 py-1 rounded-full text-sm font-bold text-gray-900 border border-gray-200">{cantidad} turnos</span>
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