import { useState } from 'react';
import { ChevronDown } from 'lucide-react';

import { DIAS_ES } from '../../../utils/fechas';

const HorariosSemana = ({ horarios, setHorarios }) => {
  const [isExpanded, setIsExpanded] = useState(false);

  const handleChange = (index, campo, valor) => {
    setHorarios(horarios.map((horario, posicion) => (
      posicion === index ? { ...horario, [campo]: valor } : horario
    )));
  };

  const copiarEnTodos = (indexOrigen) => {
    const origen = horarios[indexOrigen];
    const nuevosHorarios = horarios.map(h => ({
      ...h,
      activo: origen.activo,
      horaInicio: origen.horaInicio,
      horaFin: origen.horaFin,
      horaInicioDescanso: origen.horaInicioDescanso,
      horaFinDescanso: origen.horaFinDescanso
    }));
    setHorarios(nuevosHorarios);
  };

  if (!horarios || horarios.length === 0) return null;

  return (
    <div className="bg-white border border-gray-100 rounded-2xl overflow-hidden mt-8 mb-6 shadow-sm transition-all duration-300">
      <button
        type="button"
        onClick={() => setIsExpanded(!isExpanded)}
        className="w-full bg-app-white px-6 py-4 border-b border-gray-100 flex justify-between items-center hover:bg-gray-50 transition-colors focus:outline-none"
      >
        <div className="text-left">
          <h3 className="font-bold text-app-black">Horario de inicio y fin de la jornada</h3>
          <p className="text-sm text-app-marineblue mt-1">Activá los días que trabajás y configurá tus horas.</p>
        </div>
        <ChevronDown className={`w-6 h-6 text-app-marineblue transform transition-transform duration-300 ${isExpanded ? 'rotate-180' : ''}`} />
      </button>
      
      <div 
        className={`transition-all duration-300 ease-in-out overflow-hidden ${
          isExpanded ? 'max-h-[1000px] opacity-100' : 'max-h-0 opacity-0'
        }`}
      >
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left">
            <thead className="text-xs text-app-marineblue uppercase bg-white border-b border-gray-100">
              <tr>
                <th className="px-6 py-4">Día</th>
                <th className="px-6 py-4 text-center">Estado</th>
                <th className="px-6 py-4">Jornada (Inicio - Fin)</th>
                <th className="px-6 py-4">Descanso (Opcional)</th>
                <th className="px-6 py-4"></th>
              </tr>
            </thead>
            <tbody>
              {horarios.map((dia, index) => (
                <tr key={dia.diaSemana} className="border-b border-gray-50 last:border-0 hover:bg-gray-50/50 transition-colors">
                  <td className="px-6 py-4 font-semibold text-app-black w-32">
                    {DIAS_ES[dia.diaSemana]}
                  </td>
                  
                  <td className="px-6 py-4 text-center">
                    <label className="relative inline-flex items-center cursor-pointer">
                      <input 
                        type="checkbox" 
                        className="sr-only peer" 
                        checked={dia.activo}
                        onChange={(e) => handleChange(index, 'activo', e.target.checked)}
                      />
                      <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-0.5 after:left-0.5 after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-app-marineblue"></div>
                    </label>
                  </td>

                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <input 
                        type="time" 
                        disabled={!dia.activo}
                        value={dia.horaInicio} 
                        onChange={(e) => handleChange(index, 'horaInicio', e.target.value)}
                        className={`border-none rounded-lg px-3 py-2 focus:ring-2 focus:ring-app-marineblue transition-colors ${!dia.activo ? 'bg-gray-50 text-gray-400' : 'bg-app-white text-app-black'}`}
                      />
                      <span className="text-gray-300 font-bold">-</span>
                      <input 
                        type="time" 
                        disabled={!dia.activo}
                        value={dia.horaFin} 
                        onChange={(e) => handleChange(index, 'horaFin', e.target.value)}
                        className={`border-none rounded-lg px-3 py-2 focus:ring-2 focus:ring-app-marineblue transition-colors ${!dia.activo ? 'bg-gray-50 text-gray-400' : 'bg-app-white text-app-black'}`}
                      />
                    </div>
                  </td>

                  <td className="px-6 py-4">
                    <div className="flex items-center gap-3">
                      <input 
                        type="time" 
                        disabled={!dia.activo}
                        value={dia.horaInicioDescanso || ''} 
                        onChange={(e) => handleChange(index, 'horaInicioDescanso', e.target.value)}
                        className={`border-none rounded-lg px-3 py-2 focus:ring-2 focus:ring-app-marineblue transition-colors ${!dia.activo ? 'bg-gray-50 text-gray-400' : 'bg-gray-100 text-app-black focus:bg-white'}`}
                      />
                      <span className="text-gray-300 font-bold">-</span>
                      <input 
                        type="time" 
                        disabled={!dia.activo}
                        value={dia.horaFinDescanso || ''} 
                        onChange={(e) => handleChange(index, 'horaFinDescanso', e.target.value)}
                        className={`border-none rounded-lg px-3 py-2 focus:ring-2 focus:ring-app-marineblue transition-colors ${!dia.activo ? 'bg-gray-50 text-gray-400' : 'bg-gray-100 text-app-black focus:bg-white'}`}
                      />
                    </div>
                  </td>

                  <td className="px-6 py-4 text-right">
                    {index === 0 && (
                      <button 
                        type="button" 
                        onClick={() => copiarEnTodos(index)}
                        className="text-xs font-bold text-app-marineblue hover:text-app-black uppercase tracking-wider transition-colors"
                      >
                        Copiar en todos
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};

export default HorariosSemana;
