import { useState, useEffect } from 'react';
import HorariosSemana from './HorariosSemana'; 
import { ordenarHorariosPorDia } from '../../../utils/fechas'; 

const NegocioForm = ({ initialData, onSubmit, onCancel, isSubmitting }) => {
  const [formData, setFormData] = useState({
    nombre: '', alias: '', descripcion: '', direccion: '', telefono: '', emailContacto: '', horarios: []
  });

  useEffect(() => {
    if (initialData) {
      setFormData({
        nombre: initialData.nombre || '',
        alias: initialData.alias || '',
        descripcion: initialData.descripcion || '',
        direccion: initialData.direccion || '',
        telefono: initialData.telefono || '',
        emailContacto: initialData.emailContacto || '',
        horarios: ordenarHorariosPorDia(initialData.horarios)
      });
    }
  }, [initialData]);

  const handleChange = (e) => {
    let { id, value } = e.target;
    if (id === 'alias') {
      value = value.toLowerCase().replace(/[^a-z0-9-]/g, '-').replace(/-+/g, '-');
    }
    setFormData({ ...formData, [id]: value });
  };

  const handleHorariosChange = (nuevosHorarios) => {
    setFormData({ ...formData, horarios: nuevosHorarios });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const payload = {
      ...formData,
      horarios: formData.horarios.map(h => ({
        ...h,
        horaInicioDescanso: h.horaInicioDescanso || null,
        horaFinDescanso: h.horaFinDescanso || null
      }))
    };
    onSubmit(payload);
  };

  return (
    <section className="max-w-4xl mx-auto bg-white p-8 rounded-2xl shadow-sm border border-gray-100">
      <header className="flex items-center justify-between mb-8">
        <h2 className="text-2xl font-bold text-app-black">{initialData?.id ? 'Editar negocio' : 'Crear nuevo negocio'}</h2>
        <button type="button" onClick={onCancel} className="text-app-marineblue hover:text-app-black text-sm font-medium transition-colors">Volver atrás</button>
      </header>

      <form onSubmit={handleSubmit} className="space-y-5">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          <div>
            <label htmlFor="nombre" className="block text-app-marineblue text-sm font-bold mb-2">Nombre del Negocio</label>
            <input id="nombre" type="text" className="w-full px-4 py-3 bg-gray-50 border-none rounded-xl focus:ring-2 focus:ring-app-marineblue text-app-black transition-shadow" value={formData.nombre} onChange={handleChange} required />
          </div>
          <div>
            <label htmlFor="alias" className="block text-app-marineblue text-sm font-bold mb-2">Alias (URL)</label>
            <input id="alias" type="text" className="w-full px-4 py-3 bg-gray-100 border-none rounded-xl focus:ring-2 focus:ring-app-marineblue text-gray-500 transition-shadow" value={formData.alias} onChange={handleChange} required />
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
          <div>
            <label htmlFor="direccion" className="block text-app-marineblue text-sm font-bold mb-2">Dirección</label>
            <input id="direccion" type="text" className="w-full px-4 py-3 bg-gray-50 border-none rounded-xl focus:ring-2 focus:ring-app-marineblue text-app-black transition-shadow" value={formData.direccion} onChange={handleChange} required />
          </div>
          <div>
            <label htmlFor="telefono" className="block text-app-marineblue text-sm font-bold mb-2">Teléfono</label>
            <input id="telefono" type="tel" className="w-full px-4 py-3 bg-gray-50 border-none rounded-xl focus:ring-2 focus:ring-app-marineblue text-app-black transition-shadow" value={formData.telefono} onChange={handleChange} required />
          </div>
        </div>

        <div>
          <label htmlFor="emailContacto" className="block text-app-marineblue text-sm font-bold mb-2">Email de Contacto (Público)</label>
          <input 
            id="emailContacto" 
            type="email" 
            className="w-full px-4 py-3 bg-gray-50 border-none rounded-xl focus:ring-2 focus:ring-app-marineblue text-app-black transition-shadow" 
            value={formData.emailContacto} 
            onChange={handleChange} 
            placeholder="Ej: info@milocal.com (Opcional)" 
          />
        </div>

        <div>
          <label htmlFor="descripcion" className="block text-app-marineblue text-sm font-bold mb-2">Descripción</label>
          <textarea id="descripcion" rows="3" className="w-full px-4 py-3 bg-gray-50 border-none rounded-xl focus:ring-2 focus:ring-app-marineblue text-app-black transition-shadow resize-none" value={formData.descripcion} onChange={handleChange} />
        </div>

        <HorariosSemana horarios={formData.horarios} setHorarios={handleHorariosChange} />

        <footer className="flex justify-end pt-6 border-t border-gray-100">
          <button type="submit" disabled={isSubmitting} className={`px-8 py-3 rounded-xl text-white font-bold transition-all shadow-sm ${isSubmitting ? 'bg-gray-300 cursor-not-allowed' : 'bg-app-marineblue hover:bg-app-black/90 transition-colors duration-300'}`}>
            {isSubmitting ? 'Guardando...' : 'Guardar Negocio'}
          </button>
        </footer>
      </form>
    </section>
  );
};

export default NegocioForm;