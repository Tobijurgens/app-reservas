import { useContext, useEffect, useRef, useState } from 'react';
import { useParams, useSearchParams, Link } from 'react-router-dom';
import { RecursosService } from '../../../services/RecursosService';
import { AuthContext } from '../../../context/AuthContext';
import { ThumbsDown, TriangleAlert, MailOpen} from 'lucide-react';

const estadoDelError = (error) => {
  const status = error.response?.status;
  if (status === 410) return 'vencida';
  if (status === 400 || status === 404 || status === 409) return 'invalida';
  return 'errorConexion';
};

const mensajes = {
  cargando: ['Cargando invitación...', 'Esperá mientras consultamos la información.'],
  pendiente: ['Tenés una invitación', 'Elegí si querés administrar este recurso.'],
  procesando_aceptar: ['Aceptando invitación...', 'Esperá mientras confirmamos tu respuesta.'],
  procesando_rechazar: ['Rechazando invitación...', 'Esperá mientras confirmamos tu respuesta.'],
  aceptada: ['¡Invitación aceptada!', 'Ya podés administrar este recurso desde Mis Trabajos con la cuenta que recibió el correo.'],
  rechazada: ['Invitación rechazada', 'Informaremos al propietario. No se habilitó el acceso a la administración.'],
  vencida: ['Invitación vencida', 'Este enlace venció o pertenece a una invitación anterior sin vencimiento. Pedile al propietario que renueve la asignación del recurso.'],
  invalida: ['Invitación no disponible', 'El enlace no es válido, ya fue utilizado o la asignación cambió. Verificá que hayas abierto el correo más reciente.'],
  errorConexion: ['No pudimos confirmar el estado', 'Revisá tu conexión y volvé a consultar. Si enviaste una decisión, pudo haberse guardado; la consulta no volverá a procesarla.'],
};

const ConfirmarInvitacion = ({ token, enlaceValido }) => {
  const { user } = useContext(AuthContext);
  const [estado, setEstado] = useState(enlaceValido ? 'cargando' : 'invalida');
  const [invitacion, setInvitacion] = useState(null);
  const [intento, setIntento] = useState(0);
  const enviando = useRef(false);

  useEffect(() => {
    if (!enlaceValido) return;
    let vigente = true;
    RecursosService.obtenerInvitacion(token).then((datos) => {
      if (!vigente) return;
      setInvitacion(datos);
      setEstado('pendiente');
    }).catch((error) => {
      if (vigente) setEstado(estadoDelError(error));
    });
    return () => { vigente = false; };
  }, [token, enlaceValido, intento]);

  const responder = async (decision) => {
    if (enviando.current || estado !== 'pendiente') return;
    enviando.current = true;
    setEstado(`procesando_${decision}`);
    try {
      await RecursosService.procesarInvitacion(decision, token);
      setEstado(decision === 'aceptar' ? 'aceptada' : 'rechazada');
    } catch (error) {
      setEstado(estadoDelError(error));
    } finally {
      enviando.current = false;
    }
  };

  const procesando = estado.startsWith('procesando_');
  const esperando = estado === 'cargando' || procesando;
  const confirmar = estado === 'pendiente' || procesando;
  const error = ['vencida', 'invalida', 'errorConexion'].includes(estado);
  const [titulo, descripcion] = mensajes[estado];

  return (
    <div className="flex flex-col items-center justify-center min-h-[calc(100vh-4rem)] px-4 py-8 text-center animate-fadeIn">
      <meta name="referrer" content="no-referrer" />
      <div className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 max-w-md w-full" aria-busy={esperando}>
        <div aria-live="polite" role="status">
          {esperando ? (
            <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-app-teal mx-auto mb-6" aria-hidden="true" />
          ) : (
            <div className="w-16 h-16 bg-app-white rounded-full flex items-center justify-center mx-auto mb-4" aria-hidden="true">
              {error ? <TriangleAlert className="h-8 w-8 text-app-black" />
                : estado === 'rechazada' ? <ThumbsDown className="h-10 w-10 text-app-marineblue" />
                  : <MailOpen className="h-10 w-10 text-app-marineblue" />}
            </div>
          )}
          <h1 className="text-2xl font-bold text-app-black mb-3">{titulo}</h1>
          <p className="text-sm text-gray-600 mb-6">{descripcion}</p>
        </div>

        {confirmar && invitacion && (
          <div className="bg-gray-50 border border-gray-100 rounded-xl p-4 text-center text-sm mb-6 break-words">
            <p><span className="font-semibold">Negocio:</span> {invitacion.nombreNegocio}</p>
            <p className="mt-2"><span className="font-semibold">Recurso:</span> {invitacion.nombreRecurso}</p>
            <p className="mt-3 text-gray-600">La invitación tiene una validez de 7 días desde su emisión. Tu decisión solo se confirma al seleccionar una opción.</p>
          </div>
        )}

        {confirmar && (
          <div className="flex flex-col gap-3">
            <button type="button" disabled={procesando} onClick={() => responder('aceptar')}
              className="w-full bg-app-marineblue text-white py-3 rounded-xl font-bold hover:bg-app-black/90 transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
              {estado === 'procesando_aceptar' ? 'Aceptando...' : 'Aceptar invitación'}
            </button>
            <button type="button" disabled={procesando} onClick={() => responder('rechazar')}
              className="w-full bg-app-black/10 py-3 rounded-xl font-bold hover:bg-app-red/80 hover:text-app-white transition-colors disabled:opacity-50 disabled:cursor-not-allowed">
              {estado === 'procesando_rechazar' ? 'Rechazando...' : 'Rechazar invitación'}
            </button>
          </div>
        )}

        {estado === 'errorConexion' && (
          <button type="button" onClick={() => { setEstado('cargando'); setIntento((valor) => valor + 1); }}
            className="w-full bg-app-marineblue text-white py-3 rounded-xl font-bold hover:bg-app-black/90 transition-colors mb-4">
            Volver a consultar
          </button>
        )}

        {!esperando && (
          <Link to={user ? '/dashboard' : '/login'} className="block mt-6 text-app-marineblue font-semibold hover:underline">
            Ir a mi cuenta
          </Link>
        )}
      </div>
    </div>
  );
};

const ProcesarInvitacion = () => {
  const { accion } = useParams();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token');
  const enlaceValido = Boolean(token) && ['confirmar', 'aceptar', 'rechazar'].includes(accion);
  return <ConfirmarInvitacion key={`${accion}:${token}`} token={token} enlaceValido={enlaceValido} />;
};

export default ProcesarInvitacion;
