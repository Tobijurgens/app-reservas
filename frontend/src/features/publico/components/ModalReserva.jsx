import { useState, useEffect, useContext } from 'react';
import { TurnosService } from '../../../services/TurnosService';
import { AuthContext } from '../../../context/AuthContext'; 
import { useToast } from '../../../context/ToastContext';
import Login from '../../auth/pages/Login'; 
import Register from '../../auth/pages/Register';

import DatePicker, { registerLocale } from 'react-datepicker';
import { es } from 'date-fns/locale/es';
import { fechaCalendario, hoyEnNegocio } from '../utils/fechasReserva';
import 'react-datepicker/dist/react-datepicker.css';

registerLocale('es', es);

const ModalReserva = ({ isOpen, onClose, servicio, recursoId }) => {
  const [fecha, setFecha] = useState(null); 
  const [horarios, setHorarios] = useState([]);
  const [horarioSeleccionadoISO, setHorarioSeleccionadoISO] = useState('');
  const [cargandoHorarios, setCargandoHorarios] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [actualizacionHorarios, setActualizacionHorarios] = useState(0);

  // Estados para tachar días llenos
  const [diasLlenos, setDiasLlenos] = useState([]); 
  const [mesViendo, setMesViendo] = useState(() => hoyEnNegocio().getMonth() + 1);
  const [anioViendo, setAnioViendo] = useState(() => hoyEnNegocio().getFullYear());

  const [mostrarModalLogin, setMostrarModalLogin] = useState(false);
  const [mostrarModalRegistro, setMostrarModalRegistro] = useState(false);

  const { user } = useContext(AuthContext);
  const { addToast } = useToast();

  // EFECTO 1: Traer los días llenos desde el backend
  useEffect(() => {
    if (!isOpen || !recursoId || !servicio) return;
    let vigente = true;

    const fetchDiasLlenos = async () => {
      try {
        const dataActual = await TurnosService.obtenerDiasLlenos(anioViendo, mesViendo, recursoId, servicio.id);
        
        let mesSig = mesViendo + 1;
        let anioSig = anioViendo;
        if (mesSig > 12) { mesSig = 1; anioSig++; }
        
        const dataSig = await TurnosService.obtenerDiasLlenos(anioSig, mesSig, recursoId, servicio.id);
        
        const fechasTachadas = [...dataActual, ...dataSig].map(d => new Date(d + 'T00:00:00'));
        if (vigente) setDiasLlenos(fechasTachadas);
      } catch (e) {
        console.error("Error al traer días llenos", e);
      }
    };
    fetchDiasLlenos();
    return () => { vigente = false; };
  }, [isOpen, recursoId, servicio, mesViendo, anioViendo]);

  // EFECTO 2: Traer los horarios del día seleccionado
  useEffect(() => {
    if (!fecha || !isOpen) return;
    let vigente = true;

    const fetchHorarios = async () => {
      setCargandoHorarios(true);
      setHorarioSeleccionadoISO(''); 
      try {
        const fechaFormateada = fechaCalendario(fecha);
        const data = await TurnosService.obtenerHorariosDisponibles(fechaFormateada, recursoId, servicio.id);
        if (vigente) setHorarios(data);
      } catch (err) {
        console.error('Error al traer horarios', err);
        if (vigente) setHorarios([]);
      } finally {
        if (vigente) setCargandoHorarios(false);
      }
    };

    fetchHorarios();
    return () => { vigente = false; };
  }, [fecha, recursoId, servicio?.id, isOpen, actualizacionHorarios]);

  const handleClose = () => {
    setFecha(null);
    setHorarios([]);
    setHorarioSeleccionadoISO('');
    setIsSubmitting(false); 
    setMostrarModalLogin(false); 
    setMostrarModalRegistro(false);
    onClose();
  };

  const procesarReservaBackend = async () => {
    setIsSubmitting(true);
    try {
      await TurnosService.crearTurno({ idServicio: servicio.id, idRecurso: recursoId, fechaHoraInicio: horarioSeleccionadoISO });
      addToast('¡Reserva confirmada! En breve recibirás un correo con los detalles.', 'success');
      handleClose();
    } catch (err) {
      if (err.response?.status === 401 || err.response?.status === 403) setMostrarModalLogin(true);
      else if (err.response?.status === 409) {
        setHorarioSeleccionadoISO('');
        setHorarios([]);
        setActualizacionHorarios((valor) => valor + 1);
        addToast(err.response?.data?.message || 'Ese horario ya no está disponible. Elegí otro horario.', 'error');
      }
      else addToast(err.response?.data?.message || 'Error al confirmar la reserva.', 'error');
      setIsSubmitting(false); 
    }
  };

  const handleReservarClick = () => {
    if (!user) setMostrarModalLogin(true);
    else if (!isSubmitting) procesarReservaBackend();
  };

  const handleLoginSuccess = () => {
    setMostrarModalLogin(false); 
    procesarReservaBackend();    
  };

  // --- LÓGICA DE SEPARACIÓN DE HORARIOS ---
  let turnosMadrugada = [];
  let turnosResto = [];
  
  if (horarios.length > 0) {
    // Consideramos "Madrugada" a cualquier turno antes de las 08:00 AM
    turnosMadrugada = horarios.filter(h => parseInt(h.hora.split(':')[0]) < 8);
    // El resto del día es de 08:00 AM en adelante
    turnosResto = horarios.filter(h => parseInt(h.hora.split(':')[0]) >= 8);
  }

  // Obtenemos los nombres de los días para los títulos
  const diaActual = fecha ? fecha.toLocaleDateString('es-AR', { weekday: 'long' }) : '';

  // Función auxiliar para renderizar los botones y no repetir código
  const renderBotones = (lista) => {
    return lista.map((slot) => (
      <button 
        key={slot.fechaHoraISO} 
        disabled={!slot.disponible || isSubmitting} 
        onClick={() => setHorarioSeleccionadoISO(slot.fechaHoraISO)}
        className={`p-3 rounded-xl border text-sm font-bold transition-all flex justify-center shadow-sm ${
          !slot.disponible 
            ? 'bg-gray-100 text-gray-400 border-gray-200 cursor-not-allowed opacity-60' 
            : horarioSeleccionadoISO === slot.fechaHoraISO 
              ? 'bg-app-marineblue text-white border-app-marineblue scale-105' 
              : 'bg-white text-app-black border-gray-200 hover:bg-app-marineblue/10' 
        }`}
      >
        {slot.hora}
      </button>
    ));
  };

  if (!isOpen) return null;

  return (
    <>
      <div className="fixed inset-0 bg-app-black/40 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-fadeIn overflow-y-auto">
        <div className="bg-app-white rounded-2xl max-w-md w-full p-8 shadow-2xl border border-gray-100 my-8">
          <h2 className="text-2xl font-bold text-app-black mb-1">Reservar Turno</h2>
          <p className="text-app-marineblue text-sm mb-6">Para: <span className="font-semibold">{servicio?.nombre}</span></p>
          
          <label className="block text-sm font-bold text-app-marineblue mb-2">1. Seleccioná la fecha</label>
          
          <div className="mb-6">
            <DatePicker
              selected={fecha}
              onChange={(date) => setFecha(date)}
              onMonthChange={(date) => {
                setMesViendo(date.getMonth() + 1);
                setAnioViendo(date.getFullYear());
              }}
              locale="es"
              dateFormat="dd/MM/yyyy"
              minDate={hoyEnNegocio()}
              excludeDates={diasLlenos} 
              placeholderText="dd/mm/aaaa"
              wrapperClassName="w-full"
              className="w-full bg-white border border-gray-200 p-3 rounded-xl shadow-sm focus:ring-2 focus:ring-app-marineblue focus:outline-none text-app-black cursor-pointer"
            />
          </div>

          <label className="block text-sm font-bold text-app-marineblue mb-3">2. Horarios disponibles</label>
          
          {cargandoHorarios ? (
            <div className="flex justify-center py-6">
              <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-app-marineblue"></div>
            </div>
          ) : fecha ? (
            <div className="max-h-64 overflow-y-auto pr-2 mb-6 space-y-5">
              
              {horarios.length === 0 ? (
                <p className="text-sm text-app-marineblue text-center py-4 bg-white rounded-xl border border-dashed border-gray-200">
                  No hay horarios disponibles para este día.
                </p>
              ) : (
                <>
                  {/* SECCIÓN 1: MADRUGADA (00:00 a 07:59) */}
                  {turnosMadrugada.length > 0 && (
                    <div>
                      <h4 className="text-[11px] font-bold text-gray-500 uppercase tracking-wider mb-3 flex items-center gap-2 border-b border-gray-200 pb-1.5">
                        Madrugada del {diaActual}
                      </h4>
                      <div className="grid grid-cols-3 gap-3">
                        {renderBotones(turnosMadrugada)}
                      </div>
                    </div>
                  )}

                  {/* SECCIÓN 2: RESTO DEL DÍA (08:00 a 23:59) */}
                  {turnosResto.length > 0 && (
                    <div>
                      <h4 className="text-[11px] font-bold text-gray-500 uppercase tracking-wider mb-3 flex items-center gap-2 border-b border-gray-200 pb-1.5">
                        Jornada del {diaActual}
                      </h4>
                      <div className="grid grid-cols-3 gap-3">
                        {renderBotones(turnosResto)}
                      </div>
                    </div>
                  )}
                </>
              )}
            </div>
          ) : (
            <div className="text-sm text-app-marineblue mb-8 bg-white p-4 rounded-xl border border-dashed border-gray-200 text-center">
              Primero seleccioná una fecha.
            </div>
          )}

          <div className="flex gap-4">
            <button onClick={handleClose} disabled={isSubmitting} className="flex-1 py-3 text-app-marineblue bg-white hover:bg-app-black/10 border border-gray-200 rounded-xl font-medium transition-colors disabled:opacity-50">
              Cancelar
            </button>
            <button onClick={handleReservarClick} disabled={!fecha || !horarioSeleccionadoISO || isSubmitting} className={`flex-1 py-3 rounded-xl font-medium transition-colors shadow-sm ${!fecha || !horarioSeleccionadoISO || isSubmitting ? 'bg-gray-300 text-white cursor-not-allowed' : 'bg-app-marineblue text-white hover:bg-app-black/90'}`}>
              {isSubmitting ? 'Confirmando...' : 'Confirmar'}
            </button>
          </div>
        </div>
      </div>
      
      <Login isModal={true} isOpen={mostrarModalLogin} onClose={() => setMostrarModalLogin(false)} onSuccess={handleLoginSuccess} onSwitchToRegister={() => { setMostrarModalLogin(false); setMostrarModalRegistro(true); }} />
      <Register isModal={true} isOpen={mostrarModalRegistro} onClose={() => setMostrarModalRegistro(false)} onSuccess={() => { setMostrarModalRegistro(false); setMostrarModalLogin(true); }} onSwitchToLogin={() => { setMostrarModalRegistro(false); setMostrarModalLogin(true); }} />
    </>
  );
};

export default ModalReserva;
