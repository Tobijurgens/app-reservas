import { useState, useEffect } from 'react';
import { usePerfil } from '../hooks/usePerfil';

const PerfilUsuario = () => {
  const { perfil, loading, isSubmitting, actualizarPerfil, eliminarCuenta } = usePerfil();
  
  const [formData, setFormData] = useState({
    nombre: '',
    apellido: '',
    telefono: ''
  });

  const [mensaje, setMensaje] = useState({ tipo: '', texto: '' });
  const [mostrarModalEliminar, setMostrarModalEliminar] = useState(false);

  // Cargamos los datos en el formulario cuando el perfil esté listo
  useEffect(() => {
    if (perfil) {
      setFormData({
        nombre: perfil.nombre || '',
        apellido: perfil.apellido || '',
        telefono: perfil.telefono || ''
      });
    }
  }, [perfil]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setMensaje({ tipo: '', texto: '' }); // Limpiamos mensajes al escribir
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const res = await actualizarPerfil(formData);
    
    if (res.success) {
      setMensaje({ tipo: 'exito', texto: '¡Perfil actualizado correctamente!' });
      // Borramos el mensaje de éxito a los 3 segundos
      setTimeout(() => setMensaje({ tipo: '', texto: '' }), 3000);
    } else {
      setMensaje({ tipo: 'error', texto: res.message });
    }
  };

  if (loading) {
    return <div className="flex justify-center py-20"><div className="animate-spin rounded-full h-10 w-10 border-b-2 border-blue-600"></div></div>;
  }

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-fadeIn">
      <header className="mb-8">
        <h1 className="text-2xl sm:text-3xl font-bold text-gray-900">Mi Perfil</h1>
        <p className="text-sm text-gray-600 mt-1">Gestioná tu información personal y configuración de cuenta.</p>
      </header>

      {/* TARJETA DE DATOS PERSONALES */}
      <div className="bg-white rounded-xl shadow-sm border border-gray-100 p-6 mb-8">
        <h2 className="text-lg font-bold text-gray-800 mb-5 border-b border-gray-100 pb-3">Datos Personales</h2>
        
        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-1">Nombre</label>
              <input 
                type="text" 
                name="nombre"
                required
                value={formData.nombre} 
                onChange={handleChange} 
                className="w-full px-4 py-2 bg-gray-50 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors" 
              />
            </div>
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-1">Apellido</label>
              <input 
                type="text" 
                name="apellido"
                required
                value={formData.apellido} 
                onChange={handleChange} 
                className="w-full px-4 py-2 bg-gray-50 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors" 
              />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            <div>
              <label className="block text-sm font-bold text-gray-700 mb-1">Teléfono</label>
              <input 
                type="tel" 
                name="telefono"
                value={formData.telefono} 
                onChange={handleChange} 
                placeholder="Ej: +54 9 11 1234-5678"
                className="w-full px-4 py-2 bg-gray-50 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:bg-white transition-colors" 
              />
            </div>
            <div>
              {/* El email es de solo lectura porque es el identificador del login */}
              <label className="block text-sm font-bold text-gray-700 mb-1">Email (No editable)</label>
              <input 
                type="email" 
                disabled
                value={perfil?.email || ''} 
                className="w-full px-4 py-2 bg-gray-100 border border-gray-200 text-gray-500 rounded-lg cursor-not-allowed" 
              />
            </div>
          </div>

          {mensaje.texto && (
            <div className={`p-3 rounded-lg text-sm font-medium ${mensaje.tipo === 'exito' ? 'bg-green-50 text-green-700 border border-green-200' : 'bg-red-50 text-red-700 border border-red-200'}`}>
              {mensaje.texto}
            </div>
          )}

          <div className="pt-2 flex justify-end">
            <button 
              type="submit" 
              disabled={isSubmitting}
              className={`px-6 py-2 rounded-lg font-medium transition-colors ${
                isSubmitting ? 'bg-blue-400 text-white cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700 text-white'
              }`}
            >
              {isSubmitting ? 'Guardando...' : 'Guardar Cambios'}
            </button>
          </div>
        </form>
      </div>

      {/* ZONA PELIGROSA */}
      <div className="bg-white rounded-xl shadow-sm border border-red-100 p-6">
        <h2 className="text-lg font-bold text-red-600 mb-2">Zona Peligrosa</h2>
        <p className="text-sm text-gray-600 mb-5">
          Eliminar tu cuenta es una acción irreversible. Se darán de baja todos tus negocios, se eliminarán tus servicios y se cancelarán tus turnos pendientes.
        </p>
        <button 
          type="button"
          onClick={() => setMostrarModalEliminar(true)}
          className="px-4 py-2 bg-red-50 text-red-600 border border-red-200 hover:bg-red-600 hover:text-white rounded-lg text-sm font-bold transition-colors"
        >
          Eliminar mi cuenta definitivamente
        </button>
      </div>

      {/* MODAL CONFIRMACIÓN ELIMINAR CUENTA */}
      {mostrarModalEliminar && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50 animate-fadeIn">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-2xl">
            <div className="flex items-center justify-center w-12 h-12 rounded-full bg-red-100 mx-auto mb-4">
              <span className="text-2xl">⚠️</span>
            </div>

            <h3 className="text-lg font-bold text-gray-900 text-center mb-2">¿Estás completamente seguro?</h3>
            <p className="text-sm text-gray-500 text-center mb-6">
              Ingresá a esta acción bajo tu propio riesgo. <strong>Esta acción no se puede deshacer</strong> y perderás acceso a toda tu información de inmediato.
            </p>

            <div className="flex flex-col sm:flex-row gap-3">
              <button 
                onClick={() => setMostrarModalEliminar(false)}
                disabled={isSubmitting}
                className="flex-1 py-2 text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg font-medium transition-colors"
              >
                Cancelar
              </button>
              <button 
                onClick={eliminarCuenta}
                disabled={isSubmitting}
                className={`flex-1 py-2 rounded-lg font-medium transition-colors ${
                  isSubmitting ? 'bg-red-400 text-white cursor-not-allowed' : 'bg-red-600 text-white hover:bg-red-700'
                }`}
              >
                {isSubmitting ? 'Eliminando...' : 'Sí, eliminar mi cuenta'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default PerfilUsuario;