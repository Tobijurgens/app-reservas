import { jwtDecode } from 'jwt-decode';

// Solo interpreta la sesión de la interfaz; la firma y los permisos los valida el servidor.
export const sesionDesdeToken = (token, ahora = Date.now()) => {
  if (!token) return null;
  try {
    const payload = jwtDecode(token);
    const email = payload.sub || payload.email;
    const expiraEn = payload.exp * 1000;
    if (typeof email !== 'string' || !email || !Number.isFinite(expiraEn) || expiraEn <= ahora) return null;
    return { token, email, expiraEn };
  } catch {
    return null;
  }
};

export const debeInvalidarSesion = (error, tokenActual) => {
  if (error?.response?.status !== 401 || !tokenActual) return false;
  const config = error.config || {};
  const headers = config.headers;
  const authorization = headers?.get?.('Authorization') || headers?.Authorization || headers?.authorization;
  if (authorization !== `Bearer ${tokenActual}`) return false;

  const ruta = new URL(config.url || '/', 'http://agendapp.local').pathname.replace(/^\/api(?=\/)/, '');
  const metodo = (config.method || 'get').toLowerCase();
  if (/^\/auth(?:\/|$)/.test(ruta) || /^\/recursos\/invitacion(?:\/|$)/.test(ruta)) return false;
  const consultaPublica = metodo === 'get' && (
    /^\/negocios\/publico\/[^/]+$/.test(ruta)
    || /^\/recursos\/negocio\/[^/]+$/.test(ruta)
    || /^\/servicios\/recurso\/[^/]+$/.test(ruta)
    || /^\/turnos\/(disponibles|dias-llenos)$/.test(ruta)
  );
  return !consultaPublica;
};
