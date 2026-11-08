export const esAsignacionActiva = (recurso, email) => Boolean(
  email && recurso?.activo === true && recurso.emailUsuario === email && recurso.estadoAsociacion === 'ACTIVO'
);

export const obtenerAsignacion = (recursos, email) => (
  recursos.find(recurso => esAsignacionActiva(recurso, email))
  || recursos.find(recurso => email && recurso.emailUsuario === email && recurso.estadoAsociacion === 'PENDIENTE')
  || recursos.find(recurso => email && recurso.emailUsuario === email)
);
