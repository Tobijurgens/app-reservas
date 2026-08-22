import { useState, useContext, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { AuthContext } from '../../../context/AuthContext';

// EL SECRETO ESTÁ ACÁ: Agregamos isModal = false
const Login = ({ isModal = false, isOpen = true, onClose = null, onSuccess = null, onSwitchToRegister = null }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [isLoading, setIsLoading] = useState(false);
  
  const { login, user } = useContext(AuthContext); 
  const navigate = useNavigate();

  // CORRECCIÓN CLAVE: Usamos isModal en vez de onSuccess
  useEffect(() => {
    if (user && !isModal) {
      navigate('/dashboard', { replace: true });
    }
  }, [user, navigate, isModal]);

  const handleSubmit = async (e) => {
    e.preventDefault(); 
    setError(null);
    setIsLoading(true);

    const result = await login(email, password);

    if (result.success) {
      if (isModal && onSuccess) {
        onSuccess();
      } else {
        navigate('/dashboard');
      }
    } else {
      setError(result.message);
      setIsLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[60] bg-black bg-opacity-50 flex items-center justify-center p-4 animate-fadeIn">
      <div className="w-full max-w-md bg-white rounded-xl shadow-2xl p-8 relative">
        
        {onClose && (
          <button 
            onClick={onClose} 
            className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 transition-colors focus:outline-none"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
          </button>
        )}

        <div className="text-center mb-6">
          <h2 className="text-2xl font-bold text-gray-900">Iniciar Sesión</h2>
          <p className="text-sm text-gray-500 mt-1">Ingresá a tu cuenta para continuar</p>
        </div>

        {error && (
          <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg relative mb-4 text-sm font-medium">
            <span className="block sm:inline">{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="email">Correo Electrónico</label>
            <input
              id="email"
              type="email"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="tu@email.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="password">Contraseña</label>
            <input
              id="password"
              type="password"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
              placeholder="********"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          <button
            type="submit"
            disabled={isLoading}
            className={`w-full text-white font-bold py-2.5 px-4 rounded-lg transition-colors mt-2 ${
              isLoading ? 'bg-blue-400 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700 shadow-sm'
            }`}
          >
            {isLoading ? 'Ingresando...' : 'Entrar'}
          </button>
        </form>

        <div className="mt-6 text-center text-sm text-gray-600">
          ¿No tenés una cuenta?{' '}
          {onSwitchToRegister ? (
            <button 
              type="button" 
              onClick={onSwitchToRegister} 
              className="font-bold text-blue-600 hover:text-blue-500 hover:underline bg-transparent border-none p-0 cursor-pointer"
            >
              Registrate acá
            </button>
          ) : (
            <Link to="/register" className="font-bold text-blue-600 hover:text-blue-500 hover:underline">
              Registrate acá
            </Link>
          )}
        </div>
      </div>
    </div>
  );
};

export default Login;

// import { useState, useContext, useEffect } from 'react';
// import { useNavigate, Link } from 'react-router-dom';
// import { AuthContext } from '../../../context/AuthContext';

// // NUEVO: Agregamos isModal = false a las props
// const Login = ({ isModal = false, isOpen = true, onClose = null, onSuccess = null, onSwitchToRegister = null }) => {
//   const [email, setEmail] = useState('');
//   const [password, setPassword] = useState('');
//   const [error, setError] = useState(null);
//   const [isLoading, setIsLoading] = useState(false);
  
//   const { login, user } = useContext(AuthContext); 
//   const navigate = useNavigate();

//   // CORRECCIÓN CLAVE: Usamos isModal para controlar la redirección
//   useEffect(() => {
//     // Si el usuario está logueado y NO estamos en modo modal, redirigimos al dashboard
//     if (user && !isModal) {
//       navigate('/dashboard', { replace: true });
//     }
//   }, [user, navigate, isModal]);

//   const handleSubmit = async (e) => {
//     e.preventDefault(); 
//     setError(null);
//     setIsLoading(true);

//     const result = await login(email, password);

//     if (result.success) {
//       // Si estamos en modo Modal, llamamos a onSuccess
//       if (isModal && onSuccess) {
//         onSuccess();
//       } else {
//         // Si no es modal, redirigimos al dashboard
//         navigate('/dashboard');
//       }
//     } else {
//       setError(result.message);
//       setIsLoading(false);
//     }
//   };

//   if (!isOpen) return null;

//   return (
//     <div className="fixed inset-0 z-[60] bg-black bg-opacity-50 flex items-center justify-center p-4 animate-fadeIn">
//       <div className="w-full max-w-md bg-white rounded-xl shadow-2xl p-8 relative">
        
//         {onClose && (
//           <button 
//             onClick={onClose} 
//             className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 transition-colors focus:outline-none"
//           >
//             <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
//           </button>
//         )}

//         <div className="text-center mb-6">
//           <h2 className="text-2xl font-bold text-gray-900">Iniciar Sesión</h2>
//           <p className="text-sm text-gray-500 mt-1">Ingresá a tu cuenta para continuar</p>
//         </div>

//         {error && (
//           <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded-lg relative mb-4 text-sm font-medium">
//             <span className="block sm:inline">{error}</span>
//           </div>
//         )}

//         <form onSubmit={handleSubmit} className="space-y-4">
//           <div>
//             <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="email">Correo Electrónico</label>
//             <input
//               id="email"
//               type="email"
//               className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
//               placeholder="tu@email.com"
//               value={email}
//               onChange={(e) => setEmail(e.target.value)}
//               required
//             />
//           </div>

//           <div>
//             <label className="block text-gray-700 text-sm font-bold mb-2" htmlFor="password">Contraseña</label>
//             <input
//               id="password"
//               type="password"
//               className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
//               placeholder="********"
//               value={password}
//               onChange={(e) => setPassword(e.target.value)}
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
//             {isLoading ? 'Ingresando...' : 'Entrar'}
//           </button>
//         </form>

//         <div className="mt-6 text-center text-sm text-gray-600">
//           ¿No tenés una cuenta?{' '}
//           {onSwitchToRegister ? (
//             <button 
//               type="button" 
//               onClick={onSwitchToRegister} 
//               className="font-bold text-blue-600 hover:text-blue-500 hover:underline bg-transparent border-none p-0 cursor-pointer"
//             >
//               Registrate acá
//             </button>
//           ) : (
//             <Link to="/register" className="font-bold text-blue-600 hover:text-blue-500 hover:underline">
//               Registrate acá
//             </Link>
//           )}
//         </div>
//       </div>
//     </div>
//   );
// };

// export default Login;