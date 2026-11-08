import { BriefcaseBusiness, User, UserGroup, Phone, AlarmClock, Info } from 'lucide-react';
import { esPasado } from '../../../utils/fechas';

const TurnoCard = ({ 
  turno, 
  mostrarNegocio = false, 
  mostrarRecurso = false, 
  mostrarContacto = false,
  onAccion,
  textoAccion = "Cancelar turno",
  varianteAccion = "peligro",
  onVerDetalle // NUEVO: Función para abrir el modal
}) => {
  const fecha = new Date(turno.fechaHoraInicio);
  const esCancelado = turno.estado === 'CANCELADO';
  const esPasadoTurno = esPasado(turno.fechaHoraInicio) && !esCancelado;

  return (
    <article className={`p-4 sm:p-5 rounded-xl border flex flex-col justify-between transition-all ${
      esCancelado ? 'bg-app-ligthblue/30 border-app-ligthblue opacity-80' : 
      esPasadoTurno ? 'bg-gray-50 border-gray-200' : 
      'bg-white border-gray-200 shadow-sm hover:shadow-md hover:border-app-marineblue'
    }`}>
      <div>
        {/* Cabecera: Estado, Fecha y Botón Info */}
        <div className="flex justify-between items-start mb-3">
          <span className={`text-[9px] sm:text-[10px] font-bold px-2.5 py-1 rounded-full uppercase tracking-wider ${
            esCancelado ? 'bg-app-red text-app-white' : 
            esPasadoTurno ? 'bg-gray-200 text-gray-700' : 
            'bg-app-marineblue text-white'
          }`}>
            {turno.estado}
          </span>
          <div className="flex items-center gap-2">
            <span className={`text-xs font-bold ${esCancelado || esPasadoTurno ? 'text-gray-400' : 'text-gray-500'}`}>
              {fecha.toLocaleDateString('es-AR', { weekday: 'short', day: 'numeric', month: 'short' })}
            </span>
            {onVerDetalle && (
              <button 
                onClick={() => onVerDetalle(turno)}
                className="text-app-marineblue hover:text-app-black transition-colors p-1 -mt-1 -mr-1 rounded-full hover:bg-gray-100"
                title="Ver ficha del turno"
              >
                <Info className="w-5 h-5" />
              </button>
            )}
          </div>
        </div>

        {/* Título y Precio */}
        <div className="flex justify-between items-start gap-3 mt-1">
          <h3 className={`font-bold text-base sm:text-lg leading-tight ${esCancelado || esPasadoTurno ? 'text-gray-500' : 'text-app-black'}`}>
            {turno.nombreServicio}
          </h3>
          <span className={`text-sm sm:text-base font-extrabold shrink-0 ${esCancelado || esPasadoTurno ? 'text-gray-400' : 'text-app-black'}`}>
            ${Number(turno.precio).toLocaleString('es-AR')}
          </span>
        </div>

        {/* Detalles condicionales */}
        <div className="mt-3 pt-3 border-t border-gray-100 space-y-1.5 text-xs sm:text-sm text-app-marineblue">
          <p className="flex items-center gap-2">
            <span className="text-base"><AlarmClock className="h-4 w-4" /></span> 
            <span className="font-medium text-app-black">
              {fecha.toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit', hour12: false })} hs
            </span>
          </p>
          
          {mostrarNegocio && (
            <p className="flex items-center gap-2">
              <span className="text-base"><BriefcaseBusiness className="h-4 w-4" /></span> 
              <span className="truncate font-medium text-app-black">{turno.nombreNegocio}</span>
            </p>
          )}
          
          {mostrarRecurso && (
            <p className="flex items-center gap-2">
              <span className="text-base"><UserGroup className="h-4 w-4" /></span> 
              <span className="truncate font-medium text-app-black">{turno.nombreRecurso}</span>
            </p>
          )}

          {mostrarContacto && (
            <>
              <p className="flex items-center gap-2">
                <span className="text-base"><User className="h-4 w-4" /></span> 
                <span className="font-medium text-app-black">{turno.nombreCliente} {turno.apellidoCliente}</span>
              </p>
              {turno.telefonoCliente && (
                <p className="flex items-center gap-2">
                  <span className="text-base"><Phone className="h-4 w-4" /></span> 
                  <span className="font-medium text-app-black">{turno.telefonoCliente}</span>
                </p>
              )}
            </>
          )}
        </div>
      </div>

      {/* Botón de Acción */}
      {onAccion && (
        <div className="mt-4 pt-3 border-t border-gray-100">
          <button 
            onClick={() => onAccion(turno)}
            className={`w-full text-xs px-3 py-2.5 rounded-lg font-bold text-center shadow-sm transition-colors duration-300 ${
              varianteAccion === 'peligro' 
                ? 'bg-app-white text-app-black border border-gray-200 hover:border-app-red hover:bg-app-red hover:text-app-white'
                : 'bg-app-ligthblue border border-app-ligthblue text-app-black hover:bg-app-marineblue hover:text-white'
            }`}
          >
            {textoAccion}
          </button>
        </div>
      )}
    </article>
  );
};

export default TurnoCard;
