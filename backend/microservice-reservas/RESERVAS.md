# Etapa 2A: integridad de reservas

## Exclusión entre reservas concurrentes

`TurnoService.crearTurno` inicia una transacción con aislamiento explícito `READ_COMMITTED`.
Obtiene el recurso con el método existente `findByIdActivoParaActualizar`, que usa
`PESSIMISTIC_WRITE`. Mantiene el bloqueo de esa fila hasta el commit o rollback.
Después de adquirirlo valida el intervalo, consulta nuevamente los turnos CONFIRMADOS
superpuestos y guarda la reserva. Otra solicitud para el mismo recurso espera el bloqueo;
cuando continúa, `READ_COMMITTED` permite que vea la reserva recién confirmada.
Recursos diferentes no comparten este bloqueo.

La única operación que puede ocupar un intervalo es la creación. Se prohíbe
`CANCELADO → CONFIRMADO`, también para propietarios y empleados autorizados.
Repetir `CONFIRMADO → CONFIRMADO` es una operación idempotente sin escritura ni eventos.
La cancelación bloquea la fila del turno para impedir dos cancelaciones exitosas y avisos
duplicados. DELETE y PATCH a CANCELADO usan el mismo flujo de cancelación existente.
Se conservan las publicaciones RabbitMQ y sus payloads; no se cambia la infraestructura.

## Consultas y validaciones

`existeSolapamiento` y `findTurnosOcupados` usan la misma condición:

```text
existente.inicio < solicitado.fin AND existente.fin > solicitado.inicio
AND existente.estado = CONFIRMADO
```

`findTurnosOcupados` reemplaza el anterior BETWEEN sobre el inicio del turno, que omitía
ocupaciones iniciadas antes del rango. Los intervalos son semiabiertos: 10:00–10:30 y
10:30–11:00 pueden coexistir. El endpoint de ocupaciones diarias consulta desde medianoche
hasta la medianoche siguiente, excluida. Las consultas de reportes permanecen intactas.

La creación comprueba sesión, cuenta ACTIVA, fecha futura (también después de esperar el
bloqueo), servicio existente/activo, recurso existente/activo, negocio activo, pertenencia del
servicio y duración positiva no nula. La duración y el fin se calculan en el backend.
El intervalo completo debe entrar en un bloque de jornada tanto del recurso como del negocio,
sin atravesar sus descansos. Ausencia de horarios, día cerrado o configuraciones incompletas
se rechazan. Se reutiliza la conversión existente de horas a fechas absolutas, contemplando
la jornada iniciada el día anterior. La disponibilidad también rechaza duración cero o negativa
para evitar bucles y servicios de otro recurso. No se reescribió el algoritmo de disponibilidad;
su revisión general nocturna continúa reservada para 2B.

## Contrato HTTP

Se conservan las rutas y el `200` de éxito:

- `POST /api/turnos`: crea una reserva, con `idServicio`, `fechaHoraInicio` y un `idRecurso`
  opcional positivo. Si falta, se deriva del servicio para conservar clientes anteriores.
  Cuando se envía, el servicio debe pertenecer a ese recurso. El modal ahora lo envía.
- `DELETE /api/turnos/{id}`: cancela una reserva autorizada.
- `PATCH /api/turnos/{id}/estado?nuevoEstado=...`: cancela o conserva una confirmación
  existente; nunca reactiva una cancelación.

Los errores conservan el formato JSON del handler actual (`status`, `error`, `message`, etc.):

| Estado | Casos |
| --- | --- |
| 400 | Fecha/identificadores inválidos, servicio de otro recurso, duración o intervalo inválidos. |
| 401 | Sin autenticación válida; la cadena JWT existente también rechaza cuentas deshabilitadas. |
| 403 | Cuenta sin permisos para modificar el turno; validación de cuenta deshabilitada dentro del servicio. |
| 404 | Turno, recurso o servicio inexistente; recurso o servicio inactivos. |
| 409 | Horario ocupado, negocio inactivo, cancelación repetida o reconfirmación prohibida. |

