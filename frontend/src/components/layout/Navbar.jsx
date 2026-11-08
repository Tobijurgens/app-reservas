import { useState, useContext, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import { useNotificaciones } from '../../hooks/useNotificaciones';
import { Settings, BriefcaseBusiness, SquarePen, Store, CalendarDays, LogOut, Bell} from 'lucide-react';


const Navbar = () => {
  const { user, logout } = useContext(AuthContext);
  const navigate = useNavigate();
  
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);
  const notifRef = useRef(null);

  const { 
    notificaciones, 
    noLeidas, 
    isNotifOpen, 
    setIsNotifOpen, 
    marcarLeida, 
    marcarTodas,
    fetchNotificaciones
  } = useNotificaciones();

  const handleLogout = () => {
    logout();
    navigate('/'); 
  };

  useEffect(() => {
    const handleClickOutside = (event) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsDropdownOpen(false);
      }
      if (notifRef.current && !notifRef.current.contains(event.target)) {
        setIsNotifOpen(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [setIsNotifOpen]);

  const initial = user?.email ? user.email.charAt(0).toUpperCase() : 'U';

  const formatearFecha = (fechaArray) => {
    if (!fechaArray || !Array.isArray(fechaArray)) return '';
    const [, month, day, hour, minute] = fechaArray;
    return `${day.toString().padStart(2, '0')}/${month.toString().padStart(2, '0')} ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}`;
  };

  return (
    <nav className="bg-white shadow-sm border-b border-gray-100 w-full sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16 items-center">
          
          <div className="shrink-0 flex items-center">
            <Link to={user ? "/dashboard" : "/"} className="text-2xl font-black tracking-tight hover:opacity-80 transition-opacity">
              <span className="text-app-black">Agend</span>
              <span className="text-app-marineblue">App</span>
            </Link>
          </div>

          <div className="flex items-center space-x-4 sm:space-x-6">
            {user ? (
              <>
                {/* CAMPANITA DE NOTIFICACIONES */}
                <div className="relative" ref={notifRef}>
                  <button 
                    onClick={() => {
                      setIsNotifOpen(!isNotifOpen);
                      if (!isNotifOpen) fetchNotificaciones(); // Refresca al abrir
                    }}
                    className="relative p-2 text-app-black/80 hover:text-app-marineblue transition focus:outline-none"
                  >
                    <Bell className="w-6 h-6" />
                    
                    {noLeidas > 0 && (
                      <span className="absolute top-1 right-1 flex h-4 w-4 items-center justify-center rounded-full bg-app-ligthblue text-[10px] font-bold text-app-black ring-2 ring-white">
                        {noLeidas > 9 ? '9+' : noLeidas}
                      </span>
                    )}
                  </button>

                  {isNotifOpen && (
                    <div className="absolute right-0 mt-2 w-80 sm:w-96 bg-white rounded-2xl shadow-xl border border-gray-100 py-2 animate-fadeIn origin-top-right overflow-hidden flex flex-col max-h-[80vh]">
                      <div className="px-4 py-3 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
                        <h3 className="font-bold text-app-black">Notificaciones</h3>
                        {noLeidas > 0 && (
                          <button onClick={marcarTodas} className="text-xs font-bold text-app-marineblue hover:text-app-black transition-colors">
                            Marcar leídas
                          </button>
                        )}
                      </div>
                      
                      <div className="overflow-y-auto flex-1">
                        {notificaciones.length > 0 ? (
                          <ul className="divide-y divide-gray-50">
                            {notificaciones.map((notif) => (
                              <li 
                                key={notif.id} 
                                onClick={() => marcarLeida(notif.id, notif.leida)}
                                className={`p-4 hover:bg-gray-50 transition-colors cursor-pointer ${notif.leida ? 'opacity-70 bg-app-white' : 'bg-white'}`}
                              >
                                <div className="flex justify-between items-start mb-1">
                                  <span className={`text-sm ${notif.leida ? 'font-medium text-app-black ' : 'font-bold text-app-marineblue'}`}>
                                    {notif.titulo}
                                  </span>
                                  {!notif.leida && <span className="h-2 w-2 rounded-full bg-app-marineblue shrink-0 mt-1.5"></span>}
                                </div>
                                <p className="text-xs line-clamp-2 leading-relaxed">
                                  {notif.mensaje}
                                </p>
                                <span className="text-[10px] text-gray-400 mt-2 block font-medium">
                                  {formatearFecha(notif.fechaEnvio)}
                                </span>
                              </li>
                            ))}
                          </ul>
                        ) : (
                          <div className="px-4 py-8 text-center text-sm text-gray-400 font-medium">
                            No tenés notificaciones nuevas
                          </div>
                        )}
                      </div>
                    </div>
                  )}
                </div>

                {/* MENÚ DE USUARIO EXISTENTE */}
                <div className="relative" ref={dropdownRef}>
                  <button 
                    onClick={() => setIsDropdownOpen(!isDropdownOpen)}
                    className="flex items-center gap-2 focus:outline-none "
                  >
                    <span className="text-sm font-medium hidden sm:block">
                      {user.email}
                    </span>
                    <div className="h-10 w-10 rounded-full bg-app-marineblue text-white flex items-center justify-center font-bold shadow-sm transition duration-300 ease-in-out hover:scale-110">
                      {initial}
                    </div>
                  </button>

                  {isDropdownOpen && (
                    <div className="absolute right-0 mt-2 w-52 bg-white rounded-2xl shadow-xl border border-gray-100 py-2 animate-fadeIn origin-top-right">
                      <Link to="/dashboard" state={{ tab: 'reservas' }} onClick={() => setIsDropdownOpen(false)} className="font-semibold flex items-center gap-2 px-4 py-2 text-sm text- hover: hover:text-app-marineblue transition-colors">
                        <SquarePen className="w-5 h-5" /> Mis Reservas
                      </Link>
                      <Link to="/mi-agenda" onClick={() => setIsDropdownOpen(false)} className="font-semibold flex items-center gap-2 px-4 py-2 text-sm  hover: hover:text-app-marineblue transition-colors">
                        <CalendarDays className="w-5 h-5" /> Mi Agenda
                      </Link>
                      <Link to="/dashboard" state={{ tab: 'trabajos' }} onClick={() => setIsDropdownOpen(false)} className="font-semibold flex items-center gap-2 px-4 py-2 text-sm  hover: hover:text-app-marineblue transition-colors">
                        <BriefcaseBusiness className="w-5 h-5" /> Mis Trabajos
                      </Link>
                      <Link to="/dashboard" state={{ tab: 'negocios' }} onClick={() => setIsDropdownOpen(false)} className="font-semibold flex items-center gap-2 px-4 py-2 text-sm  hover: hover:text-app-marineblue transition-colors">
                        <Store className="w-5 h-5" /> Mis Negocios
                      </Link>
                      <Link to="/perfil" onClick={() => setIsDropdownOpen(false)} className="font-semibold flex items-center gap-2 px-4 py-2 text-sm  hover: hover:text-app-marineblue transition-colors">
                        <Settings className="w-5 h-5"/> Mi Perfil
                      </Link>

                      <div className="border-t border-gray-100 my-2"></div>
                      
                      <button onClick={handleLogout} className="font-semibold flex w-full items-center gap-2 text-left px-4 py-2 text-sm text-app-red hover:text-app-black transition-colors">
                        <LogOut className="w-5 h-5" /> Cerrar Sesión
                      </button>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <>
                <Link to="/" className=" hover:text-app-black px-3 py-2 rounded-md text-sm font-medium transition-colors">Inicio</Link>
                <Link to="/login" className="bg-app-marineblue hover:bg-app-black/90 text-white px-5 py-2.5 rounded-xl text-sm font-medium transition-colors shadow-sm">Iniciar Sesión</Link>
              </>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
