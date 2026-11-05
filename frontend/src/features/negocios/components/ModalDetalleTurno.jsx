import { FileText, Toolbox, User, UserGroup, Phone, BriefcaseBusiness } from 'lucide-react';

const ModalDetalleTurno = ({ turnoDetalle, onClose, onCancelarTurno, esDueño = false }) => {
  if (!turnoDetalle) return null;

  return (
    <div className="fixed inset-0 bg-app-black/40 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-fadeIn">
      <div className="bg-white rounded-2xl max-w-md w-full shadow-2xl overflow-hidden border border-gray-100">
        <div className={`px-6 py-5 flex justify-between items-center border-b ${
          turnoDetalle.estado === 'CANCELADO' ? 'bg-app-ligthblue/30 border-app-ligthblue' : 'bg-app-white border-gray-100'
        }`}>
          <h3 className="font-bold text-app-black text-lg flex items-center gap-2">
            <span><FileText className="h-4 w-4" /></span> Ficha del Turno
          </h3>
          <button onClick={onClose} className="text-app-marineblue hover:text-app-black bg-white p-1.5 rounded-full shadow-sm transition-colors">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        </div>
        
        <div className="p-6 space-y-5">
          <div className="flex justify-between items-center pb-4 border-b border-gray-100">
            <div>
              <span className="text-xs text-app-black block mb-1">Estado actual</span>
              <span className={`px-3 py-1 rounded-full text-xs font-bold ${
                turnoDetalle.estado === 'CANCELADO' ? 'bg-app-red text-app-white' : 'bg-app-marineblue text-white'
              }`}>{turnoDetalle.estado}</span>
            </div>
            <div className="text-right">
              <span className="text-xs text-app-black block mb-1">Precio</span>
              <span className="font-bold text-app-black text-xl">${Number(turnoDetalle.precio).toLocaleString('es-AR')}</span>
            </div>
          </div>
          
          <div className="grid grid-cols-2 gap-3">
            <div className="bg-app-white p-3 rounded-xl border border-transparent">
              <span className="text-xs text-app-marineblue block mb-1 text-center">Día</span>
              <p className="font-semibold text-app-black text-sm text-center">{new Date(turnoDetalle.fechaHoraInicio).toLocaleDateString('es-AR')}</p>
            </div>
            <div className="bg-app-white p-3 rounded-xl border border-transparent">
              <span className="text-xs text-app-marineblue block mb-1 text-center">Hora</span>
              <p className="font-semibold text-app-black text-sm text-center">{new Date(turnoDetalle.fechaHoraInicio).toLocaleTimeString('es-AR', { hour: '2-digit', minute: '2-digit', hour12: false })} hs</p>
            </div>
          </div>
          
          <div className="space-y-3 pt-4 border-t border-gray-100">
            <h4 className="text-xs font-bold text-app-black uppercase tracking-wider">Detalles</h4>
            
            {/* Si NO es dueño (es el cliente), mostramos el nombre del negocio */}
            {!esDueño && (
              <div className="flex items-center gap-3 text-sm">
                <BriefcaseBusiness className="h-4 w-4 shrink-0 text-app-marineblue" />
                <span className="font-medium text-app-marineblue">
                  Negocio: <span className="text-app-black">{turnoDetalle.nombreNegocio}</span>
                </span>
              </div>
            )}

            <div className="flex items-center gap-3 text-sm">
              <Toolbox className="h-4 w-4 shrink-0 text-app-marineblue" />
              <span className="font-medium text-app-marineblue">
                Servicio: <span className="text-app-black">{turnoDetalle.nombreServicio}</span>
              </span>
            </div>

            <div className="flex items-center gap-3 text-sm">
              <UserGroup className="h-4 w-4 shrink-0 text-app-marineblue" />
              <span className="font-medium text-app-marineblue">
                Recurso: <span className="text-app-black">{turnoDetalle.nombreRecurso}</span>
              </span>
            </div>

            {/* Si ES dueño (o recurso), mostramos los datos del cliente */}
            {esDueño && (
              <>
                <div className="flex items-center gap-3 text-sm">
                  <User className="h-4 w-4 shrink-0 text-app-marineblue" />
                  <span className="font-medium text-app-marineblue">
                    Cliente: <span className="text-app-black">{turnoDetalle.nombreCliente} {turnoDetalle.apellidoCliente}</span>
                  </span>
                </div>

                {turnoDetalle.telefonoCliente && (
                  <div className="flex items-center gap-3 text-sm">
                    <Phone className="h-4 w-4 shrink-0 text-app-marineblue" />
                    <span className="font-medium text-app-marineblue">
                        Teléfono Cliente:{' '}
                        <a 
                          href={`https://wa.me/${turnoDetalle.telefonoCliente.replace(/\D/g, '')}`} 
                          target="_blank" 
                          rel="noreferrer" 
                          className="text-app-black hover:text-app-marineblue transition-colors"
                        >
                          {turnoDetalle.telefonoCliente}
                        </a>
                    </span>
                  </div>
                )}
              </>
            )}
          </div>

          {turnoDetalle.estado !== 'CANCELADO' && onCancelarTurno && esDueño && (
            <div className="pt-5 border-t border-gray-100 flex justify-end">
              <button 
                onClick={() => {
                  onCancelarTurno(turnoDetalle);
                  onClose(); 
                }}
                className="px-5 py-3 w-full sm:w-auto bg-app-black/90 text-app-white hover:bg-app-red hover:text-app-white rounded-xl text-sm font-bold transition-colors flex items-center justify-center gap-2 border border-app-ligthblue"
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
  );
};

export default ModalDetalleTurno;