Ante un 409 de creación, el modal conserva la fecha, limpia el horario, muestra el mensaje y
vuelve a consultar disponibilidad para elegir otro. No cambia el diseño ni la autenticación.

No hay cambios de esquema, dependencias nuevas ni migraciones para esta etapa.
El bloqueo protege las escrituras realizadas por este servicio; no es una restricción de
exclusión que proteja inserciones manuales de SQL ajenas a la aplicación.

## Verificación automática

Desde `backend`, con Java 17 y Maven:

```powershell
mvn.cmd -pl microservice-reservas '-Dtest=ReservasSecurityTest,TurnoServiceTest' test
```

La integración exige PostgreSQL real en una base local dedicada `agendapp_turnos_test`.
No apunta a `turnos_db`, no usa volúmenes de AgendApp y se omite si no se define la variable.
Ejemplo de entorno aislado temporal:

```powershell
docker run --detach --rm --name agendapp-turnos-test --publish 127.0.0.1::5432 --env POSTGRES_HOST_AUTH_METHOD=trust --env POSTGRES_DB=agendapp_turnos_test --tmpfs /var/lib/postgresql/data postgres:16
docker port agendapp-turnos-test 5432
# Reemplazar PUERTO por el puerto informado por Docker:
$env:AGENDAPP_TEST_TURNOS_POSTGRES_URL = 'jdbc:postgresql://127.0.0.1:PUERTO/agendapp_turnos_test'
mvn.cmd -pl microservice-reservas '-Dtest=ReservasSecurityTest,TurnoServiceTest,TurnoPostgresTest' '-DargLine=-Duser.timezone=GMT-03:00' package
Remove-Item Env:AGENDAPP_TEST_TURNOS_POSTGRES_URL
docker stop agendapp-turnos-test
```

La integración comprueba las dos consultas con intervalos contiguos, parciales, contenidos,
contenedores y cancelados. Para concurrencia mantiene la primera transacción abierta y
consulta `pg_stat_activity` y `pg_blocking_pids`, exigiendo un bloqueo real sobre la segunda
solicitud. Comprueba los resultados y las filas persistidas: exactamente una confirmación
para reservas idénticas o parcialmente superpuestas; dos para reservas consecutivas,
tanto anteriores como posteriores. También comprueba dos cancelaciones simultáneas.
Solo RabbitTemplate se simula para que las pruebas no envíen mensajes ni correos reales.

Desde `frontend`:

```powershell
.\node_modules\.bin\eslint.cmd src/features/publico/components/ModalReserva.jsx src/services/TurnosService.js
npm.cmd run build
```

Los tests generales `contextLoads` no se incluyen en estos comandos porque utilizan la
configuración e infraestructura de la aplicación. Tampoco son pruebas de navegador o SMTP.

Validación ejecutada para esta etapa: 110 pruebas HTTP de `ReservasSecurityTest`, 41 de
`TurnoServiceTest` y 19 de `TurnoPostgresTest`, todas exitosas (170 en total, sin omisiones).
Las 19 pruebas de integración se ejecutaron con PostgreSQL 16 real en un contenedor temporal
dedicado, posteriormente detenido. También pasaron el empaquetado de Reservas, el lint de
los dos archivos frontend afectados y su build. Vite informó el aviso de bundle mayor a 500 kB.

## Comprobación manual

1. Reiniciar/reconstruir Reservas y usar el frontend actualizado.
2. Con dos cuentas, abrir el mismo servicio y seleccionar el mismo horario antes de confirmar.
   Confirmar desde ambas sesiones: una reserva debe guardarse y la otra debe mostrar el conflicto,
   limpiar la selección y cargar horarios nuevamente.
3. Reservar el horario inmediatamente posterior: debe funcionar. Un intervalo parcialmente
   superpuesto enviado a la API debe devolver 409.
4. Enviar a la API una fecha pasada, un horario fuera de jornada o que cruce un descanso:
   debe devolver 400 aunque React no haya permitido seleccionarlo.
5. Cancelar una reserva propia: debe funcionar. Repetir la cancelación o intentar reconfirmarla
   por PATCH debe devolver 409; intentar modificar la reserva de un tercero debe devolver 403.
