import { AlertTriangle } from 'lucide-react';

const ModalConfirmacion = ({ 
  isOpen, 
  onClose, 
  onConfirm, 
  isConfirming, 
  titulo, 
  mensaje, 
  textoConfirmar = "Sí, confirmar",
  textoCancelar = "No, volver",
  esPeligroso = true 
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-app-black/40 backdrop-blur-sm flex items-center justify-center p-4 z-50 animate-fadeIn">
      <div className="bg-white rounded-2xl max-w-sm w-full p-8 shadow-2xl border border-gray-100">
        <div className={`flex items-center justify-center w-14 h-14 rounded-full mx-auto mb-5 shadow-sm ${esPeligroso ? 'bg-app-ligthblue text-app-black' : 'bg-gray-100 text-app-marineblue'}`}>
          <AlertTriangle className="w-7 h-7" />
        </div>

        <h3 className="text-xl font-bold text-app-black text-center mb-2">{titulo}</h3>
        <div className="text-sm text-app-marineblue text-center mb-8">
          {mensaje}
        </div>

        <div className="flex gap-3">
          <button 
            onClick={onClose}
            disabled={isConfirming}
            className="flex-1 py-3 text-app-black bg-gray-100 hover:bg-gray-200 rounded-xl font-medium transition-colors"
          >
            {textoCancelar}
          </button>
          <button 
            onClick={onConfirm}
            disabled={isConfirming}
            className={`flex-1 py-3 rounded-xl font-medium transition-colors shadow-sm ${
              isConfirming 
                ? 'bg-gray-300 text-gray-500 cursor-not-allowed' 
                : esPeligroso 
                  ? 'bg-app-black text-white hover:bg-gray-800' 
                  : 'bg-app-marineblue text-white hover:bg-app-black/90'
            }`}
          >
            {isConfirming ? 'Procesando...' : textoConfirmar}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ModalConfirmacion;