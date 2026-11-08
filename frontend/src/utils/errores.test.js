import test from 'node:test';
import assert from 'node:assert/strict';
import { mensajeError } from './errores.js';

test('extrae texto de los errores estructurados y de las respuestas de texto', () => {
  assert.equal(mensajeError({ response: { data: { message: 'El token ha expirado.', detalles: {} } } }), 'El token ha expirado.');
  assert.equal(mensajeError({ response: { data: 'Enlace inválido.' } }), 'Enlace inválido.');
});

test('una respuesta inesperada o un fallo de red nunca devuelve un objeto al toast', () => {
  for (const error of [null, new Error('Network Error'), { response: { data: {} } }, { response: { data: { message: {} } } }]) {
    assert.equal(mensajeError(error, 'No pudimos verificar la cuenta.'), 'No pudimos verificar la cuenta.');
  }
});
