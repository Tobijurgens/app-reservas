# Etapa 1D: invitaciones de empleados

## Comportamiento y endpoints

Abrir el correo muestra una confirmación con el negocio y recurso, sin modificar la asociación.
Los correos nuevos enlazan a `/invitacion/confirmar?token=...` en React. Las rutas anteriores
`/invitacion/aceptar` y `/invitacion/rechazar` siguen mostrando la misma confirmación con ambos botones.
La ruta existente `/invitacion/:accion` ya admite las tres variantes; no requiere cambios en el router.

En Reservas, también a través del Gateway:

| Método | Ruta | Resultado |
| --- | --- | --- |
| GET | `/api/recursos/invitacion?token=...` | Consulta los nombres y el vencimiento; nunca consume el token. |
| GET | `/api/recursos/invitacion/aceptar?token=...` | Alias de consulta para compatibilidad; nunca acepta. |
| GET | `/api/recursos/invitacion/rechazar?token=...` | Alias de consulta para compatibilidad; nunca rechaza. |
| POST | `/api/recursos/invitacion/aceptar` | Acepta después de una decisión explícita. |
| POST | `/api/recursos/invitacion/rechazar` | Rechaza después de una decisión explícita. |

Los POST reciben JSON `{"token":"<token del correo>"}`. No reciben el token por query string.
El token autoriza exclusivamente la decisión sobre la asociación que lo contiene, sin iniciar sesión,
cambiar la cuenta autenticada ni verificar cuentas. Quien posea el enlace puede decidir sobre esa
invitación: no debe compartirse. Las respuestas no incluyen tokens ni emails y usan `Cache-Control: no-store`.
No configurar logs de cuerpos o query strings que capturen estos enlaces.

Respuestas: `200` para consulta/decisión válidas, `404` para token inexistente, consumido,
asociación no pendiente o no disponible, `410` para vencimiento o invitación antigua sin fecha,
y `400` para un cuerpo POST sin token válido. Los errores no reproducen el token.

## Vencimiento y base de datos

Se conserva UUID v4 aleatorio y se agrega únicamente la columna nullable
`recursos.fecha_expiracion_invitacion`, tipo `timestamp without time zone`.
Las invitaciones nuevas y renovadas vencen a los siete días de emitirse. Se usa la misma
convención `LocalDateTime` y zona horaria del servicio Reservas existente.

Antes de desplegar Reservas, si el esquema se administra manualmente, aplicar:

```sql
ALTER TABLE recursos
    ADD COLUMN IF NOT EXISTS fecha_expiracion_invitacion timestamp without time zone;
```

Es una operación aditiva, sin borrar filas ni rellenar fechas anteriores. La configuración actual
`spring.jpa.hibernate.ddl-auto=update` también agrega la columna al iniciar la versión nueva.
No se ejecutó esta modificación sobre las bases de AgendApp durante el desarrollo de esta etapa.

Una invitación PENDIENTE sin fecha **no es válida**: devuelve `410`. Para renovarla, el propietario
edita el recurso y guarda la asignación al mismo email desde el formulario existente. Si estaba
vencida o no tenía fecha, se emite otro UUID, vence en siete días y se envía el evento RabbitMQ
de invitación habitual. Guardar una invitación vigente no regenera el token ni envía otro correo.
Cambiar de empleado también invalida el token anterior. Aceptar, rechazar o quitar la asignación
limpia tanto el token como su vencimiento.

## Uso único y permisos

Aceptar/rechazar ejecuta una transacción con `PESSIMISTIC_WRITE` sobre el recurso encontrado por token.
Dentro del bloqueo se valida PENDIENTE, vencimiento, recurso/negocio activos y cuenta habilitada.
Después se cambia el estado y se elimina el token, antes de confirmar la transacción.
Una segunda decisión espera el bloqueo y, al terminar la primera, ya no encuentra el token.
La edición del recurso bloquea la misma fila para serializar cambios de asignación y decisiones.

PENDIENTE conserva la prohibición de administrar. Solo la aceptación válida cambia la asociación a
ACTIVO. Rechazar mantiene RECHAZADO y desvincula al empleado, como antes; el propietario conserva
su acceso. Los tokens de verificación se buscan en su repositorio separado y la verificación de
cuentas exige además `TipoToken.VERIFICACION`. No se cambió la validación JWT.

