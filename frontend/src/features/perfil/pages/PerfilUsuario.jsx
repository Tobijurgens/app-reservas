import { useState } from 'react';
import { usePerfil } from '../hooks/usePerfil';
import { useToast } from '../../../context/ToastContext';
import ModalConfirmacion from '../../../components/ModalConfirmacion';

const PerfilUsuario = () => {
  const datos = usePerfil();
  if (datos.loading) return <div className="flex justify-center py-20"><div className="animate-spin rounded-full h-10 w-10 border-b-2 border-app-marineblue"></div></div>;
  if (datos.error || !datos.perfil) return (
    <div role="alert" className="mx-auto max-w-3xl px-4 py-12 text-center">
      <p>{datos.error || 'No pudimos cargar tu perfil.'}</p>
      <button onClick={datos.reintentar} className="mt-4 font-semibold text-app-marineblue hover:underline">Reintentar</button>
    </div>
  );
  return <DatosPerfil key={datos.perfil.id} {...datos} />;
};

const DatosPerfil = ({ perfil, isSubmitting, actualizarPerfil, eliminarCuenta }) => {
  const { addToast } = useToast();
  const [formData, setFormData] = useState(() => ({
    nombre: perfil.nombre || '', apellido: perfil.apellido || '', telefono: perfil.telefono || '',
  }));
  const [mostrarModalEliminar, setMostrarModalEliminar] = useState(false);

  const handleChange = (e) => setFormData({ ...formData, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    const res = await actualizarPerfil(formData);
    
    if (res.success) {
      setFormData({ nombre: res.perfil.nombre || '', apellido: res.perfil.apellido || '', telefono: res.perfil.telefono || '' });
      addToast('¡Perfil actualizado correctamente!', 'success');
    }
    else addToast(res.message, 'error');
  };


  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 lg:px-8 py-8 animate-fadeIn">
      <header className="mb-8">
        <h1 className="text-2xl sm:text-3xl font-bold text-app-black">Mi Perfil</h1>
        <p className="text-sm text-app-marineblue mt-1">Gestioná tu información personal y configuración de cuenta.</p>
      </header>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 sm:p-8 mb-8">
        <div className="flex justify-between items-center border-b border-gray-100 pb-4 mb-6">
          <h2 className="text-lg font-bold text-app-black">Datos Personales</h2>
          {perfil?.estadoCuenta && (
            <span className="px-3 py-1 bg-app-white text-app-marineblue text-xs font-bold rounded-full border border-app-marineblue/20">
              {perfil.estadoCuenta}
            </span>
          )}
        </div>
        
        <form onSubmit={handleSubmit} className="space-y-5">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            <div>
              <label className="block text-sm font-bold text-app-marineblue mb-1">Nombre</label>
              <input type="text" name="nombre" required value={formData.nombre} onChange={handleChange} className="w-full px-4 py-3 bg-gray-50 border-none rounded-xl focus:ring-2 focus:ring-app-marineblue focus:bg-white transition-colors text-app-black" />
            </div>
            <div>
              <label className="block text-sm font-bold text-app-marineblue mb-1">Apellido</label>
              <input type="text" name="apellido" required value={formData.apellido} onChange={handleChange} className="w-full px-4 py-3 bg-gray-50 border-none rounded-xl focus:ring-2 focus:ring-app-marineblue focus:bg-white transition-colors text-app-black" />
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            <div>
              <label className="block text-sm font-bold text-app-marineblue mb-1">Teléfono</label>
              <input type="tel" name="telefono" value={formData.telefono} onChange={handleChange} placeholder="Ej: +54 9 11 1234-5678" className="w-full px-4 py-3 bg-gray-50 border-none rounded-xl focus:ring-2 focus:ring-app-marineblue focus:bg-white transition-colors text-app-black" />
            </div>
            <div>
              <label className="block text-sm font-bold text-app-marineblue mb-1">Email (No editable)</label>
              <input type="email" disabled value={perfil?.email || ''} className="w-full px-4 py-3 bg-gray-100 border-none text-gray-400 rounded-xl cursor-not-allowed" />
            </div>
          </div>

          <div className="pt-4 flex justify-end">
            <button type="submit" disabled={isSubmitting} className={`px-6 py-3 rounded-xl font-medium transition-colors shadow-sm ${isSubmitting ? 'bg-gray-300 text-gray-500 cursor-not-allowed' : 'bg-app-marineblue hover:bg-app-black/90 text-white transition-colors duration-300'}`}>
              {isSubmitting ? 'Guardando...' : 'Guardar Cambios'}
            </button>
          </div>
        </form>
      </div>

      <div className="bg-white rounded-2xl shadow-sm border border-gray-200 p-6 sm:p-8">
        <h2 className="text-lg font-bold text-app-black mb-2">Zona Peligrosa</h2>
        <p className="text-sm text-app-red mb-6">Eliminar tu cuenta es una acción irreversible. Se darán de baja todos tus negocios, se eliminarán tus servicios y se cancelarán tus turnos pendientes.</p>
        <button type="button" onClick={() => setMostrarModalEliminar(true)} className="px-5 py-3 bg-white text-app-black border border-app-black hover:bg-app-red hover:border-app-red hover:text-white rounded-xl text-sm font-bold transition-colors shadow-sm duration-300">
          Eliminar mi cuenta definitivamente
        </button>
      </div>

      <ModalConfirmacion 
        isOpen={mostrarModalEliminar}
        titulo="¿Estás completamente seguro?"
        mensaje={<>Ingresá a esta acción bajo tu propio riesgo. <strong>Esta acción no se puede deshacer</strong> y perderás acceso a toda tu información de inmediato.</>}
        textoConfirmar="Sí, eliminar"
        esPeligroso={true}
        isConfirming={isSubmitting}
        onClose={() => setMostrarModalEliminar(false)}
        onConfirm={eliminarCuenta}
      />
    </div>
  );
};

export default PerfilUsuario;
