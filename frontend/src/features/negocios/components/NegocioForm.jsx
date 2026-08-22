import { useState, useEffect } from 'react';
import HorariosSemana from './HorariosSemana'; // IMPORTAMOS EL COMPONENTE

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
        emailContacto: initialData.emailContacto || '', // <-- AHORA CARGA EL EMAIL SI EXISTE
        horarios: initialData.horarios || []
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

    // Limpiamos strings vacíos a null para el backend
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
    <section className="max-w-4xl mx-auto bg-white p-6 rounded-xl shadow-sm border border-gray-100">
      <header className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold text-gray-900">{initialData?.id ? 'Editar negocio' : 'Crear nuevo negocio'}</h2>
        <button type="button" onClick={onCancel} className="text-gray-500 hover:text-gray-700 text-sm font-medium">Volver atrás</button>
      </header>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label htmlFor="nombre" className="block text-gray-700 text-sm font-bold mb-2">Nombre del Negocio</label>
            <input id="nombre" type="text" className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500" value={formData.nombre} onChange={handleChange} required />
          </div>
          <div>
            <label htmlFor="alias" className="block text-gray-700 text-sm font-bold mb-2">Alias (URL)</label>
            <input id="alias" type="text" className="w-full px-4 py-2 border rounded-lg bg-gray-50 focus:ring-2 focus:ring-blue-500" value={formData.alias} onChange={handleChange} required />
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label htmlFor="direccion" className="block text-gray-700 text-sm font-bold mb-2">Dirección</label>
            <input id="direccion" type="text" className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500" value={formData.direccion} onChange={handleChange} required />
          </div>
          <div>
            <label htmlFor="telefono" className="block text-gray-700 text-sm font-bold mb-2">Teléfono</label>
            <input id="telefono" type="tel" className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500" value={formData.telefono} onChange={handleChange} required />
          </div>
        </div>

        {/* NUEVO CAMPO: EMAIL DE CONTACTO */}
        <div>
          <label htmlFor="emailContacto" className="block text-gray-700 text-sm font-bold mb-2">Email de Contacto (Público)</label>
          <input 
            id="emailContacto" 
            type="email" 
            className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500" 
            value={formData.emailContacto} 
            onChange={handleChange} 
            placeholder="Ej: info@milocal.com (Opcional)" 
          />
        </div>

        <div>
          <label htmlFor="descripcion" className="block text-gray-700 text-sm font-bold mb-2">Descripción</label>
          <textarea id="descripcion" rows="3" className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-blue-500" value={formData.descripcion} onChange={handleChange} />
        </div>

        {/* INYECTAMOS LA GRILLA DE HORARIOS */}
        <HorariosSemana horarios={formData.horarios} setHorarios={handleHorariosChange} />

        <footer className="flex justify-end pt-4">
          <button type="submit" disabled={isSubmitting} className={`px-6 py-2 rounded-lg text-white font-bold transition-colors ${isSubmitting ? 'bg-blue-400' : 'bg-blue-600 hover:bg-blue-700'}`}>
            {isSubmitting ? 'Guardando...' : 'Guardar Negocio'}
          </button>
        </footer>
      </form>
    </section>
  );
};

export default NegocioForm;