Se conservan los eventos y routing keys de invitaciones y avisos al propietario, y el listener
existente de Notificaciones. No se agregó infraestructura ni dependencias. Las pruebas de RabbitMQ
verifican publicación/consumo con mocks y renderizan el correo real con un remitente simulado;
no envían emails ni mensajes al broker de la aplicación.

## Pruebas reproducibles

Desde `backend`, con Java 17 y Maven:

```powershell
mvn.cmd -pl microservice-reservas '-Dtest=ReservasSecurityTest,TurnoServiceTest' test
mvn.cmd -pl microservice-notificaciones '-Dtest=NotificacionSecurityTest,NotificacionListenerTest,EmailInvitacionTest' test
mvn.cmd -pl microservice-reservas,microservice-notificaciones -DskipTests package
```

`InvitacionPostgresTest` se omite si no se define `AGENDAPP_TEST_POSTGRES_URL`.
La prueba exige una base PostgreSQL local dedicada llamada `agendapp_invitaciones_test`, usuario
`postgres`, sin contraseña. No usar una base con información a conservar. Un ejemplo aislado es:

```powershell
docker run --detach --rm --name agendapp-invitaciones-test --publish 127.0.0.1::5432 --env POSTGRES_HOST_AUTH_METHOD=trust --env POSTGRES_DB=agendapp_invitaciones_test --tmpfs /var/lib/postgresql/data postgres:16
docker port agendapp-invitaciones-test 5432
# Reemplazar PUERTO por el puerto local informado por Docker:
$env:AGENDAPP_TEST_POSTGRES_URL = 'jdbc:postgresql://127.0.0.1:PUERTO/agendapp_invitaciones_test'
mvn.cmd -pl microservice-reservas '-Dtest=InvitacionPostgresTest' '-DargLine=-Duser.timezone=GMT-03:00' test
Remove-Item Env:AGENDAPP_TEST_POSTGRES_URL
docker stop agendapp-invitaciones-test
```

La prueba usa PostgreSQL real, repositorios JPA y transacciones reales; solo RabbitTemplate se simula.
Mantiene abierta la primera transacción y consulta `pg_stat_activity`/`pg_blocking_pids` para comprobar
que la segunda conexión espera un bloqueo. Prueba aceptar antes de rechazar y el orden inverso;
exige un único éxito, un único aviso y estado persistido coherente.

Desde `frontend`:

```powershell
.\node_modules\.bin\eslint.cmd src/services/RecursosService.js src/features/auth/pages/ProcesarInvitacion.jsx
npm.cmd run build
```

## Verificación manual

1. Con los módulos actualizados, invitar una cuenta ACTIVA desde la edición/creación de un recurso.
   Comprobar el correo y la tarjeta PENDIENTE en Mis Trabajos; la URL administrativa debe seguir bloqueada.
2. Abrir el correo, incluso un enlace anterior `/invitacion/aceptar` o `/invitacion/rechazar`.
   Ver negocio y recurso con ambos botones. En DevTools debe aparecer solo el GET de consulta,
   y la asociación debe seguir PENDIENTE hasta hacer clic.
3. Aceptar: comprobar el POST con token en el cuerpo, botones deshabilitados mientras se procesa,
   confirmación y acceso a la administración en Mis Trabajos al volver al panel.
4. Volver a abrir/reutilizar el mismo enlace: debe indicar invitación no disponible y no permitir
   revertir la decisión. Repetir con otra invitación eligiendo Rechazar y verificar que no habilita acceso.
5. Abrir un token inventado: mostrar invitación no disponible. Una invitación anterior sin vencimiento
   debe mostrarse vencida. El propietario puede renovarla guardando la asignación al mismo email;
   el correo nuevo debe funcionar y el enlace anterior debe quedar inválido.
6. Para probar vencimientos en una base de desarrollo aislada, ajustar únicamente el vencimiento de
   una invitación de prueba a una fecha pasada. GET y ambos POST deben devolver `410`, sin cambiar el estado.
7. Simular conexión sin red: mostrar error con opción Volver a consultar (solo GET). Iniciar sesión antes
   de abrir el correo y comprobar que la sesión se conserva en éxitos y errores de invitación.

El build y las pruebas de backend no sustituyen una prueba manual del navegador, la entrega SMTP ni
la integración con un broker real. La publicación de avisos conserva el comportamiento transaccional
previo; esta etapa no implementa outbox ni cambios en reservas/disponibilidad.
