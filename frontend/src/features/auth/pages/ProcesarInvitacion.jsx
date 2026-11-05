import { useEffect, useRef, useState } from 'react';
import { useParams, useSearchParams, useNavigate, Link } from 'react-router-dom';
import { RecursosService } from '../../../services/RecursosService';
import { useToast } from '../../../context/ToastContext';
import { ThumbsDown, ThumbsUp, TriangleAlert } from 'lucide-react';

const ProcesarInvitacion = () => {
  const { accion } = useParams();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  
  const navigate = useNavigate();
  const { addToast } = useToast();
  
  const peticionEnviada = useRef(false);
  const [estado, setEstado] = useState('procesando');

  useEffect(() => {
    if (peticionEnviada.current) return;
    peticionEnviada.current = true;

    if (!token || (accion !== 'aceptar' && accion !== 'rechazar')) {
      addToast('Enlace de invitación inválido o incompleto.', 'error');
      navigate('/');
      return;
    }

    const enviarRespuesta = async () => {
      try {
        await RecursosService.procesarInvitacion(accion, token);
        setEstado('exito');
        
        if (accion === 'aceptar') {
          addToast('¡Invitación aceptada! Ya podés ver el espacio en tu panel.', 'success');
        } else {
          addToast('Invitación rechazada correctamente.', 'success');
        }
        
      } catch (error) {
        setEstado('error');
        addToast(
          error.response?.data?.message || error.response?.data || 'Error al procesar la invitación. Es posible que el enlace haya expirado.', 
          'error'
        );
      }
    };

    enviarRespuesta();
  }, [accion, token, navigate, addToast]);

  return (
    <div className="flex flex-col items-center justify-center min-h-[calc(100vh-4rem)]  px-4 text-center animate-fadeIn">
      {estado === 'procesando' && (
        <>
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-app-teal mb-6"></div>
          <h2 className="text-2xl font-bold text-app-black mb-2">Procesando invitación...</h2>
          <p className=" text-sm">Por favor, esperá un instante.</p>
        </>
      )}

      {estado === 'exito' && (
        <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 max-w-sm w-full">
          <div className="w-16 h-16  bg-app-white rounded-full flex items-center justify-center text-3xl mx-auto mb-4">
            {accion === 'aceptar' ? <ThumbsUp className="h-10 w-10 text-app-marineblue" /> : <ThumbsDown className="h-10 w-10 text-app-marineblue" />}
          </div>
          <h2 className="text-2xl font-bold text-app-black mb-2">
            {accion === 'aceptar' ? '¡Todo listo!' : 'Invitación rechazada'}
          </h2>
          <p className=" text-sm mb-6">
            {accion === 'aceptar' 
              ? 'Ya estás vinculado al negocio. Ingresá a tu panel para ver tus horarios y servicios.' 
              : 'Informaremos al administrador que declinaste la invitación.'}
          </p>
          <Link to="/login" className="block w-full bg-app-marineblue text-white py-3 rounded-xl font-bold hover:bg-app-black/90 transition-colors shadow-sm duration-300">
            Ir a mi cuenta
          </Link>
        </div>
      )}

      {estado === 'error' && (
        <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 max-w-sm w-full">
          <div className="w-16 h-16 bg-app-white text-app-black rounded-full flex items-center justify-center mx-auto mb-4">
            <TriangleAlert className="h-8 w-8 text-app-black" />
          </div>
          <h2 className="text-xl font-bold text-app-black mb-2">Ocurrió un problema</h2>
          <p className=" text-sm mb-6">No pudimos procesar la invitación. Verificá que el enlace esté completo o pedile al administrador que te reenvíe el correo.</p>
          <Link to="/" className="block w-full bg-app-marineblue text-app-white hover:bg-app-black/90 py-3 rounded-xl font-bold transition-colors duration-300">
            Volver al inicio
          </Link>
        </div>
      )}
    </div>
  );
};

export default ProcesarInvitacion;