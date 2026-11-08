import { useState, useRef } from 'react';
import HorariosSemana from './HorariosSemana'; 
import { horariosIniciales, ordenarHorariosPorDia } from '../../../utils/fechas';
import { validarHorariosRecurso } from '../utils/horarios';
import { useToast } from '../../../context/ToastContext';
import { mensajeError } from '../../../utils/errores';
import { Copy } from 'lucide-react';

const RecursoForm = ({ initialData, negocioHorarios, onSubmit, onCancel, isOwner = true }) => {
  const [formData, setFormData] = useState(() => {
    if (initialData) {
      return {
        idNegocio: initialData.negocioId || initialData.idNegocio || null, 
        nombre: initialData.nombre || '',
        descripcion: initialData.descripcion || '',
        emailUsuario: initialData.emailUsuario || '',
        horarios: horariosIniciales(initialData.horarios)
      };
    }
    
    return {
      idNegocio: null,
      nombre: '', descripcion: '', emailUsuario: '',
      horarios: horariosIniciales(negocioHorarios),
    };
  });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const enviando = useRef(false);
  const { addToast } = useToast();
  const erroresHorario = validarHorariosRecurso(formData.horarios, negocioHorarios);

  const handleCopiarHorariosNegocio = () => {
    if (!negocioHorarios) return;
    setFormData(prev => ({
      ...prev,
      horarios: ordenarHorariosPorDia(negocioHorarios.map(h => ({ ...h })))
    }));
  };

  const handleHorariosChange = (nuevosHorarios) => {
    setFormData({ ...formData, horarios: nuevosHorarios });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (enviando.current || erroresHorario.length > 0) return;
    enviando.current = true;
    setIsSubmitting(true);

    const payload = {
      ...formData,
      idNegocio: formData.idNegocio,
      horarios: formData.horarios.map(h => ({
        ...h,
        horaInicioDescanso: h.horaInicioDescanso || null,
        horaFinDescanso: h.horaFinDescanso || null
      }))
    };
    
    try {
      await onSubmit(payload);
    } catch (error) {
      addToast(mensajeError(error, 'Error al guardar el recurso.'), 'error');
    } finally {
      enviando.current = false;
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="bg-white p-8 rounded-2xl shadow-sm border border-gray-100 mb-8 max-w-4xl animate-fadeIn">
      <header className="mb-6">
        <h3 className="text-xl font-bold text-app-black">{initialData ? 'Editar Recurso' : 'Agregar Nuevo Recurso'}</h3>
        {initialData && (
          <p className="text-xs text-app-black mt-3 p-3 bg-app-yellow/50 rounded-xl">
            <strong>⚠️ Buenas prácticas:</strong> Evitá cambiar el nombre por un recurso totalmente distinto para no arruinar el historial de turnos.
          </p>
        )}
      </header>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-5 mb-5">
        <div>
          <label className="block text-sm font-bold text-app-marineblue mb-2">Nombre</label>
          <input 
            type="text" 
            required 
            disabled={!isOwner}
            value={formData.nombre} 
            onChange={e => setFormData({...formData, nombre: e.target.value})} 
            className={`w-full px-4 py-3 border-none rounded-xl focus:ring-2 focus:ring-app-marineblue transition-shadow ${!isOwner ? 'bg-gray-100 text-gray-500 cursor-not-allowed' : 'bg-gray-50 text-app-black'}`} 
            placeholder="Ej: Peluquero Juan" 
          />
        </div>
        <div>
          <label className="block text-sm font-bold text-app-marineblue mb-2">Email del usuario (Opcional)</label>
          <input 
            type="email" 
            disabled={!isOwner}
            value={formData.emailUsuario} 
            onChange={e => setFormData({...formData, emailUsuario: e.target.value})} 
            className={`w-full px-4 py-3 border-none rounded-xl focus:ring-2 focus:ring-app-marineblue transition-shadow ${!isOwner ? 'bg-gray-100 text-gray-500 cursor-not-allowed' : 'bg-gray-50 text-app-black'}`} 
            placeholder="Si es empleado, su email" 
          />
        </div>
      </div>

      <div className="mb-8">
        <label className="block text-sm font-bold text-app-marineblue mb-2">Descripción</label>
        <input type="text" value={formData.descripcion} onChange={e => setFormData({...formData, descripcion: e.target.value})} className="w-full px-4 py-3 bg-gray-50 border-none rounded-xl focus:ring-2 focus:ring-app-marineblue text-app-black transition-shadow" />
      </div>

      <div className="flex justify-between items-center mb-2">
        <h3 className="font-bold text-app-black text-sm">Configuración de Horarios</h3>
        {isOwner && (
          <button 
            type="button" 
            onClick={handleCopiarHorariosNegocio}
            className="text-xs font-bold text-app-marineblue bg-app-ligthblue/30 px-3 py-1.5 rounded-lg hover:bg-app-ligthblue/60 transition-colors flex items-center gap-1"
          >
            <Copy className="w-3 h-3" /> Copiar horarios del negocio
          </button>
        )}
      </div>

      <HorariosSemana horarios={formData.horarios} setHorarios={handleHorariosChange} />
      
      {erroresHorario.length > 0 && (
        <div className="mt-6 p-4 bg-app-ligthblue/30 rounded-xl border border-app-ligthblue animate-fadeIn">
          <p className="text-sm font-bold text-app-black mb-2">Corregí los siguientes conflictos de horario:</p>
          <ul className="list-disc list-inside text-sm text-app-black/80 space-y-1">
            {erroresHorario.map((err, idx) => (
              <li key={idx}>{err}</li>
            ))}
          </ul>
        </div>
      )}

      <footer className="flex justify-end gap-4 mt-8 border-t border-gray-100 pt-6">
        <button type="button" disabled={isSubmitting} onClick={onCancel} className="text-app-marineblue text-sm font-bold uppercase tracking-wider hover:text-app-black transition-colors">Cancelar</button>
        <button 
          type="submit" 
          disabled={isSubmitting || erroresHorario.length > 0}
          className={`px-8 py-3 rounded-xl text-sm font-bold transition-all shadow-sm ${
            erroresHorario.length > 0 
              ? 'bg-gray-200 text-gray-400 cursor-not-allowed' 
              : 'bg-app-marineblue text-white hover:bg-app-black/90'
          }`}
        >
          {isSubmitting ? 'Guardando...' : initialData ? 'Actualizar' : 'Guardar'}
        </button>
      </footer>
    </form>
  );
};

export default RecursoForm;
