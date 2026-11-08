import test from 'node:test';
import assert from 'node:assert/strict';
import { validarHorariosRecurso } from './horarios.js';
import { horariosIniciales } from '../../../utils/fechas.js';

const jornada = (diaSemana, horaInicio, horaFin, activo = true) => ({ diaSemana, horaInicio, horaFin, activo });

test('acepta una jornada diurna contenida y rechaza los excesos', () => {
  const negocio = [jornada('MONDAY', '09:00', '18:00')];
  assert.deepEqual(validarHorariosRecurso([jornada('MONDAY', '09:00', '18:00')], negocio), []);
  assert.equal(validarHorariosRecurso([jornada('MONDAY', '08:00', '12:00')], negocio).length, 1);
  assert.equal(validarHorariosRecurso([jornada('MONDAY', '12:00', '19:00')], negocio).length, 1);
});

test('acepta la madrugada del martes dentro de la jornada iniciada el lunes', () => {
  const negocio = [jornada('MONDAY', '22:00', '06:00'), jornada('TUESDAY', '09:00', '18:00', false)];
  assert.deepEqual(validarHorariosRecurso([jornada('TUESDAY', '01:00', '03:00')], negocio), []);
  assert.equal(validarHorariosRecurso([jornada('TUESDAY', '05:00', '07:00')], negocio).length, 1);
});

test('contempla el cambio de semana y una jornada nocturna del mismo día', () => {
  assert.deepEqual(validarHorariosRecurso([jornada('MONDAY', '01:00', '03:00')], [jornada('SUNDAY', '22:00', '06:00')]), []);
  assert.deepEqual(validarHorariosRecurso([jornada('MONDAY', '23:00', '05:00')], [jornada('MONDAY', '22:00', '06:00')]), []);
});

test('no admite días cerrados ni jornadas incompletas y respeta días inactivos', () => {
  assert.equal(validarHorariosRecurso([jornada('MONDAY', '09:00', '10:00')], [jornada('MONDAY', '09:00', '18:00', false)]).length, 1);
  assert.equal(validarHorariosRecurso([jornada('MONDAY', '', '10:00')], [jornada('MONDAY', '09:00', '18:00')]).length, 1);
  assert.deepEqual(validarHorariosRecurso([jornada('MONDAY', '', '', false)], [jornada('MONDAY', '09:00', '18:00')]), []);
});

test('cada formulario obtiene copias independientes de los horarios originales', () => {
  const original = [jornada('MONDAY', '09:00', '18:00')];
  const copia = horariosIniciales(original);
  copia[0].horaInicio = '10:00';
  assert.equal(original[0].horaInicio, '09:00');
  assert.equal(horariosIniciales().length, 7);
});
