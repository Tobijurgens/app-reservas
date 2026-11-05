import { useState, useContext, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { AuthContext } from '../../../context/AuthContext';
import { useToast } from '../../../context/ToastContext';

const Login = ({ isModal = false, isOpen = true, onClose = null, onSuccess = null, onSwitchToRegister = null }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  
  const { login, user } = useContext(AuthContext); 
  const { addToast } = useToast();
  const navigate = useNavigate();

  useEffect(() => {
    if (user && !isModal) navigate('/dashboard', { replace: true });
  }, [user, navigate, isModal]);

  const handleSubmit = async (e) => {
    e.preventDefault(); 
    setIsLoading(true);

    const result = await login(email, password);

    if (result.success) {
      addToast('¡Ingreso exitoso!', 'success');
      if (isModal && onSuccess) onSuccess();
      else navigate('/dashboard');
    } else {
      addToast(result.message || 'Verificá tus credenciales o si activaste tu email.', 'error');
      setIsLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-60 bg-app-black/40 backdrop-blur-sm flex items-center justify-center p-4 animate-fadeIn">
      <div className="w-full max-w-md bg-app-white rounded-2xl shadow-2xl p-8 relative border border-gray-100">
        
        {/* BOTÓN VOLVER (Solo para la vista de página completa, no modal) */}
        {/* BOTÓN VOLVER (Solo para la vista de página completa, no modal) */}
        {!isModal && (
          <button 
            onClick={() => navigate('/')} 
            className="absolute top-4 left-4 text-app-marineblue hover:text-app-black transition-colors focus:outline-none flex items-center gap-1 text-sm font-medium"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7" />
            </svg>
          </button>
        )}

        {/* BOTÓN CERRAR MODAL (Ya lo tenías, solo para la vista modal) */}
        {onClose && (
          <button onClick={onClose} className="absolute top-4 right-4 text-app-marineblue hover:text-app-black transition-colors focus:outline-none">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        )}

        <div className="text-center mb-6 mt-2">
          <h2 className="text-2xl font-bold text-app-black">Iniciar Sesión</h2>
          <p className="text-sm text-app-black mt-1">Ingresá a tu cuenta para continuar</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-app-marineblue text-xs font-bold mb-1" htmlFor="email">Correo Electrónico</label>
            <input id="email" type="email" className="w-full px-3 py-3 bg-white border-none rounded-xl shadow-sm focus:outline-none focus:ring-2 focus:ring-app-marineblue transition-shadow text-sm text-app-black" placeholder="tu@email.com" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </div>

          <div>
            <label className="block text-app-marineblue text-xs font-bold mb-1" htmlFor="password">Contraseña</label>
            <input id="password" type="password" className="w-full px-3 py-3 bg-white border-none rounded-xl shadow-sm focus:outline-none focus:ring-2 focus:ring-app-marineblue transition-shadow text-sm text-app-black" placeholder="********" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </div>

          <button type="submit" disabled={isLoading} className={`w-full text-white font-bold py-3 px-4 rounded-xl transition-all shadow-sm mt-4 ${isLoading ? 'bg-gray-400 cursor-not-allowed' : 'bg-app-marineblue hover:bg-app-black/90 hover:shadow-md transition-colors duration-300'}`}>
            {isLoading ? 'Ingresando...' : 'Entrar'}
          </button>
        </form>

        <div className="mt-6 text-center text-sm text-app-black">
          ¿No tenés una cuenta?{' '}
          {onSwitchToRegister ? (
            <button type="button" onClick={onSwitchToRegister} className="font-bold text-app-marineblue hover:text-app-marineblue transition-colors bg-transparent border-none p-0 cursor-pointer">Registrate acá</button>
          ) : (
            <Link to="/register" className="font-bold text-app-marineblue hover:text-app-marineblue transition-colors">Registrate acá</Link>
          )}
        </div>
      </div>
    </div>
  );
};

export default Login;