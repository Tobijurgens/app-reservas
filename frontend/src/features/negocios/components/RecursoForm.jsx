import { useState, useEffect } from 'react';
import HorariosSemana from './HorariosSemana'; 

const DIAS_ES = {
  MONDAY: 'Lunes', TUESDAY: 'Martes', WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves', FRIDAY: 'Viernes', SATURDAY: 'Sábado', SUNDAY: 'Domingo'
};

const RecursoForm = ({ initialData, negocioHorarios, onSubmit, onCancel }) => {
  const [formData, setFormData] = useState(() => {
    if (initialData) {
      return {
        // LA SOLUCIÓN CLAVE: Atrapamos negocioId o idNegocio
        idNegocio: initialData.negocioId || initialData.idNegocio || null, 
        nombre: initialData.nombre || '',
        descripcion: initialData.descripcion || '',
        emailUsuario: initialData.emailUsuario || '',
        horarios: initialData.horarios || []
      };
    }
    return {
      idNegocio: null,
      nombre: '', descripcion: '', emailUsuario: '',
      horarios: negocioHorarios ? negocioHorarios.map(h => ({ ...h })) : []
    };
  });

  const [erroresHorario, setErroresHorario] = useState([]);

  useEffect(() => {
    if (!negocioHorarios || negocioHorarios.length === 0) return;

    const errores = [];
    const normalizarHora = (horaStr) => horaStr ? horaStr.substring(0, 5) : null;

    formData.horarios.forEach(hRecurso => {
      if (!hRecurso.activo) return; 

      const hNegocio = negocioHorarios.find(hn => hn.diaSemana === hRecurso.diaSemana);
      
      if (!hNegocio || !hNegocio.activo) {
        errores.push(`El local está cerrado los ${DIAS_ES[hRecurso.diaSemana]}.`);
        return; 
      }

      const recInicio = normalizarHora(hRecurso.horaInicio);
      const negInicio = normalizarHora(hNegocio.horaInicio);
      const recFin = normalizarHora(hRecurso.horaFin);
      const negFin = normalizarHora(hNegocio.horaFin);

      if (recInicio && negInicio && recInicio < negInicio) {
        errores.push(`El ${DIAS_ES[hRecurso.diaSemana]} inicia antes de la apertura del local (${negInicio}).`);
      }
      
      if (recFin && negFin && recFin > negFin) {
        errores.push(`El ${DIAS_ES[hRecurso.diaSemana]} termina después del cierre del local (${negFin}).`);
      }
    });

    setErroresHorario(errores);
  }, [formData.horarios, negocioHorarios]);

  const handleHorariosChange = (nuevosHorarios) => {
    setFormData({ ...formData, horarios: nuevosHorarios });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (erroresHorario.length > 0) return;

    const payload = {
      ...formData,
      // Si el id viene, lo enviamos; si no, dejamos que el backend falle explícitamente.
      idNegocio: formData.idNegocio,
      horarios: formData.horarios.map(h => ({
        ...h,
        horaInicioDescanso: h.horaInicioDescanso || null,
        horaFinDescanso: h.horaFinDescanso || null
      }))
    };
    
    onSubmit(payload);
  };

  return (
    <form onSubmit={handleSubmit} className="bg-white p-5 rounded-xl shadow-sm border border-blue-200 mb-6 max-w-4xl animate-fadeIn">
      <header className="mb-4">
        <h3 className="font-bold text-blue-800">{initialData ? 'Editar Recurso' : 'Agregar Nuevo Recurso'}</h3>
        {initialData && (
          <p className="text-xs text-yellow-800 mt-2 p-2 bg-yellow-50 rounded-md border-l-4 border-yellow-400">
            <strong>⚠️ Buenas prácticas:</strong> Evitá cambiar el nombre por un recurso totalmente distinto para no arruinar el historial.
          </p>
        )}
      </header>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
        <div>
          <label className="block text-xs font-bold text-gray-700 mb-1">Nombre</label>
          <input type="text" required value={formData.nombre} onChange={e => setFormData({...formData, nombre: e.target.value})} className="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500" placeholder="Ej: Peluquero Juan" />
        </div>
        <div>
          <label className="block text-xs font-bold text-gray-700 mb-1">Email del usuario (Opcional)</label>
          <input type="email" value={formData.emailUsuario} onChange={e => setFormData({...formData, emailUsuario: e.target.value})} className="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500" placeholder="Si es empleado, su email" />
        </div>
      </div>

      <div className="mb-4">
        <label className="block text-xs font-bold text-gray-700 mb-1">Descripción</label>
        <input type="text" value={formData.descripcion} onChange={e => setFormData({...formData, descripcion: e.target.value})} className="w-full px-3 py-2 border rounded-md focus:ring-2 focus:ring-blue-500" />
      </div>

      <HorariosSemana horarios={formData.horarios} setHorarios={handleHorariosChange} />
      
      {erroresHorario.length > 0 && (
        <div className="mt-4 p-3 bg-red-50 rounded-lg border border-red-200 animate-fadeIn">
          <p className="text-sm font-bold text-red-800 mb-1">Corregí los siguientes conflictos de horario:</p>
          <ul className="list-disc list-inside text-xs text-red-600">
            {erroresHorario.map((err, idx) => (
              <li key={idx}>{err}</li>
            ))}
          </ul>
        </div>
      )}

      <footer className="flex justify-end gap-3 mt-4">
        <button type="button" onClick={onCancel} className="text-gray-500 text-sm font-medium hover:text-gray-700">Cancelar</button>
        <button 
          type="submit" 
          disabled={erroresHorario.length > 0}
          className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
            erroresHorario.length > 0 
              ? 'bg-gray-300 text-gray-500 cursor-not-allowed' 
              : 'bg-blue-600 text-white hover:bg-blue-700'
          }`}
        >
          {initialData ? 'Actualizar' : 'Guardar'}
        </button>
      </footer>
    </form>
  );
};

export default RecursoForm;