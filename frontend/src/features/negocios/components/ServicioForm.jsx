import { useState, useEffect } from 'react';

const ServicioForm = ({ initialData, recursoNombre, onSubmit, onCancel }) => {
  const [formData, setFormData] = useState({ nombre: '', descripcion: '', precio: '', duracionMinutos: '' });

  useEffect(() => {
    if (initialData) setFormData(initialData);
  }, [initialData]);

  const handleSubmit = (e) => {
    e.preventDefault();
    onSubmit(formData);
  };

  return (
    <form onSubmit={handleSubmit} className="p-6 bg-app-white border-b border-gray-100">
      <h4 className="text-base font-bold text-app-black mb-4">
        {initialData ? `Editar servicio en ${recursoNombre}` : `Nuevo servicio para ${recursoNombre}`}
      </h4>
      
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-4">
        <div className="sm:col-span-1">
          <label className="block text-xs font-bold text-app-marineblue uppercase tracking-wider mb-1.5">Nombre</label>
          <input type="text" required value={formData.nombre} onChange={e => setFormData({...formData, nombre: e.target.value})} className="w-full px-4 py-2 text-sm bg-white border-none rounded-xl shadow-sm focus:ring-2 focus:ring-app-marineblue text-app-black transition-shadow" />
        </div>
        <div>
          <label className="block text-xs font-bold text-app-marineblue uppercase tracking-wider mb-1.5">Precio ($) (Opcional)</label>
          <input type="number" min="0" required={false} value={formData.precio} onChange={e => setFormData({...formData, precio: e.target.value})} className="w-full px-4 py-2 text-sm bg-white border-none rounded-xl shadow-sm focus:ring-2 focus:ring-app-marineblue text-app-black transition-shadow" />
        </div>
        <div>
          <label className="block text-xs font-bold text-app-marineblue uppercase tracking-wider mb-1.5">Duración (Min)</label>
          <input type="number" min="5" required value={formData.duracionMinutos} onChange={e => setFormData({...formData, duracionMinutos: e.target.value})} className="w-full px-4 py-2 text-sm bg-white border-none rounded-xl shadow-sm focus:ring-2 focus:ring-app-marineblue text-app-black transition-shadow" />
        </div>
      </div>

      <div className="mb-6">
        <label className="block text-xs font-bold text-app-marineblue uppercase tracking-wider mb-1.5">Descripción (Opcional)</label>
        <input type="text" value={formData.descripcion} onChange={e => setFormData({...formData, descripcion: e.target.value})} className="w-full px-4 py-2 text-sm bg-white border-none rounded-xl shadow-sm focus:ring-2 focus:ring-app-marineblue text-app-black transition-shadow" />
      </div>

      <footer className="flex justify-end gap-4 mt-2">
        <button type="submit" className="bg-app-marineblue text-white px-6 py-2 rounded-xl text-xs font-bold uppercase tracking-wider hover:bg-app-black/90 transition-colors shadow-sm duration-300">
          {initialData ? 'Actualizar' : 'Guardar'}
        </button>
        <button type="button" onClick={onCancel} className="text-app-black px-6 py-2 rounded-xl text-xs font-bold uppercase tracking-wider hover:text-app-red hover:bg-app-black/5 transition-colors shadow-sm duration-300">Cancelar</button>
      </footer>
    </form>
  );
};

export default ServicioForm;