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