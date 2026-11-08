# Etapa 1C: consultas y datos personales

## Auditoría y contratos

| Endpoint | Exposición anterior / consumidor | Contrato de esta etapa |
| --- | --- | --- |
| GET /api/usuarios | Listado de cuentas con nombre, apellido, email, teléfono y estado; sin consumidor de pantalla | 403: no existe un permiso de listado global. |
| GET /api/usuarios/{id} | Perfil de cualquier cuenta activa; wrapper React sin consumidor | Solo permite consultar el ID de la cuenta autenticada. Los demás IDs devuelven 403. |
| GET /api/usuarios/email/{email} | Perfil de cualquier cuenta activa; usado por usePerfil | Solo permite consultar el email autenticado. React pasa a GET /api/usuarios/perfil. |
| GET y PUT /api/usuarios/perfil | PUT ya utilizaba Principal; faltaba GET | Consulta y edición del perfil propio mediante Principal. El DTO personal conserva sus campos. |
| GET /api/negocios y /api/negocios/publico/{alias} | NegocioResponseDTO incluía emailOwner | DTO público con ID, alias, nombre, descripción, contacto comercial y horarios del negocio. |
| GET /api/negocios/{id} | Metadata administrativa accesible a cualquier cuenta | Propietario o empleado ACTIVO del negocio. Sin emailOwner; incluye indicador propietario. |
| GET /api/negocios/mis-negocios y creación/edición | Gestión del dueño | DTO administrativo con propietario, sin emails privados. |
| GET /api/recursos y /api/recursos/negocio/{id} | DTO con emailUsuario, estado y horarios de empleados | DTO público: ID, nombre, descripción y negocioId. Solo recursos de negocios activos. |
| GET /api/recursos/{id} | Administración accesible a cualquier cuenta | Propietario o empleado ACTIVO del recurso; DTO administrativo con estado, horarios, email asignado e indicador propietario. |
| GET /api/recursos/negocio/{id}/todos | Incluía recursos inactivos y empleados ajenos | Dueño: todos sus recursos. Empleado ACTIVO: solo sus recursos activos del negocio. Otros: 403. |
| GET /api/recursos/mis-asignaciones | Mis Trabajos, agenda y comprobación de invitaciones | Solo asignaciones del Principal, incluidas pendientes para mostrar el aviso; no concede administración. |
| GET /api/servicios/recurso/{id} | Catálogo de reserva sin datos personales | Se conserva DTO; exige recurso y negocio activos. |
| Consultas de disponibilidad | Horas disponibles y días llenos | Se conservan DTO y algoritmo: no contienen información personal. |

Los emails de cuentas y empleados y los tokens de invitación no forman parte de
respuestas públicas. `emailContacto`, teléfono y dirección son datos comerciales
del negocio publicados explícitamente en su perfil; no son los datos de Usuario.
El DTO administrativo de un recurso incluye el email asignado porque el dueño
lo necesita para gestionar la invitación y el empleado necesita conservarlo en
el formulario existente. Otros empleados no reciben ese recurso administrativo.

La invitación por email sigue resolviéndose dentro de la operación autorizada de
asignación de recursos. No depende del listado ni de consultas públicas de usuarios.
El rechazo de una invitación continúa desvinculando al usuario como antes.

## React

- Perfil utiliza GET /usuarios/perfil sin enviar email ni ID para identificarse.
- Gestión de negocio usa propietario y recibe solo los recursos autorizados.
- Mi trabajo reutiliza mis-asignaciones para mostrar invitaciones pendientes;
  solo consulta el recurso administrativo si no está en esas asignaciones.
- La página pública conserva nombre, descripción, catálogo, contacto y horarios
  comerciales. No necesita el email del dueño ni horarios privados de empleados.
- Las URLs administrativas denegadas conservan el aviso de invitación pendiente
  mediante la consulta de asignaciones propias, sin obtener datos de compañeros.

No cambian JWT, las reservas, disponibilidad, RabbitMQ ni Notificaciones.

## Comprobación manual

1. Desde Perfil, consultar y guardar los datos propios: Network debe usar
   /usuarios/perfil. Repetir una consulta cambiando ID o email a otra cuenta:
   debe devolver 403. GET /usuarios tampoco devuelve un listado.
2. Sin iniciar sesión, abrir el perfil público de un negocio y revisar Network:
   no deben aparecer emailOwner, emailUsuario, propietario, estadoAsociacion,
   tokenInvitacion ni datos de cuentas. Contacto comercial y catálogo siguen visibles.
3. Como dueño, administrar recursos y empleados propios, incluidos los inactivos
   en historial/reportes. Consultar un negocio ajeno por su URL administrativa:
   debe denegarse, salvo que exista una asociación ACTIVO que autorice el acceso.
4. Como empleado activo, abrir Mi trabajo y agenda: conservar servicios, horarios
   y reportes propios. El listado administrativo del negocio no debe incluir a
   otros empleados ni recursos inactivos.
5. Como invitado pendiente, verificar el aviso en Mis Trabajos y en las URLs
   administrativas. Las consultas privadas devuelven 403; el aviso usa únicamente
   sus asignaciones. Aceptar/rechazar desde el correo mantiene el flujo existente.

Las pruebas HTTP usan JWT y servicios reales con repositorios y RabbitMQ simulados;
no modifican PostgreSQL ni envían correos. Los tests de contexto dependientes de la
infraestructura se ejecutan por separado.
