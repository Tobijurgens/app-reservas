import { useState, useEffect } from 'react';

const DIAS = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];
const DIAS_ES = {
  MONDAY: 'Lunes', TUESDAY: 'Martes', WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves', FRIDAY: 'Viernes', SATURDAY: 'Sábado', SUNDAY: 'Domingo'
};

const HorariosSemana = ({ horarios, setHorarios }) => {
  // Si no vienen horarios iniciales, armamos la semana por defecto (Lun-Vie 09 a 18)
  useEffect(() => {
    if (!horarios || horarios.length === 0) {
      const defaultHorarios = DIAS.map(dia => ({
        diaSemana: dia,
        activo: !['SATURDAY', 'SUNDAY'].includes(dia),
        horaInicio: '09:00',
        horaFin: '18:00',
        horaInicioDescanso: '',
        horaFinDescanso: ''
      }));
      setHorarios(defaultHorarios);
    }
  }, []);

  const handleChange = (index, campo, valor) => {
    const nuevosHorarios = [...horarios];
    nuevosHorarios[index][campo] = valor;
    setHorarios(nuevosHorarios);
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
    <div className="bg-white border border-gray-200 rounded-xl overflow-hidden mt-6 mb-6">
      <div className="bg-gray-50 px-4 py-3 border-b border-gray-200">
        <h3 className="font-bold text-gray-800">Horario de inicio y fin de la jornada</h3>
        <p className="text-xs text-gray-500">Activá los días que trabajás y configurá tus horas.</p>
      </div>
      
      <div className="overflow-x-auto">
        <table className="w-full text-sm text-left">
          <thead className="text-xs text-gray-500 uppercase bg-gray-50 border-b">
            <tr>
              <th className="px-4 py-3">Día</th>
              <th className="px-4 py-3 text-center">Estado</th>
              <th className="px-4 py-3">Jornada (Inicio - Fin)</th>
              <th className="px-4 py-3">Descanso (Opcional)</th>
              <th className="px-4 py-3"></th>
            </tr>
          </thead>
          <tbody>
            {horarios.map((dia, index) => (
              <tr key={dia.diaSemana} className="border-b last:border-0 hover:bg-gray-50">
                <td className="px-4 py-3 font-medium text-gray-900 w-28">
                  {DIAS_ES[dia.diaSemana]}
                </td>
                
                <td className="px-4 py-3 text-center">
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input 
                      type="checkbox" 
                      className="sr-only peer" 
                      checked={dia.activo}
                      onChange={(e) => handleChange(index, 'activo', e.target.checked)}
                    />
                    <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
                  </label>
                </td>

                <td className="px-4 py-3">
                  <div className="flex items-center gap-2">
                    <input 
                      type="time" 
                      disabled={!dia.activo}
                      value={dia.horaInicio} 
                      onChange={(e) => handleChange(index, 'horaInicio', e.target.value)}
                      className={`border rounded px-2 py-1 ${!dia.activo ? 'bg-gray-100 text-gray-400' : 'bg-white'}`}
                    />
                    <span className="text-gray-400">-</span>
                    <input 
                      type="time" 
                      disabled={!dia.activo}
                      value={dia.horaFin} 
                      onChange={(e) => handleChange(index, 'horaFin', e.target.value)}
                      className={`border rounded px-2 py-1 ${!dia.activo ? 'bg-gray-100 text-gray-400' : 'bg-white'}`}
                    />
                  </div>
                </td>

                <td className="px-4 py-3">
                  <div className="flex items-center gap-2">
                    <input 
                      type="time" 
                      disabled={!dia.activo}
                      value={dia.horaInicioDescanso || ''} 
                      onChange={(e) => handleChange(index, 'horaInicioDescanso', e.target.value)}
                      className={`border rounded px-2 py-1 ${!dia.activo ? 'bg-gray-100 text-gray-400' : 'bg-white'}`}
                    />
                    <span className="text-gray-400">-</span>
                    <input 
                      type="time" 
                      disabled={!dia.activo}
                      value={dia.horaFinDescanso || ''} 
                      onChange={(e) => handleChange(index, 'horaFinDescanso', e.target.value)}
                      className={`border rounded px-2 py-1 ${!dia.activo ? 'bg-gray-100 text-gray-400' : 'bg-white'}`}
                    />
                  </div>
                </td>

                <td className="px-4 py-3 text-right">
                  {index === 0 && (
                    <button 
                      type="button" 
                      onClick={() => copiarEnTodos(index)}
                      className="text-xs text-blue-600 hover:text-blue-800 hover:underline"
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
  );
};

export default HorariosSemana;