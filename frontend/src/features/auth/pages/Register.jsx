import { useState, useContext, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { AuthContext } from '../../../context/AuthContext';
import { AuthService } from '../../../services/AuthService';
import { useToast } from '../../../context/ToastContext';

const Register = ({ isModal = false, isOpen = true, onClose = null, onSuccess = null, onSwitchToLogin = null }) => {
  const [formData, setFormData] = useState({
    nombre: '', apellido: '', telefono: '', email: '', password: '', confirmPassword: ''
  });
  const [isLoading, setIsLoading] = useState(false);
  
  const { user } = useContext(AuthContext);
  const { addToast } = useToast();
  const navigate = useNavigate();

  useEffect(() => {
    if (user && !isModal) navigate('/dashboard', { replace: true });
  }, [user, navigate, isModal]);

  const handleChange = (e) => setFormData({ ...formData, [e.target.id]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (formData.password !== formData.confirmPassword) {
      addToast('Las contraseñas no coinciden', 'error');
      return;
    }

    if (formData.password.length < 6) {
      addToast('La contraseña debe tener al menos 6 caracteres', 'error');
      return;
    }

    setIsLoading(true);

    try {
      await AuthService.register({
        nombre: formData.nombre,
        apellido: formData.apellido,
        telefono: formData.telefono,
        email: formData.email,
        password: formData.password
      });

      addToast('¡Cuenta creada! Revisá tu bandeja de entrada para verificar tu email antes de ingresar.', 'success');

      if (onSuccess) onSuccess();
      else navigate('/login');
      
    } catch (err) {
      addToast(err.response?.data?.message || 'Ocurrió un error al crear la cuenta.', 'error');
    } finally {
      setIsLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-60 bg-app-black/40 backdrop-blur-sm flex items-center justify-center p-4 animate-fadeIn">
      <div className="w-full max-w-md bg-app-white rounded-2xl shadow-2xl p-8 relative max-h-[95vh] overflow-y-auto border border-gray-100">
        
        {onClose && (
          <button onClick={onClose} className="absolute top-4 right-4 text-app-marineblue hover:text-app-black transition-colors focus:outline-none">
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        )}

        <div className="text-center mb-8">
          <h2 className="text-2xl font-bold text-app-black">Crear una cuenta</h2>
          <p className="text-app-black mt-2 text-sm">Sumate para gestionar tus turnos o tu negocio</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-app-marineblue text-xs font-bold mb-1" htmlFor="nombre">Nombre</label>
              <input id="nombre" type="text" className="w-full px-3 py-3 bg-white border-none rounded-xl shadow-sm focus:outline-none focus:ring-2 focus:ring-app-marineblue transition-shadow text-sm text-app-black" placeholder="Ej: Juan" value={formData.nombre} onChange={handleChange} required />
            </div>
            <div>
              <label className="block text-app-marineblue text-xs font-bold mb-1" htmlFor="apellido">Apellido</label>
              <input id="apellido" type="text" className="w-full px-3 py-3 bg-white border-none rounded-xl shadow-sm focus:outline-none focus:ring-2 focus:ring-app-marineblue transition-shadow text-sm text-app-black" placeholder="Ej: Pérez" value={formData.apellido} onChange={handleChange} required />
            </div>
          </div>

          <div>
            <label className="block text-app-marineblue text-xs font-bold mb-1" htmlFor="telefono">Teléfono</label>
            <input id="telefono" type="tel" className="w-full px-3 py-3 bg-white border-none rounded-xl shadow-sm focus:outline-none focus:ring-2 focus:ring-app-marineblue transition-shadow text-sm text-app-black" placeholder="Ej: +54 9 351 1234567" value={formData.telefono} onChange={handleChange} required />
          </div>

          <div>
            <label className="block text-app-marineblue text-xs font-bold mb-1" htmlFor="email">Correo Electrónico</label>
            <input id="email" type="email" className="w-full px-3 py-3 bg-white border-none rounded-xl shadow-sm focus:outline-none focus:ring-2 focus:ring-app-marineblue transition-shadow text-sm text-app-black" placeholder="tu@email.com" value={formData.email} onChange={handleChange} required />
          </div>

          <div>
            <label className="block text-app-marineblue text-xs font-bold mb-1" htmlFor="password">Contraseña</label>
            <input id="password" type="password" className="w-full px-3 py-3 bg-white border-none rounded-xl shadow-sm focus:outline-none focus:ring-2 focus:ring-app-marineblue transition-shadow text-sm text-app-black" placeholder="Mínimo 6 caracteres" value={formData.password} onChange={handleChange} required />
          </div>

          <div>
            <label className="block text-app-marineblue text-xs font-bold mb-1" htmlFor="confirmPassword">Confirmar Contraseña</label>
            <input id="confirmPassword" type="password" className="w-full px-3 py-3 bg-white border-none rounded-xl shadow-sm focus:outline-none focus:ring-2 focus:ring-app-marineblue transition-shadow text-sm text-app-black" placeholder="Repetí tu contraseña" value={formData.confirmPassword} onChange={handleChange} required />
          </div>

          <button type="submit" disabled={isLoading} className={`w-full text-white font-bold py-3 px-4 rounded-xl transition-all shadow-sm mt-4 ${isLoading ? 'bg-gray-400 cursor-not-allowed' : 'bg-app-marineblue hover:bg-app-black/90 hover:shadow-md transition-colors duration-300'}`}>
            {isLoading ? 'Creando cuenta...' : 'Registrarme'}
          </button>
        </form>

        <div className="mt-6 text-center">
          <p className="text-sm text-app-black">
            ¿Ya tenés una cuenta?{' '}
            {onSwitchToLogin ? (
              <button type="button" onClick={onSwitchToLogin} className="font-bold text-app-marineblue hover:text-app-black transition-colors bg-transparent border-none p-0 cursor-pointer">Iniciá sesión</button>
            ) : (
              <Link to="/login" className="font-bold text-app-marineblue hover:text-app-marineblue transition-colors">Iniciá sesión</Link>
            )}
          </p>
        </div>
      </div>
    </div>
  );
};

export default Register;