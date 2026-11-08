import test from 'node:test';
import assert from 'node:assert/strict';
import { fechaCalendario, hoyEnNegocio } from './fechasReserva.js';

test('serializa el día seleccionado sin convertirlo a UTC', () => {
  assert.equal(fechaCalendario(new Date(2026, 10, 6)), '2026-11-06');
  assert.equal(fechaCalendario(new Date(2027, 0, 1)), '2027-01-01');
});

test('la noche argentina conserva su fecha aunque en UTC ya sea el día siguiente', () => {
  assert.equal(fechaCalendario(hoyEnNegocio(new Date('2026-11-07T01:30:00Z'))), '2026-11-06');
});

test('el día cambia a medianoche argentina', () => {
  assert.equal(fechaCalendario(hoyEnNegocio(new Date('2026-11-07T02:59:59Z'))), '2026-11-06');
  assert.equal(fechaCalendario(hoyEnNegocio(new Date('2026-11-07T03:00:00Z'))), '2026-11-07');
});
