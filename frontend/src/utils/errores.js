export const mensajeError = (error, fallback = 'No se pudo completar la operación.') => {
  const datos = error?.response?.data;
  if (typeof datos?.message === 'string' && datos.message.trim()) return datos.message;
  if (typeof datos === 'string' && datos.trim()) return datos;
  return fallback;
};
