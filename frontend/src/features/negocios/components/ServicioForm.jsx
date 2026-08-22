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
    <form onSubmit={handleSubmit} className="p-5 bg-blue-50 border-b border-blue-100">
      <h4 className="text-sm font-bold text-blue-800 mb-3">
        {initialData ? `Editar servicio en ${recursoNombre}` : `Nuevo servicio para ${recursoNombre}`}
      </h4>
      
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-3">
        <div className="sm:col-span-1">
          <label className="block text-xs font-bold text-gray-700 mb-1">Nombre</label>
          <input type="text" required value={formData.nombre} onChange={e => setFormData({...formData, nombre: e.target.value})} className="w-full px-3 py-1.5 text-sm border rounded-md focus:ring-2 focus:ring-blue-500" />
        </div>
        <div>
          <label className="block text-xs font-bold text-gray-700 mb-1">Precio ($)</label>
          <input type="number" required value={formData.precio} onChange={e => setFormData({...formData, precio: e.target.value})} className="w-full px-3 py-1.5 text-sm border rounded-md focus:ring-2 focus:ring-blue-500" />
        </div>
        <div>
          <label className="block text-xs font-bold text-gray-700 mb-1">Duración (Min)</label>
          <input type="number" required value={formData.duracionMinutos} onChange={e => setFormData({...formData, duracionMinutos: e.target.value})} className="w-full px-3 py-1.5 text-sm border rounded-md focus:ring-2 focus:ring-blue-500" />
        </div>
      </div>

      <div className="mb-3">
        <label className="block text-xs font-bold text-gray-700 mb-1">Descripción (Opcional)</label>
        <input type="text" value={formData.descripcion} onChange={e => setFormData({...formData, descripcion: e.target.value})} className="w-full px-3 py-1.5 text-sm border rounded-md focus:ring-2 focus:ring-blue-500" />
      </div>

      <footer className="flex justify-end gap-3 mt-3">
        <button type="button" onClick={onCancel} className="text-gray-500 text-xs font-medium hover:text-gray-700">Cancelar</button>
        <button type="submit" className="bg-blue-600 text-white px-3 py-1.5 rounded-md text-xs font-medium hover:bg-blue-700">
          {initialData ? 'Actualizar' : 'Guardar'}
        </button>
      </footer>
    </form>
  );
};

export default ServicioForm;