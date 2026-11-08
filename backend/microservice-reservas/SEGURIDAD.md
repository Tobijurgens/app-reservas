# Etapa 1B: cuentas y permisos por recurso

## Precisiones del contrato antes de implementar la corrección

- Se conservan los endpoints y DTO existentes. `PUT /api/recursos/{id}` recibe
  el recurso completo, como ya envía React: nombre, descripción, `idNegocio`,
  `emailUsuario` y horarios semanales.
- El propietario puede editar nombre, descripción, horarios y asignación del
  empleado. El recurso sigue perteneciendo al negocio original: `idNegocio`
  debe coincidir con el persistido. Este endpoint no transfiere recursos.
- El empleado con asociación ACTIVO puede editar descripción y horarios,
  funcionalidades ya presentes en React. Debe conservar nombre, `idNegocio`
  y `emailUsuario`. Enviar el email vacío/null también es una desasignación
  y se rechaza; no se confía en campos deshabilitados de la interfaz.
- En las operaciones revisadas de recursos, servicios, agendas, turnos y reportes,
  los intentos de administración sin permiso o de cambiar campos reservados
  devuelven 403 con el formato de error existente, en lugar del 400 genérico.
- Las solicitudes privadas sin JWT válido o de una cuenta no habilitada
  devuelven 401. Los endpoints públicos siguen disponibles como anónimos.
  Registro, login y verificación conservan sus rutas y contratos actuales.

## Permisos

| Identidad | Acceso |
| --- | --- |
| Propietario | Administra sus recursos, asignaciones, servicios, agendas, reportes y turnos según las operaciones existentes. |
| Empleado ACTIVO | Administra servicios, descripción, horarios, agenda, historial, reportes y turnos de su recurso activo. |
| Empleado PENDIENTE, RECHAZADO o sin asociación | Sin permisos de administración como empleado. |
| Cliente | Conserva sus operaciones sobre reservas propias; no obtiene permisos administrativos. |

Las reglas usan la relación persistida, no roles enviados por el cliente. Un
usuario que también sea propietario o cliente conserva los permisos de esa
relación aunque tenga una invitación pendiente como empleado.
Los reportes de un recurso deben corresponder al negocio indicado en la URL.
Los reportes globales y el historial global siguen siendo exclusivos del dueño.
Las consultas públicas de recursos, servicios y disponibilidad no cambian.

## Desactivación de cuentas

Reservas carga al usuario desde su repositorio en cada solicitud con JWT y
comprueba `isEnabled()`: solo ACTIVO está habilitado. SUSPENDIDO (incluida la
baja lógica), PENDIENTE_VERIFICACION y usuarios inexistentes no se autentican.
El rechazo se aplica a las solicitudes posteriores a la desactivación confirmada;
no cancela solicitudes que ya estaban en curso. No hay listas negras ni Redis.

Esto NO es revocación global: Notificaciones valida firma y vencimiento de forma
independiente. Allí un token anterior puede seguir utilizándose hasta su
expiración. No se agregan consultas a la base de Reservas ni mecanismos nuevos
de revocación. Si una cuenta se reactiva, un JWT anterior aún vigente puede volver
a ser válido: esta etapa verifica el estado actual, no revoca tokens individuales.

## Comprobación manual

1. Como dueño, crear un recurso e invitar a otro usuario activo. Antes de aceptar,
   ese usuario no debe poder guardar cambios ni consultar agenda o reportes privados.
2. Aceptar la invitación desde el correo. En Mi trabajo, guardar descripción y
   horarios y gestionar los servicios del recurso. Las operaciones deben funcionar.
3. Desde las herramientas de desarrollo, repetir la petición de edición cambiando
   `emailUsuario`, nombre o `idNegocio`: debe devolver 403 y no guardar cambios.
   Repetir sobre un recurso ajeno: también debe rechazarse.
4. Rechazar una invitación y verificar que no concede permisos de administración.
   Como cliente, comprobar que siguen disponibles las reservas propias.
5. Conservar localmente el JWT de una cuenta de prueba activa, eliminar esa cuenta
   desde Perfil y repetir una petición privada a Reservas con ese JWT: debe
   devolver 401. No compartir ni versionar tokens. La baja tiene efectos de dominio;
   realizar esta comprobación con una cuenta destinada a pruebas.
6. Repetir solicitudes por el Gateway y directamente en el puerto 8081. Las
   consultas públicas siguen funcionando y login/verificación siguen disponibles.

Las pruebas automatizadas de esta etapa simulan repositorios y RabbitMQ; no
modifican PostgreSQL ni envían correos reales. Los tests HTTP usan el filtro JWT,
Spring Security, controladores y servicios reales.

Desde `backend`, con Java 17 y Maven:

```powershell
mvn.cmd -pl microservice-reservas -am test "-Dtest=ReservasSecurityTest,TurnoServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false"
mvn.cmd package -DskipTests
```

El `contextLoads` preexistente depende del entorno y no forma parte de la ejecución
aislada. Para probar con Docker, compilar los JAR y reconstruir el contenedor de
Reservas; no hace falta cambiar ni compartir bases entre microservicios.
