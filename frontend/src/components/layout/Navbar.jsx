import { useState, useContext, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';

const Navbar = () => {
  const { user, logout } = useContext(AuthContext);
  const navigate = useNavigate();
  
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);

  const handleLogout = () => {
    logout();
    navigate('/'); 
  };

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsDropdownOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const initial = user?.email ? user.email.charAt(0).toUpperCase() : 'U';

  return (
    <nav className="bg-white shadow-sm border-b border-gray-100 w-full relative z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16 items-center">
          
          <div className="flex-shrink-0 flex items-center">
            <Link to={user ? "/dashboard" : "/"} className="text-2xl font-black text-blue-600 tracking-tight hover:text-blue-700 transition-colors">
              AgendApp
            </Link>
          </div>

          <div className="flex items-center space-x-4">
            {user ? (
              <div className="relative" ref={dropdownRef}>
                <button 
                  onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                  className="flex items-center gap-2 focus:outline-none"
                >
                  <span className="text-sm font-medium text-gray-700 hidden sm:block">
                    {user.email}
                  </span>
                  <div className="h-10 w-10 rounded-full bg-blue-600 text-white flex items-center justify-center font-bold shadow-sm hover:bg-blue-700 transition-colors">
                    {initial}
                  </div>
                </button>

                {isDropdownOpen && (
                  <div className="absolute right-0 mt-2 w-52 bg-white rounded-xl shadow-lg border border-gray-100 py-2 animate-fadeIn origin-top-right">
                    
                    <Link 
                      to="/dashboard" 
                      state={{ tab: 'reservas' }}
                      onClick={() => setIsDropdownOpen(false)}
                      className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-blue-50 hover:text-blue-700 transition-colors"
                    >
                      <span className="text-blue-500">📅</span> Mis Reservas
                    </Link>

                    {/* ¡RESTAURADO!: MI AGENDA */}
                    <Link 
                      to="/mi-agenda" 
                      onClick={() => setIsDropdownOpen(false)}
                      className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-blue-50 hover:text-blue-700 transition-colors"
                    >
                      <span className="text-green-600">📆</span> Mi Agenda
                    </Link>
                    
                    <Link 
                      to="/dashboard" 
                      state={{ tab: 'trabajos' }}
                      onClick={() => setIsDropdownOpen(false)}
                      className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-blue-50 hover:text-blue-700 transition-colors"
                    >
                      <span className="text-amber-700">💼</span> Mis Trabajos
                    </Link>

                    <Link 
                      to="/dashboard" 
                      state={{ tab: 'negocios' }}
                      onClick={() => setIsDropdownOpen(false)}
                      className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-blue-50 hover:text-blue-700 transition-colors"
                    >
                      <span className="text-teal-600">🏪</span> Mis Negocios
                    </Link>
                    
                    <Link 
                      to="/perfil" 
                      onClick={() => setIsDropdownOpen(false)}
                      className="flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-blue-50 hover:text-blue-700 transition-colors"
                    >
                      <span className="text-purple-500">⚙️</span> Mi Perfil
                    </Link>

                    <div className="border-t border-gray-100 my-1"></div>
                    
                    <button
                      onClick={handleLogout}
                      className="flex w-full items-center gap-2 text-left px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition-colors font-bold"
                    >
                      <span className="text-red-500">🚪</span> Cerrar Sesión
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <>
                <Link to="/" className="text-gray-600 hover:text-blue-600 px-3 py-2 rounded-md text-sm font-medium transition-colors">
                  Inicio
                </Link>
                <Link
                  to="/login"
                  className="bg-blue-600 hover:bg-blue-700 text-white px-5 py-2.5 rounded-lg text-sm font-medium transition-colors shadow-sm"
                >
                  Iniciar Sesión
                </Link>
              </>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;