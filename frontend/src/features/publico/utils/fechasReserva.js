// Las fechas del calendario son días del negocio; no representan instantes UTC.
export const fechaCalendario = (fecha) => {
  const mes = String(fecha.getMonth() + 1).padStart(2, '0');
  const dia = String(fecha.getDate()).padStart(2, '0');
  return `${fecha.getFullYear()}-${mes}-${dia}`;
};

export const hoyEnNegocio = (ahora = new Date()) => {
  const partes = new Intl.DateTimeFormat('en', {
    timeZone: 'America/Argentina/Buenos_Aires', year: 'numeric', month: '2-digit', day: '2-digit',
  }).formatToParts(ahora);
  const valor = (tipo) => Number(partes.find((parte) => parte.type === tipo).value);
  // DatePicker espera un Date local. Sus componentes representan la fecha argentina.
  return new Date(valor('year'), valor('month') - 1, valor('day'));
};
