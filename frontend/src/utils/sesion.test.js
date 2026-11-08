import test from 'node:test';
import assert from 'node:assert/strict';
import { debeInvalidarSesion, sesionDesdeToken } from './sesion.js';

const ahora = 1700000000000;
const token = payload => `cabecera.${btoa(JSON.stringify(payload))}.firma`;
const error401 = (url, method = 'get', jwt = 'actual') => ({
  config: { url, method, headers: { Authorization: `Bearer ${jwt}` } },
  response: { status: 401 },
});

test('interpreta la identidad y el vencimiento del JWT para la interfaz', () => {
  const jwt = token({ sub: 'persona@example.com', exp: ahora / 1000 + 60 });
  assert.deepEqual(sesionDesdeToken(jwt, ahora), { token: jwt, email: 'persona@example.com', expiraEn: ahora + 60000 });
});

test('descarta tokens vencidos, malformados o sin identidad o vencimiento', () => {
  for (const jwt of [null, 'invalido', token({ sub: 'a@example.com', exp: ahora / 1000 }), token({ exp: ahora / 1000 + 60 }), token({ sub: 'a@example.com' })]) {
    assert.equal(sesionDesdeToken(jwt, ahora), null);
  }
});

test('un 401 de una operación privada con el token actual invalida la sesión', () => {
  assert.equal(debeInvalidarSesion(error401('/usuarios/perfil'), 'actual'), true);
  assert.equal(debeInvalidarSesion(error401('/notificaciones/1/leer', 'put'), 'actual'), true);
});

test('un error de login o una operación pública no cierra otra sesión válida', () => {
  for (const url of ['/auth/login', '/auth/register', '/auth/verificar?token=prueba', '/negocios/publico/local', '/recursos/negocio/1', '/servicios/recurso/1', '/turnos/disponibles?fecha=2026-11-07', '/turnos/dias-llenos', '/recursos/invitacion/aceptar']) {
    assert.equal(debeInvalidarSesion(error401(url), 'actual'), false, url);
  }
});

test('la respuesta de una sesión anterior y las respuestas 403 no cierran la sesión actual', () => {
  assert.equal(debeInvalidarSesion(error401('/usuarios/perfil', 'get', 'anterior'), 'actual'), false);
  assert.equal(debeInvalidarSesion(error401('/usuarios/perfil'), null), false);
  const prohibido = error401('/usuarios/perfil');
  prohibido.response.status = 403;
  assert.equal(debeInvalidarSesion(prohibido, 'actual'), false);
});

test('distingue las rutas administrativas de las consultas públicas y normaliza /api', () => {
  assert.equal(debeInvalidarSesion(error401('http://localhost:8080/api/recursos/negocio/1/todos'), 'actual'), true);
  assert.equal(debeInvalidarSesion(error401('/api/turnos/disponibles'), 'actual'), false);
  assert.equal(debeInvalidarSesion(error401('/recursos/negocio/1', 'post'), 'actual'), true);
});
