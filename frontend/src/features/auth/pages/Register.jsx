import { useState, useContext, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { AuthContext } from '../../../context/AuthContext';
import api from '../../../services/api';

// EL SECRETO ESTÁ ACÁ: Agregamos isModal = false
const Register = ({ isModal = false, isOpen = true, onClose = null, onSuccess = null, onSwitchToLogin = null }) => {
  const [formData, setFormData] = useState({
    nombre: '',
    apellido: '',
    telefono: '',
    email: '',
    password: '',
    confirmPassword: ''
  });
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  
  const { user } = useContext(AuthContext);
  const navigate = useNavigate();

  // CORRECCIÓN CLAVE: Usamos isModal en vez de onSuccess
  useEffect(() => {
    if (user && !isModal) {
      navigate('/dashboard', { replace: true });
    }
  }, [user, navigate, isModal]);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.id]: e.target.value
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (formData.password !== formData.confirmPassword) {
      setError('Las contraseñas no coinciden');
      return;
    }

    if (formData.password.length < 6) {
      setError('La contraseña debe tener al menos 6 caracteres');
      return;
    }

    setIsLoading(true);

    try {
      await api.post('/auth/register', {
        nombre: formData.nombre,
        apellido: formData.apellido,
        telefono: formData.telefono,
        email: formData.email,
        password: formData.password
      });

      if (onSuccess) {
        onSuccess();
      } else {
        navigate('/login');
      }
      
    } catch (err) {
      setError(
        err.response?.data?.message || 
        'Ocurrió un error al crear la cuenta. Intente nuevamente.'
      );
    } finally {
      setIsLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[60] bg-black bg-opacity-50 flex items-center justify-center p-4 animate-fadeIn">
      <div className="w-full max-w-md bg-white rounded-xl shadow-2xl p-8 relative max-h-[95vh] overflow-y-auto">
        
        {onClose && (
          <button 
            onClick={onClose} 
            className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 transition-colors focus:outline-none"
            title="Cerrar"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        )}

        <div className="text-center mb-8">
          <h2 className="text-2xl font-bold text-gray-900">Crear una cuenta</h2>
          <p className="text-gray-500 mt-2 text-sm">Sumate para gestionar tus turnos o tu negocio</p>
        </div>

        {error && (
          <div className="bg-red-50 border-l-4 border-red-500 p-3 mb-6 rounded-md">
            <p className="text-red-700 text-sm font-medium">{error}</p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-gray-700 text-xs font-bold mb-1" htmlFor="nombre">Nombre</label>
              <input id="nombre" type="text" className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow text-sm" placeholder="Ej: Juan" value={formData.nombre} onChange={handleChange} required />
            </div>
            <div>
              <label className="block text-gray-700 text-xs font-bold mb-1" htmlFor="apellido">Apellido</label>
              <input id="apellido" type="text" className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow text-sm" placeholder="Ej: Pérez" value={formData.apellido} onChange={handleChange} required />
            </div>
          </div>

          <div>
            <label className="block text-gray-700 text-xs font-bold mb-1" htmlFor="telefono">Teléfono</label>
            <input id="telefono" type="tel" className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow text-sm" placeholder="Ej: +54 9 351 1234567" value={formData.telefono} onChange={handleChange} required />
          </div>

          <div>
            <label className="block text-gray-700 text-xs font-bold mb-1" htmlFor="email">Correo Electrónico</label>
            <input id="email" type="email" className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow text-sm" placeholder="tu@email.com" value={formData.email} onChange={handleChange} required />
          </div>

          <div>
            <label className="block text-gray-700 text-xs font-bold mb-1" htmlFor="password">Contraseña</label>
            <input id="password" type="password" className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow text-sm" placeholder="Mínimo 6 caracteres" value={formData.password} onChange={handleChange} required />
          </div>

          <div>
            <label className="block text-gray-700 text-xs font-bold mb-1" htmlFor="confirmPassword">Confirmar Contraseña</label>
            <input id="confirmPassword" type="password" className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow text-sm" placeholder="Repetí tu contraseña" value={formData.confirmPassword} onChange={handleChange} required />
          </div>

          <button type="submit" disabled={isLoading} className={`w-full text-white font-bold py-2.5 px-4 rounded-lg transition-colors mt-2 ${isLoading ? 'bg-blue-400 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700 shadow-sm'}`}>
            {isLoading ? 'Creando cuenta...' : 'Registrarme'}
          </button>
        </form>

        <div className="mt-5 text-center">
          <p className="text-sm text-gray-600">
            ¿Ya tenés una cuenta?{' '}
            {onSwitchToLogin ? (
              <button type="button" onClick={onSwitchToLogin} className="font-semibold text-blue-600 hover:text-blue-800 transition-colors bg-transparent border-none p-0 cursor-pointer">
                Iniciá sesión
              </button>
            ) : (
              <Link to="/login" className="font-semibold text-blue-600 hover:text-blue-800 transition-colors">
                Iniciá sesión
              </Link>
            )}
          </p>
        </div>
      </div>
    </div>
  );
};

export default Register;
// import { useState, useContext, useEffect } from 'react';
// import { useNavigate, Link } from 'react-router-dom';
// import { AuthContext } from '../../../context/AuthContext';
// import api from '../../../services/api';

// const Register = ({ isOpen = true, onClose = null, onSuccess = null, onSwitchToLogin = null }) => {
//   const [formData, setFormData] = useState({
//     nombre: '',
//     apellido: '',
//     telefono: '',
//     email: '',
//     password: '',
//     confirmPassword: ''
//   });
//   const [error, setError] = useState(null);
//   const [isLoading, setIsLoading] = useState(false);
  
//   const { user } = useContext(AuthContext);
//   const navigate = useNavigate();

//   // CORRECCIÓN CLAVE: Agregamos "!onSuccess"
//   useEffect(() => {
//     if (user && !onSuccess) {
//       navigate('/dashboard', { replace: true });
//     }
//   }, [user, navigate, onSuccess]);

//   const handleChange = (e) => {
//     setFormData({
//       ...formData,
//       [e.target.id]: e.target.value
//     });
//   };

//   const handleSubmit = async (e) => {
//     e.preventDefault();
//     setError(null);

//     if (formData.password !== formData.confirmPassword) {
//       setError('Las contraseñas no coinciden');
//       return;
//     }

//     if (formData.password.length < 6) {
//       setError('La contraseña debe tener al menos 6 caracteres');
//       return;
//     }

//     setIsLoading(true);

//     try {
//       await api.post('/auth/register', {
//         nombre: formData.nombre,
//         apellido: formData.apellido,
//         telefono: formData.telefono,
//         email: formData.email,
//         password: formData.password
//       });

//       // Si estamos en un Modal y el registro fue exitoso, ejecutamos la acción
//       // (generalmente será pasarlo al modal de Login)
//       if (onSuccess) {
//         onSuccess();
//       } else {
//         navigate('/login');
//       }
      
//     } catch (err) {
//       setError(
//         err.response?.data?.message || 
//         'Ocurrió un error al crear la cuenta. Intente nuevamente.'
//       );
//     } finally {
//       setIsLoading(false);
//     }
//   };

//   // Si se usa como Modal y está cerrado, no dibuja nada
//   if (!isOpen) return null;

//   return (
//     <div className="fixed inset-0 z-[60] bg-black bg-opacity-50 flex items-center justify-center p-4 animate-fadeIn">
//       {/* Agregamos overflow-y-auto por si la pantalla del celular es muy bajita */}
//       <div className="w-full max-w-md bg-white rounded-xl shadow-2xl p-8 relative max-h-[95vh] overflow-y-auto">
        
//         {/* Botón de cierre: Solo se muestra si nos pasan la función onClose */}
//         {onClose && (
//           <button 
//             onClick={onClose} 
//             className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 transition-colors focus:outline-none"
//             title="Cerrar"
//           >
//             <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
//           </button>
//         )}

//         <div className="text-center mb-8">
//           <h2 className="text-2xl font-bold text-gray-900">
//             Crear una cuenta
//           </h2>
//           <p className="text-gray-500 mt-2 text-sm">
//             Sumate para gestionar tus turnos o tu negocio
//           </p>
//         </div>

//         {error && (
//           <div className="bg-red-50 border-l-4 border-red-500 p-3 mb-6 rounded-md">
//             <p className="text-red-700 text-sm font-medium">{error}</p>
//           </div>
//         )}

//         <form onSubmit={handleSubmit} className="space-y-4">
//           <div className="grid grid-cols-2 gap-4">
//             <div>
//               <label className="block text-gray-700 text-xs font-bold mb-1" htmlFor="nombre">
//                 Nombre
//               </label>
//               <input
//                 id="nombre"
//                 type="text"
//                 className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow text-sm"
//                 placeholder="Ej: Juan"
//                 value={formData.nombre}
//                 onChange={handleChange}
//                 required
//               />
//             </div>
//             <div>
//               <label className="block text-gray-700 text-xs font-bold mb-1" htmlFor="apellido">
//                 Apellido
//               </label>
//               <input
//                 id="apellido"
//                 type="text"
//                 className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow text-sm"
//                 placeholder="Ej: Pérez"
//                 value={formData.apellido}
//                 onChange={handleChange}
//                 required
//               />
//             </div>
//           </div>

//           <div>
//             <label className="block text-gray-700 text-xs font-bold mb-1" htmlFor="telefono">
//               Teléfono
//             </label>
//             <input
//               id="telefono"
//               type="tel"
//               className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow text-sm"
//               placeholder="Ej: +54 9 351 1234567"
//               value={formData.telefono}
//               onChange={handleChange}
//               required
//             />
//           </div>

//           <div>
//             <label className="block text-gray-700 text-xs font-bold mb-1" htmlFor="email">
//               Correo Electrónico
//             </label>
//             <input
//               id="email"
//               type="email"
//               className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow text-sm"
//               placeholder="tu@email.com"
//               value={formData.email}
//               onChange={handleChange}
//               required
//             />
//           </div>

//           <div>
//             <label className="block text-gray-700 text-xs font-bold mb-1" htmlFor="password">
//               Contraseña
//             </label>
//             <input
//               id="password"
//               type="password"
//               className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow text-sm"
//               placeholder="Mínimo 6 caracteres"
//               value={formData.password}
//               onChange={handleChange}
//               required
//             />
//           </div>

//           <div>
//             <label className="block text-gray-700 text-xs font-bold mb-1" htmlFor="confirmPassword">
//               Confirmar Contraseña
//             </label>
//             <input
//               id="confirmPassword"
//               type="password"
//               className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 transition-shadow text-sm"
//               placeholder="Repetí tu contraseña"
//               value={formData.confirmPassword}
//               onChange={handleChange}
//               required
//             />
//           </div>

//           <button
//             type="submit"
//             disabled={isLoading}
//             className={`w-full text-white font-bold py-2.5 px-4 rounded-lg transition-colors mt-2 ${
//               isLoading ? 'bg-blue-400 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700 shadow-sm'
//             }`}
//           >
//             {isLoading ? 'Creando cuenta...' : 'Registrarme'}
//           </button>
//         </form>

//         <div className="mt-5 text-center">
//           <p className="text-sm text-gray-600">
//             ¿Ya tenés una cuenta?{' '}
//             {onSwitchToLogin ? (
//               // Si estamos en un Modal, ejecutamos la función para volver al Login
//               <button 
//                 type="button" 
//                 onClick={onSwitchToLogin} 
//                 className="font-semibold text-blue-600 hover:text-blue-800 transition-colors bg-transparent border-none p-0 cursor-pointer"
//               >
//                 Iniciá sesión
//               </button>
//             ) : (
//               // Si estamos en la página normal, usamos el Link de React Router
//               <Link to="/login" className="font-semibold text-blue-600 hover:text-blue-800 transition-colors">
//                 Iniciá sesión
//               </Link>
//             )}
//           </p>
//         </div>

//       </div>
//     </div>
//   );
// };

// export default Register;