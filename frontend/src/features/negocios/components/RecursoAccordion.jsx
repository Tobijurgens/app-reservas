import { useState } from 'react';
import ServicioForm from './ServicioForm';

const RecursoAccordion = ({ recurso, isExpanded, onToggle, onEditRecurso, onDeleteRecurso, onSaveServicio, onDeleteServicio }) => {
  const [isAddingServicio, setIsAddingServicio] = useState(false);
  const [editingServicio, setEditingServicio] = useState(null);

  const handleOpenServicioForm = (servicio = null) => {
    setEditingServicio(servicio);
    setIsAddingServicio(true);
  };

  const handleCloseServicioForm = () => {
    setEditingServicio(null);
    setIsAddingServicio(false);
  };

  const handleServicioSubmit = async (formData) => {
    await onSaveServicio(formData, editingServicio?.id, recurso.id);
    handleCloseServicioForm();
  };

  const diasActivos = recurso.horarios?.filter(h => h.activo).length || 0;

  return (
    <article className="bg-white border border-gray-200 rounded-xl overflow-hidden transition-all">
      <header 
        onClick={onToggle}
        className="bg-gray-50 px-5 py-4 border-b border-gray-200 flex justify-between items-center cursor-pointer hover:bg-gray-100 transition-colors"
      >
        <div className="flex-1 pr-4">
          <div className="flex items-center gap-2">
            <h3 className="font-bold text-gray-900 text-lg">{recurso.nombre}</h3>
            {recurso.emailUsuario && (
              <span className="bg-blue-100 text-blue-700 text-xs px-2 py-0.5 rounded-full font-medium">Vinculado: {recurso.emailUsuario}</span>
            )}
          </div>
          <p className="text-sm text-gray-500">
            Trabaja {diasActivos} días a la semana - {recurso.descripcion || 'Sin descripción'}
          </p>
          
          <div className="flex flex-wrap gap-3 mt-2">
            <button onClick={(e) => { e.stopPropagation(); onEditRecurso(recurso); }} className="text-xs text-blue-600 hover:underline font-medium">Editar horario/info</button>
            <button onClick={(e) => { e.stopPropagation(); onDeleteRecurso(recurso.id, recurso.nombre); }} className="text-xs text-red-500 hover:underline font-medium">Eliminar recurso</button>
          </div>
        </div>
        
        <svg className={`w-6 h-6 text-gray-400 transform transition-transform duration-200 flex-shrink-0 ${isExpanded ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
        </svg>
      </header>

      {isExpanded && (
        <div className="animate-fadeIn">
          <div className="bg-white p-3 flex justify-end border-b border-gray-100">
            {!isAddingServicio && (
              <button onClick={() => handleOpenServicioForm()} className="text-sm bg-white border border-gray-300 text-gray-700 px-3 py-1.5 rounded-lg font-medium hover:bg-gray-100">
                + Agregar Servicio
              </button>
            )}
          </div>

          {isAddingServicio && (
            <ServicioForm 
              initialData={editingServicio} 
              recursoNombre={recurso.nombre}
              onSubmit={handleServicioSubmit} 
              onCancel={handleCloseServicioForm} 
            />
          )}

          <div className="px-5 py-2">
            {recurso.servicios?.length > 0 ? (
              <ul className="divide-y divide-gray-100">
                {recurso.servicios.map(servicio => (
                  <li key={servicio.id} className="py-3 flex justify-between items-center">
                    <div>
                      <span className="font-semibold text-gray-800 block">{servicio.nombre}</span>
                      <span className="text-xs text-gray-500">⏱ {servicio.duracionMinutos} min</span>
                    </div>
                    <div className="text-right">
                      <span className="font-extrabold text-green-600 block mb-1">${servicio.precio}</span>
                      <div className="flex gap-2 justify-end">
                        <button onClick={() => handleOpenServicioForm(servicio)} className="text-xs text-blue-600 hover:underline">Editar</button>
                        <button onClick={() => onDeleteServicio(servicio.id, servicio.nombre)} className="text-xs text-red-500 hover:underline">Eliminar</button>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-gray-400 py-3 italic">No hay servicios asignados a este recurso todavía.</p>
            )}
          </div>
        </div>
      )}
    </article>
  );
};

export default RecursoAccordion;