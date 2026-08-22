import { useState, useEffect, useContext } from 'react';
import api from '../../../services/api';
import { AuthContext } from '../../../context/AuthContext'; 
// Asegurate de que estas dos rutas apunten bien a tus componentes
import Login from '../../auth/pages/Login'; 
import Register from '../../auth/pages/Register';

const ModalReserva = ({ isOpen, onClose, servicio, recursoId }) => {
  const [fecha, setFecha] = useState('');
  const [horarios, setHorarios] = useState([]);
  const [horarioSeleccionadoISO, setHorarioSeleccionadoISO] = useState('');
  const [cargandoHorarios, setCargandoHorarios] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // ESTADOS PARA LOS MODALES DE AUTH
  const [mostrarModalLogin, setMostrarModalLogin] = useState(false);
  const [mostrarModalRegistro, setMostrarModalRegistro] = useState(false);

  const { user } = useContext(AuthContext);

  useEffect(() => {
    if (!fecha || !isOpen) return;

    const fetchHorarios = async () => {
      setCargandoHorarios(true);
      setHorarioSeleccionadoISO(''); 
      
      try {
        const response = await api.get('/turnos/disponibles', {
          params: {
            fecha: fecha,
            idRecurso: recursoId,
            idServicio: servicio.id
          }
        });
        setHorarios(response.data); 
      } catch (err) {
        console.error('Error al traer horarios', err);
        setHorarios([]);
      } finally {
        setCargandoHorarios(false);
      }
    };

    fetchHorarios();
  }, [fecha, recursoId, servicio?.id, isOpen]);

  const handleClose = () => {
    setFecha('');
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
      await api.post('/turnos', {
        idServicio: servicio.id,
        fechaHoraInicio: horarioSeleccionadoISO 
      });
      alert('¡Reserva confirmada con éxito!');
      handleClose();
    } catch (err) {
      if (err.response?.status === 401 || err.response?.status === 403) {
        setMostrarModalLogin(true);
      } else {
        alert(err.response?.data?.message || 'Error al confirmar la reserva.');
      }
      setIsSubmitting(false); 
    }
  };

  const handleReservarClick = () => {
    if (!user) {
      setMostrarModalLogin(true);
      return;
    }
    if (!isSubmitting) procesarReservaBackend();
  };

  const handleLoginSuccess = () => {
    setMostrarModalLogin(false); 
    procesarReservaBackend();    
  };

  if (!isOpen) return null;

  return (
    <>
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50 animate-fadeIn">
        <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-2xl">
          <h2 className="text-xl font-bold text-gray-900 mb-1">Reservar Turno</h2>
          <p className="text-gray-500 text-sm mb-4">Para: <span className="font-semibold text-gray-700">{servicio?.nombre}</span></p>
          
          <label className="block text-sm font-bold text-gray-700 mb-2">1. Seleccioná la fecha</label>
          <input 
            type="date" 
            min={new Date().toISOString().split('T')[0]}
            className="w-full border border-gray-300 p-2 rounded-lg mb-4 focus:ring-2 focus:ring-blue-500 focus:outline-none" 
            onChange={(e) => setFecha(e.target.value)} 
            value={fecha}
          />

          <label className="block text-sm font-bold text-gray-700 mb-2">2. Horarios disponibles</label>
          
          {cargandoHorarios ? (
            <div className="flex justify-center py-6">
              <div className="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-600"></div>
            </div>
          ) : fecha ? (
            <div className="grid grid-cols-3 gap-2 mb-6 max-h-48 overflow-y-auto p-1 pr-2">
              {horarios.length > 0 ? (
                horarios.map((slot) => (
                  <button 
                    key={slot.fechaHoraISO} 
                    disabled={!slot.disponible || isSubmitting} 
                    onClick={() => setHorarioSeleccionadoISO(slot.fechaHoraISO)}
                    className={`p-2 rounded-lg border text-sm font-medium transition-colors ${
                      !slot.disponible 
                        ? 'bg-gray-100 text-gray-400 border-gray-200 cursor-not-allowed line-through' 
                        : horarioSeleccionadoISO === slot.fechaHoraISO 
                          ? 'bg-blue-600 text-white border-blue-600 shadow-md' 
                          : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50 hover:border-blue-300' 
                    }`}
                  >
                    {slot.hora}
                  </button>
                ))
              ) : (
                <p className="col-span-3 text-sm text-gray-500 text-center py-4 bg-gray-50 rounded-lg border border-dashed border-gray-200">
                  No hay horarios disponibles para este día.
                </p>
              )}
            </div>
          ) : (
            <div className="text-sm text-gray-500 mb-6 italic bg-gray-50 p-3 rounded-lg border border-dashed border-gray-200 text-center">
              Primero seleccioná una fecha.
            </div>
          )}

          <div className="flex gap-3">
            <button 
              onClick={handleClose} 
              disabled={isSubmitting} 
              className="flex-1 py-2 text-gray-600 bg-gray-100 hover:bg-gray-200 rounded-lg font-medium transition-colors disabled:opacity-50"
            >
              Cancelar
            </button>
            <button 
              onClick={handleReservarClick}
              disabled={!fecha || !horarioSeleccionadoISO || isSubmitting}
              className={`flex-1 py-2 rounded-lg font-medium transition-colors shadow-sm ${
                !fecha || !horarioSeleccionadoISO || isSubmitting
                  ? 'bg-blue-300 text-white cursor-not-allowed' 
                  : 'bg-blue-600 text-white hover:bg-blue-700'
              }`}
            >
              {isSubmitting ? 'Confirmando...' : 'Confirmar Reserva'}
            </button>
          </div>
        </div>
      </div>

      {/* --- EL BLINDAJE: MODALES CONECTADOS --- */}
      
      <Login 
        isModal={true}
        isOpen={mostrarModalLogin} 
        onClose={() => setMostrarModalLogin(false)} 
        onSuccess={handleLoginSuccess} 
        // Si no tiene cuenta, cerramos Login y abrimos Registro sin salir de la página
        onSwitchToRegister={() => {
          setMostrarModalLogin(false);
          setMostrarModalRegistro(true);
        }}
      />

      <Register 
        isModal={true}
        isOpen={mostrarModalRegistro} 
        onClose={() => setMostrarModalRegistro(false)} 
        // Si se registra con éxito, lo mandamos al Login sin salir de la página
        onSuccess={() => {
          setMostrarModalRegistro(false);
          setMostrarModalLogin(true);
        }}
        // Si se arrepiente y quiere loguearse, volvemos atrás
        onSwitchToLogin={() => {
          setMostrarModalRegistro(false);
          setMostrarModalLogin(true);
        }}
      />
    </>
  );
};

export default ModalReserva;