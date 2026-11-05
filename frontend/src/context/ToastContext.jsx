import { createContext, useState, useContext, useCallback } from 'react';

export const ToastContext = createContext();

export const useToast = () => useContext(ToastContext);

export const ToastProvider = ({ children }) => {
  const [toasts, setToasts] = useState([]);

  // Función para agregar un toast. Tipos: 'success', 'error', 'warning'
  const addToast = useCallback((mensaje, tipo = 'success') => {
    const id = Date.now();
    setToasts((prev) => [...prev, { id, mensaje, tipo }]);

    // Se auto-elimina a los 4 segundos
    setTimeout(() => {
      setToasts((prev) => prev.filter((toast) => toast.id !== id));
    }, 4000);
  }, []);

  const removeToast = (id) => {
    setToasts((prev) => prev.filter((toast) => toast.id !== id));
  };

  return (
    <ToastContext.Provider value={{ addToast }}>
      {children}
      
      {/* Contenedor fijo donde se apilan las notificaciones */}
      <div className="fixed bottom-6 right-6 z-100 flex flex-col gap-3 pointer-events-none">
        {toasts.map((toast) => (
          <div 
            key={toast.id} 
            className={`flex items-center gap-3 px-5 py-4 rounded-2xl shadow-xl min-w-75 max-w-md animate-fadeIn pointer-events-auto transition-all transform ${
              toast.tipo === 'success' ? 'bg-app-marineblue text-white' : 
              toast.tipo === 'error' ? 'bg-app-black text-white' : 
              'bg-app-ligthblue text-app-black'
            }`}
          >
            <span className="text-xl">
              {toast.tipo === 'success' ? '✓' : toast.tipo === 'error' ? '✕' : '⚠️'}
            </span>
            <p className="flex-1 text-sm font-medium">{toast.mensaje}</p>
            <button 
              onClick={() => removeToast(toast.id)}
              className="opacity-70 hover:opacity-100 transition-opacity"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" /></svg>
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
};