export const esHoy = (fechaString) => {
  const fecha = new Date(fechaString);
  const ahora = new Date();
  return fecha.getDate() === ahora.getDate() &&
         fecha.getMonth() === ahora.getMonth() &&
         fecha.getFullYear() === ahora.getFullYear();
};

export const esFuturo = (fechaString) => {
  return new Date(fechaString) >= new Date();
};

export const esPasado = (fechaString) => {
  return new Date(fechaString) < new Date();
};

export const ordenarPorFechaAsc = (a, b) => new Date(a.fechaHoraInicio) - new Date(b.fechaHoraInicio);
export const ordenarPorFechaDesc = (a, b) => new Date(b.fechaHoraInicio) - new Date(a.fechaHoraInicio);

export const DIAS_ES = {
  MONDAY: 'Lunes', TUESDAY: 'Martes', WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves', FRIDAY: 'Viernes', SATURDAY: 'Sábado', SUNDAY: 'Domingo'
};

const ORDEN_DIAS = {
  MONDAY: 1, TUESDAY: 2, WEDNESDAY: 3,
  THURSDAY: 4, FRIDAY: 5, SATURDAY: 6, SUNDAY: 7
};

export const ordenarHorariosPorDia = (horarios) => {
  if (!horarios || !Array.isArray(horarios)) return [];
  return [...horarios].sort((a, b) => ORDEN_DIAS[a.diaSemana] - ORDEN_DIAS[b.diaSemana]);
};
export const horariosIniciales = (horarios) => {
  if (horarios?.length) return ordenarHorariosPorDia(horarios).map(horario => ({ ...horario }));
  return Object.keys(DIAS_ES).map(diaSemana => ({
    diaSemana,
    activo: !['SATURDAY', 'SUNDAY'].includes(diaSemana),
    horaInicio: '09:00', horaFin: '18:00',
    horaInicioDescanso: '', horaFinDescanso: '',
  }));
};
