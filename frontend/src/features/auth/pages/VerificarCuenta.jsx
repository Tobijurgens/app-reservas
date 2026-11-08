import { useEffect, useRef } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { AuthService } from '../../../services/AuthService';
import { useToast } from '../../../context/ToastContext';

import { mensajeError } from '../../../utils/errores';

const VerificarCuenta = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { addToast } = useToast();
  
  // Usamos una referencia para evitar que el StrictMode de React 18 dispare la petición dos veces
  const peticionEnviada = useRef(false);
  const token = searchParams.get('token');

  useEffect(() => {
    if (peticionEnviada.current) return;
    peticionEnviada.current = true;

    if (!token) {
      addToast('Enlace de verificación inválido.', 'error');
      navigate('/login');
      return;
    }

    const confirmarCuenta = async () => {
      try {
        // Llama al endpoint de tu backend de forma transparente
        await AuthService.verificarCuenta(token);
        addToast('¡Cuenta verificada con éxito! Ya podés iniciar sesión.', 'success');
      } catch (error) {
        addToast(
          mensajeError(error, 'Error al verificar la cuenta. Es posible que el enlace haya expirado.'),
          'error'
        );
      } finally {
        navigate('/login');
      }
    };

    confirmarCuenta();
  }, [token, navigate, addToast]);

  return (
    <div className="flex flex-col items-center justify-center min-h-[calc(100vh-4rem)]  px-4 text-center animate-fadeIn">
      <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-app-teal mb-6"></div>
      <h2 className="text-2xl font-bold text-app-black mb-2">Verificando tu cuenta...</h2>
      <p className=" text-sm">Por favor, esperá un instante mientras confirmamos tus datos.</p>
    </div>
  );
};

export default VerificarCuenta;
