import React, { useState } from 'react';
import ServicioForm from './ServicioForm';
import { useToast } from '../../../context/ToastContext';
import { CircleDashedCheck, ClockFading, CircleX, Clock} from 'lucide-react';

const RecursoAccordion = ({ recurso, isExpanded, onToggle, onEditRecurso, onDeleteRecurso, onSaveServicio, onDeleteServicio }) => {
  const [isAddingServicio, setIsAddingServicio] = useState(false);
  const [editingServicio, setEditingServicio] = useState(null);
  const { addToast } = useToast();

  const handleOpenServicioForm = (servicio = null) => {
    setEditingServicio(servicio);
    setIsAddingServicio(true);
  };

  const handleCloseServicioForm = () => {
    setEditingServicio(null);
    setIsAddingServicio(false);
  };

  const handleServicioSubmit = async (formData) => {
    const resultado = await onSaveServicio(formData, editingServicio?.id, recurso.id);
    if (resultado?.success) handleCloseServicioForm();
    return resultado;
  };

  const renderEstadoAsociacion = () => {
    if (!recurso.emailUsuario) return null;

    // Usamos exactamente el campo estadoAsociacion que viene del backend
    const estado = recurso.estadoAsociacion?.toUpperCase() || 'ACEPTADA';

    if (estado === 'PENDIENTE') {
      return (
        <span className="bg-app-white text-app-black border border-app-ligthblue text-xs px-3 py-1 rounded-full font-bold flex items-center gap-1">
          <ClockFading className="w-5 h-5" /> Pendiente: {recurso.emailUsuario}
        </span>
      );
    }
    
    if (estado === 'RECHAZADO') {
      return (
        <span className="bg-app-ligthblue text-app-black border border-app-ligthblue text-xs px-3 py-1 rounded-full font-bold flex items-center gap-1">
          <CircleX className="w-5 h-5" /> Rechazó: {recurso.emailUsuario}
        </span>
      );
    }

    return (
      <span className="bg-app-ligthblue text-app-black border border-app-ligthblue text-xs px-3 py-1 rounded-full font-bold flex items-center gap-1">
        <CircleDashedCheck className="w-5 h-5" /> Vinculado: {recurso.emailUsuario}
      </span>
    );
  };

  const diasActivos = recurso.horarios?.filter(h => h.activo).length || 0;

  return (
    <article className="bg-white border border-gray-100 rounded-2xl overflow-hidden transition-all shadow-sm hover:shadow-md">
      <header 
        onClick={onToggle}
        className=" px-6 py-5 border-b border-gray-100 flex justify-between items-center cursor-pointer hover:bg-[#dbebd8] transition-colors"
      >
        <div className="flex-1 pr-4">
          <div className="flex flex-wrap items-center gap-3 mb-1">
            <h3 className="font-bold text-app-marineblue text-lg">{recurso.nombre}</h3>
            {renderEstadoAsociacion()}
          </div>
          <p className="text-sm ">
            Trabaja {diasActivos} días a la semana - {recurso.descripcion || 'Sin descripción'}
          </p>
          
          <div className="flex flex-wrap gap-4 mt-3">
            <button onClick={(e) => { e.stopPropagation(); onEditRecurso(recurso); }} className="text-xs  hover:text-app-marineblue font-bold uppercase tracking-wider transition-colors">Editar info</button>
            <button 
              onClick={(e) => { 
                e.stopPropagation(); 
                if (typeof onDeleteRecurso === 'function') {
                  onDeleteRecurso(recurso.id, recurso.nombre); 
                } else {
                  addToast('Acción denegada: Solo el dueño del negocio puede dar de baja este recurso.', 'error');
                }
              }} 
              className="text-xs text-gray-400 hover:text-app-red px-2 py-0.5 rounded transition-colors font-bold uppercase tracking-wider"
            >
              Eliminar recurso
            </button>
          </div>
        </div>
        
        <svg className={`w-6 h-6  transform transition-transform duration-200 shrink-0 ${isExpanded ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7" />
        </svg>
      </header>

      {isExpanded && (
        <div className="animate-fadeIn">
          
          {/* 1. LISTA DE SERVICIOS */}
          <div className="px-6 py-2">
            {recurso.servicios?.length > 0 ? (
              <ul className="divide-y divide-gray-100">
                {recurso.servicios.map(servicio => (
                  <React.Fragment key={servicio.id}>
                    
                    {/* CONDICIONAL: Si este servicio es el que estamos editando, mostramos el form inline */}
                    {editingServicio?.id === servicio.id ? (
                      <li className="py-4 px-4 bg-gray-50 border border-gray-200 rounded-xl my-3 shadow-sm">
                        <h4 className="text-sm font-bold text-app-marineblue mb-4">Editando: {servicio.nombre}</h4>
                        <ServicioForm 
                          initialData={editingServicio} 
                          recursoNombre={recurso.nombre}
                          onSubmit={handleServicioSubmit} 
                          onCancel={handleCloseServicioForm} 
                        />
                      </li>
                    ) : (
                      
                      <li className="py-3 flex justify-between items-stretch group">
                        
                        {/* Contenedor Izquierdo (Info y Descripción) */}
                        <div className="pr-4 flex flex-col justify-start"> 
                          <span className="font-bold text-app-black block text-base leading-none mb-1.5">{servicio.nombre}</span>
                          <span className="text-sm  flex items-center gap-1.5">
                            <Clock className="w-4 h-4" /> 
                            <span>{servicio.duracionMinutos} min</span>
                          </span>
                          
                          {/* El párrafo se renderiza SIEMPRE para mantener altura */}
                          <p className="text-sm text-gray-500 mt-1.5 leading-snug line-clamp-1">
                            {servicio.descripcion || '\u00A0'}
                          </p>
                        </div>
                        
                        {/* Contenedor Derecho (Precio y Acciones) */}
                        <div className="text-right shrink-0 flex flex-col justify-between">
                          <span className="font-semibold text-app-black block text-lg leading-none">${Number(servicio.precio).toLocaleString('es-AR')}</span>
                          
                          {/* mt-auto empuja los botones bien al fondo */}
                          <div className="flex gap-3 justify-end mt-auto pt-2">
                            <button onClick={() => handleOpenServicioForm(servicio)} className="text-xs font-bold text-app-black hover:text-app-marineblue uppercase transition-colors">Editar</button>
                            <button onClick={() => onDeleteServicio(servicio.id, servicio.nombre)} className="text-xs font-bold text-gray-400 hover:text-app-red uppercase transition-colors">Eliminar</button>
                          </div>
                        </div>
                        
                      </li>
                    )}
                  </React.Fragment>
                ))}
              </ul>
            ) : (
              <p className="text-sm text-gray-400 py-6 text-center italic">No hay servicios asignados a este recurso todavía.</p>
            )}
          </div>

          {/* 2. FORMULARIO AGREGAR NUEVO */}
          {isAddingServicio && !editingServicio && (
            <div className="border-t border-gray-100 p-6 bg-gray-50">
              <h4 className="text-sm font-bold text-app-marineblue mb-4">Nuevo servicio para: {recurso.nombre}</h4>
              <ServicioForm 
                initialData={null} 
                recursoNombre={recurso.nombre}
                onSubmit={handleServicioSubmit} 
                onCancel={handleCloseServicioForm} 
              />
            </div>
          )}

          {/* 3. BOTÓN AGREGAR */}
          {!isAddingServicio && (
            <div className="bg-app-black/5 p-4 flex justify-end border-t border-gray-50">
              <button onClick={() => handleOpenServicioForm()} className="text-sm bg-app-marineblue text-white px-6 py-2.5 rounded-xl font-semibold hover:bg-app-black/90 transition-colors shadow-sm">
                + Agregar Servicio
              </button>
            </div>
          )}

        </div>
      )}
    </article>
  );
};

export default RecursoAccordion;
