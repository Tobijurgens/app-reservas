import { DIAS_ES } from '../../../utils/fechas.js';

const DIAS = Object.keys(DIAS_ES);
const minutos = hora => {
  if (!hora || !/^\d{2}:\d{2}(:\d{2})?$/.test(hora)) return null;
  const [h, m] = hora.split(':').map(Number);
  return h < 24 && m < 60 ? h * 60 + m : null;
};

const intervalo = (horario, dia) => {
  const inicio = minutos(horario.horaInicio);
  const fin = minutos(horario.horaFin);
  if (inicio === null || fin === null || inicio === fin) return null;
  return { inicio: dia * 1440 + inicio, fin: dia * 1440 + fin + (fin < inicio ? 1440 : 0) };
};

export const validarHorariosRecurso = (horarios, horariosNegocio) => {
  if (!horariosNegocio?.length) return [];
  const errores = [];
  for (const horario of horarios) {
    if (!horario.activo) continue;
    const dia = DIAS.indexOf(horario.diaSemana);
    const recurso = intervalo(horario, dia);
    if (!recurso) {
      errores.push(`Completá una jornada válida para el ${DIAS_ES[horario.diaSemana]}.`);
      continue;
    }
    const contenido = [dia - 1, dia].some(diaInicio => {
      const negocio = horariosNegocio.find(h => h.diaSemana === DIAS[(diaInicio + 7) % 7] && h.activo);
      const jornada = negocio && intervalo(negocio, diaInicio);
      return jornada && recurso.inicio >= jornada.inicio && recurso.fin <= jornada.fin;
    });
    if (!contenido) errores.push(`La jornada del ${DIAS_ES[horario.diaSemana]} debe estar dentro del horario del negocio.`);
  }
  return errores;
};
