# Etapa 3B: recepción idempotente y reintentos SMTP persistentes

## Diagnóstico focalizado

El listener anterior enviaba SMTP, capturaba la excepción y retornaba normalmente después de
guardar ERROR. Con ACK automático, RabbitMQ podía considerar atendido un mensaje cuyo correo
había fallado. No había reintento SMTP ni clave de deduplicación. Una redelivery podía generar
otro correo e historial. Una caída tras aceptar SMTP y antes de persistir también podía duplicar.
Los mensajes que fallaban en la conversión JSON antes del listener quedaban sujetos al manejo
predeterminado del contenedor, sin una evidencia persistente propia.

Se conservan exchange, cinco colas durables y routing keys, DTO JSON, datos y plantillas HTML,
autenticación JWT y respuestas privadas existentes. Tampoco se cambian permisos, tokens,
vencimientos, algoritmo de reservas, disponibilidad ni reportes. `messageId` estable del outbox
de 3A se reutiliza; no se agrega un identificador al cuerpo JSON.

## Parte A: cambio mínimo del outbox de Reservas

Después de cada fallo registrado, los próximos intentos son:

| Fallos | Pausa |
| --- | --- |
| 1 | 60 segundos |
| 2 | 120 segundos |
| 3 | 240 segundos |
| 4 y posteriores | 300 segundos |

El evento nuevo conserva `proximo_intento=creado_en`: es elegible inmediatamente, con la
latencia normal del dispatcher (delay de 3 segundos). El backoff se aplica solo al fallar.
No hay límite de intentos del outbox ni descarte. Se mantienen confirms, mandatory, returns,
persistencia, correlación única por intento y `FOR UPDATE SKIP LOCKED`.

Cada publicación usa el mismo UUID en `messageId` y en el nuevo header `eventId`. La correlación
de confirms sigue siendo distinta por intento: un ACK atrasado no confirma otro envío.
No cambia el JSON ni ninguna transacción de creación/cancelación/asignación de Reservas.

La consulta existente ya excluye filas con `proximo_intento` futuro mediante SQL e índice.
Sin filas elegibles, solo se realiza esa consulta por ciclo: no hay UPDATE de intentos ni
conexión AMQP por esas filas. Se conserva el polling razonable para no demorar eventos nuevos.

### Actividad de conexión

Reservas no tiene listeners: su dispatcher pide una publicación por evento elegible y
RabbitTemplate/CachingConnectionFactory obtienen la conexión necesaria. Una operación puede
generar varios eventos, cada uno con su propio intento. Notificaciones tiene cinco consumidores
que recuperan sus conexiones automáticamente mientras el broker está apagado. Eso es actividad
normal de Spring AMQP, no evidencia de un bucle defectuoso. Las consultas de salud Rabbit de
Actuator también pueden intentar conectarse si alguien solicita ese endpoint.

No se deshabilita recuperación automática ni se agregan conectores. El nuevo backoff reduce
los intentos del productor fallido. El contenedor conserva su recuperación normal y las
excepciones controladas de recepción omiten payload/causa/stack repetitivo; los demás errores
de infraestructura siguen siendo observables.

## Parte B: bandeja persistente de Notificaciones

1. El listener recibe el `Message` AMQP crudo. El converter del contenedor entrega bytes,
   sin deserializar clases indicadas por headers ni intentar interpretar JSON antes del listener.
2. `RecepcionEventoService.registrar` inserta en `eventos_recibidos`, dentro de una transacción,
   usando `INSERT ... ON CONFLICT (event_id) DO NOTHING`.
3. El método retorna después del commit. Recién entonces AUTO puede enviar ACK. Ese ACK
   transfiere responsabilidad al trabajo durable; **no afirma que el correo esté enviado**.
4. `NotificacionesDispatcher` procesa lotes de hasta 10 trabajos. Cada llamada al procesador
   tiene su propia transacción y toma una fila PENDIENTE vencida con `FOR UPDATE SKIP LOCKED`.
5. El procesador valida el JSON y destinatario, prepara/persiste la estructura del historial
   sin marcar ENVIADO y realiza SMTP. Solo después de su retorno exitoso establece ENVIADO y
   COMPLETADO, que se confirman juntos. Un aviso in-app no llama SMTP.
6. Un fallo temporal guarda ERROR, próximo intento y código seguro en el mismo historial.
   El reintento reutiliza ese historial y el cuerpo/token original; no crea una notificación nueva.
7. Los errores definitivos conservan la fila, payload e historial cuando pudo prepararse,
   sin ejecutar más intentos automáticos. Los inválidos sin destinatario conservan evidencia
   en la bandeja aunque no puedan asociarse a un historial público.

La limpieza existente por `UsuarioEliminadoEvent` se ejecuta una sola vez por evento y sigue
usando el repositorio de historial actual. `historial_id` es una referencia interna sin FK:
la limpieza previa del historial no elimina la evidencia de deduplicación.

### Identidad y compatibilidad

- Preferencia: header `eventId`, luego `messageId` para eventos ya producidos por 3A.
- UUID se normaliza. IDs externos no UUID se representan por SHA-256 para no imprimir
  identificadores que pudieran contener enlaces, datos personales o tokens.
  Los IDs binarios se hashean por sus bytes, sin usar el `toString` de un array Java;
  headers de tipos no soportados usan messageId o la política legacy.
- Sin ambos IDs se usa `legacy:SHA-256(tipo + cuerpo exacto)`, nunca un UUID aleatorio nuevo.
  La primera recepción legacy se registra con advertencia segura; las redeliveries comparten clave.
- Dos eventos antiguos legítimos con tipo y cuerpo idénticos son indistinguibles y se consideran
  duplicados. Es una limitación explícita de compatibilidad. Los eventos nuevos con UUID distintos
  se procesan separadamente aunque tengan cuerpos idénticos.
- El ID se presupone único por evento lógico. No se sobrescribe un evento existente al repetirlo.

La PK persiste en PostgreSQL y sobrevive a reinicios. Dos receptores concurrentes no insertan
dos trabajos. Un duplicado de un pendiente se reconoce porque su recuperación ya está a cargo
de la bandeja; no se vuelve a enviar desde el listener. Un duplicado COMPLETADO o FALLIDO
tampoco reinicia efectos o intentos. No se borran claves de deduplicación automáticamente.

### Estados y recuperación

| Estado | Significado y salida |
| --- | --- |
| PENDIENTE | Recién recibido o con fallo temporal. Se selecciona solo al vencer `proximo_intento`. |
| COMPLETADO | SMTP aceptó la llamada y se confirmó su resultado, o se completó el efecto in-app/limpieza. |
| FALLIDO | Mensaje inválido, error permanente o límite de intentos. Conserva error y fecha final. |

No se guarda PROCESANDO: la exclusión es el bloqueo transaccional de la fila. Si el proceso
cae, PostgreSQL libera el bloqueo y revierte los cambios no confirmados; el pendiente vuelve a
ser elegible. No hay leases ni estados que requieran desbloqueo manual tras un reinicio.

### Política SMTP

Máximo predeterminado: **5 intentos registrados** (el inicial y hasta cuatro reintentos).
Los fallos temporales esperan 60, 120, 240 y 300 segundos. Al quinto fallo se guarda FALLIDO.
No hay `Thread.sleep` para esas pausas: los trabajos futuros se excluyen con SQL y los otros
pendientes pueden procesarse. Los timeouts de conexión, lectura y escritura SMTP son de 5 segundos.

Clasificación segura:

| Fallo | Tratamiento |
| --- | --- |
| Conexión/red, timeout, rechazo SMTP 4xx | Temporal, con límite y pausa. |
| Rechazo SMTP 5xx, dirección inválida | Definitivo. |
| Autenticación/configuración SMTP inválida | Definitivo, requiere corregir configuración. |
| JSON inválido/incompleto, mensaje o plantilla inválidos | Definitivo, sin bucle de reentrega. |
| Fallo de envío no clasificado | Temporal con el mismo límite; no se reintenta indefinidamente. |

Los códigos no incluyen el texto de excepciones del proveedor. Se conserva el último error
sanitizado, intentos, próxima fecha y fecha final, correlacionados por ID.

### ACK/NACK y caída de PostgreSQL

Si PostgreSQL no confirma la recepción, no se reconoce como exitosa. El contenedor hace
hasta tres reintentos locales, con pausas de 1, 2 y 4 segundos. Si persiste el problema,
la excepción controlada de requeue conserva el mensaje en RabbitMQ y permite recuperación
del consumidor/redelivery. No se usa el recoverer predeterminado que descarta mensajes.
No hay reencolado inmediato continuo sin pausa y no se registran cuerpos en esa excepción.
Estos reintentos cortos son de transferencia a la DB, no de SMTP.

No se agrega DLQ: los mensajes malformados también se guardan como bytes y después quedan
FALLIDO. Esto permite conservar evidencia y reconocerlos sin envenenar el resto de la cola.
Una caída tras el commit de recepción pero antes del ACK causa redelivery y deduplicación.

## Garantías y límites

- Se mantiene entrega al menos una vez entre outbox, broker y recepción persistente.
- Una entrega duplicada conocida no vuelve a ejecutar un efecto confirmado.
- SMTP y PostgreSQL no tienen una transacción atómica: si SMTP acepta y luego el proceso
  cae o falla el commit, puede enviarse un correo duplicado. No se promete exactamente una vez.
- Los 5 intentos limitan los resultados registrados. Una llamada cuyo resultado se pierde por
  rollback/caída puede repetirse y no quedar contada; no hay un límite físico exacto frente a
  caídas repetidas en esa ventana incierta.
- Aceptación SMTP no prueba entrega final al destinatario. No se implementan seguimiento de
  rebotes ni confirmaciones del proveedor.
- No se garantiza orden global entre eventos distintos o consumidores. Se conserva la lógica
  actual de limpieza de usuarios; esta etapa no incorpora coordinación con Reservas para ella.
- Tokens y vencimientos no se regeneran. Un trabajo muy atrasado puede contener un enlace ya
  vencido. No se agrega reenvío de invitaciones, verificaciones ni una pantalla nueva.
- FALLIDO requiere diagnóstico/intervención después de corregir la causa; no se reactiva por
  recibir otro duplicado. No se implementa un endpoint administrativo nuevo.
- La bandeja contiene los datos necesarios del mensaje y debe permanecer interna. No se expone
  en respuestas HTTP, y `detalleError` continúa excluido del DTO público de historial.

## PostgreSQL y migración

Solo en la base de **Notificaciones**, el script idempotente
`src/main/resources/db/manual/3B_eventos_recibidos.sql`:

- Crea `eventos_recibidos` con PK `event_id`, payload `bytea`, tipo, estado, intentos, fechas,
  último código de error e ID del historial.
- Crea `idx_recibidos_pendientes (estado, proximo_intento, creado_en)`.
- Amplía `historial_notificaciones.titulo` y `mensaje` a `text`, sin truncar datos. Evita que
  mensajes válidos fallen al persistir su historial por el antiguo límite de 255 caracteres.

No modifica el esquema de Reservas ni hace consultas cruzadas entre bases. `ddl-auto=update`
puede crear la tabla nueva, pero **se debe aplicar explícitamente el script para ampliar las
columnas existentes**: Hibernate puede considerar varchar/text equivalentes y no ampliarlas.
No se agregó Flyway ni dependencia alguna. No se aplicó sobre las bases de desarrollo.
Se probó dos veces en una base temporal con un historial anterior: la fila se conservó.

## Configuración y Docker Compose

Notificaciones agrega estos defaults en `application.properties`:

```properties
app.notificaciones.enabled=true
app.notificaciones.poll-ms=3000
app.notificaciones.batch-size=10
app.notificaciones.max-intentos=5
app.notificaciones.retry-initial-ms=60000
app.notificaciones.retry-max-ms=300000
app.notificaciones.ingest-retry-ms=1000
spring.rabbitmq.connection-timeout=3s
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000
```

Desactivar el dispatcher conserva la recepción durable, pero no procesa trabajos hasta
reactivarlo. Las credenciales existentes no cambian. No habilitar debug de Mail, cuerpos AMQP
o parámetros SQL para monitoreo cotidiano.

Compose conserva servicios, puertos y volúmenes. Expone solamente variables opcionales con
defaults compatibles: `OUTBOX_RETRY_INITIAL_MS=60000`, `OUTBOX_RETRY_MAX_MS=300000`,
`SMTP_MAX_INTENTOS=5`, `SMTP_RETRY_INITIAL_MS=60000`, `SMTP_RETRY_MAX_MS=300000`,
`SMTP_HOST=smtp.gmail.com` y `SMTP_PORT=587`. No agrega servicios ni cambia los secretos existentes.

## Diagnóstico SQL sin payloads

En la base de Notificaciones:

```sql
SELECT estado, tipo, ultimo_error, count(*)
FROM eventos_recibidos GROUP BY estado, tipo, ultimo_error;

SELECT event_id, tipo, estado, intentos, ultimo_error, creado_en,
       proximo_intento, finalizado_en, historial_id
FROM eventos_recibidos ORDER BY creado_en DESC LIMIT 30;
```

En Reservas se mantienen las consultas de `OUTBOX.md`, usando solo ID, ruta, intentos y fechas.
No seleccionar/copiar payloads a logs: pueden contener emails y tokens. Los datos para
diagnosticar fallos permanecen en la base correspondiente, sin endpoints públicos nuevos.

## Pruebas reproducibles

Desde `backend`:

```powershell
mvn.cmd -pl microservice-notificaciones '-Dtest=NotificacionSecurityTest,NotificacionListenerTest,EmailInvitacionTest,ProcesadorNotificacionTest,IdentidadEventoTest' test
mvn.cmd -pl microservice-reservas '-Dtest=OutboxPublisherTest,OutboxServiceTest' test
```

La integración de Notificaciones requiere una base local dedicada llamada
`agendapp_notificaciones_test`, usuario postgres sin contraseña, y un broker temporal en
127.0.0.1 con un puerto diferente de 5672. Nunca usar las bases o el broker de AgendApp:

```powershell
$env:AGENDAPP_TEST_NOTIFICACIONES_POSTGRES_URL = 'jdbc:postgresql://127.0.0.1:PUERTO_PG/agendapp_notificaciones_test'
$env:AGENDAPP_TEST_RABBIT_PORT = 'PUERTO_RABBIT'
mvn.cmd -pl microservice-notificaciones '-Dtest=ConfiabilidadPostgresTest' '-DargLine=-Duser.timezone=UTC' package
```

El test usa PostgreSQL real, recrea el contexto Spring manteniendo la DB y prueba dos receptores
y dos workers concurrentes, rollback, máximo de intentos y selección por fecha. Con RabbitMQ
real usa la fábrica de consumidores y comprueba redelivery, ACK después del commit, NACK/
recuperación ante fallo, reinicio del contenedor, ID estable, JSON malformado y pérdida de ACK.
El fallo inicial de persistencia del caso Rabbit se simula antes del acceso a DB; las transacciones
y restricciones se comprueban en los otros casos con PostgreSQL real.

`ServidorSmtpPrueba` es un servidor local incluido solo en tests. No reenvía ni guarda correos;
controla respuestas 250/451/550, timeout y conexión rechazada. Se usan EmailService y las plantillas
reales para probar envío, recuperación, rechazo permanente, plantilla inexistente y la ventana de
duplicado tras SMTP aceptado y rollback. Las pruebas unitarias con mocks no demuestran infraestructura.

La regresión de Reservas usa las variables de 3A para bases de pruebas separadas y Rabbit temporal:

```powershell
$env:AGENDAPP_TEST_OUTBOX_POSTGRES_URL = 'jdbc:postgresql://127.0.0.1:PUERTO_PG/agendapp_outbox_test'
$env:AGENDAPP_TEST_TURNOS_POSTGRES_URL = 'jdbc:postgresql://127.0.0.1:PUERTO_PG/agendapp_turnos_test'
$env:AGENDAPP_TEST_POSTGRES_URL = 'jdbc:postgresql://127.0.0.1:PUERTO_PG/agendapp_invitaciones_test'
mvn.cmd -pl microservice-reservas '-Dtest=ReservasSecurityTest,TurnoServiceTest,DisponibilidadServiceTest,JornadaUtilTest,RecursoHorarioTest,OutboxPublisherTest,OutboxServiceTest,OutboxPostgresTest,TurnoPostgresTest,InvitacionPostgresTest' '-DargLine=-Duser.timezone=GMT-03:00' package
```

Sin variables, las respectivas integraciones se omiten. Los `contextLoads` generales y pruebas
de navegador no se incluyen: usan la configuración/infraestructura de la aplicación.

## Validación manual exacta desde AgendApp

1. Aplicar el script aditivo **en Notificaciones** y actualizar solo los módulos afectados:

   ```powershell
   Get-Content -Raw backend/microservice-notificaciones/src/main/resources/db/manual/3B_eventos_recibidos.sql | docker exec -i base-datos-notificaciones psql -U postgres -d notificaciones_db -v ON_ERROR_STOP=1
   docker compose -f backend/docker-compose.yml up -d --build reservas-app notificaciones-app
   ```

   Estos comandos se proporcionan para desarrollo: no se ejecutaron contra AgendApp durante la tarea.

2. Crear una reserva propia. Comprobar outbox publicado, bandeja COMPLETADO y una única fila del
   historial. Repetir con cancelación, invitación y verificación de cuenta desde sus flujos actuales.
   Los enlaces de invitación conservan POST, token original y vencimiento de siete días.

3. Detener solo RabbitMQ con `docker compose -f backend/docker-compose.yml stop rabbitmq`.
   Crear otra reserva: debe confirmarse. Revisar `proximo_intento` en Reservas: 60, 120, 240 y
   luego 300 segundos tras fallos sucesivos. Otros eventos nuevos siguen siendo elegibles.
   Restablecer con `docker compose -f backend/docker-compose.yml start rabbitmq`; esperar la fecha
   programada más el ciclo de consulta y comprobar recepción sin repetir la reserva.

4. Para simular SMTP no disponible sin cambiar credenciales ni enviar a servidores de terceros:

   ```powershell
   $smtpHostAnterior = $env:SMTP_HOST
   $env:SMTP_HOST = 'smtp-no-disponible.invalid'
   docker compose -f backend/docker-compose.yml up -d --force-recreate notificaciones-app
   ```

   Crear una reserva dirigida a una cuenta de prueba propia. El evento debe recibirse/ACK y quedar
   PENDIENTE con error temporal; RabbitMQ no debe redeliverlo por el fallo SMTP. El historial pasa
   a ERROR y la próxima ejecución queda programada. No debe haber un envío inmediato en bucle.

5. Restaurar SMTP antes de agotar los intentos:

   ```powershell
   $env:SMTP_HOST = $smtpHostAnterior
   docker compose -f backend/docker-compose.yml up -d --force-recreate notificaciones-app
   ```

   Esperar `proximo_intento`. Debe pasar a COMPLETADO/ENVIADO reutilizando el mismo ID e historial.
   Si se agotaron cinco fallos, queda FALLIDO y no se reactiva automáticamente al restablecer SMTP.

6. Reiniciar Notificaciones con `docker compose -f backend/docker-compose.yml restart notificaciones-app`
   mientras haya trabajos pendientes. Deben seguir en la DB y recuperarse al vencer su fecha.
   No hay estado PROCESANDO que desbloquear ni generación de nuevos tokens.

7. Para duplicados, mensajes malformados y fallos permanentes, ejecutar la integración controlada:
   no hace falta publicar cuerpos con datos personales ni editar registros productivos manualmente.
   Las pruebas de 451/550 y plantilla inexistente verifican la evidencia FALLIDO sin correos reales.


## Resultado de esta implementación

Validación ejecutada el 7 de noviembre de 2026:

| Módulo | Pruebas aprobadas | Fallos / errores / omitidas |
| --- | --- | --- |
| Reservas | 238 | 0 / 0 / 0 |
| Notificaciones | 71 | 0 / 0 / 0 |
| Total, sin contar reejecuciones | 309 | 0 / 0 / 0 |

Reservas ejecutó la selección completa indicada arriba y terminó con BUILD SUCCESS.
Notificaciones ejecutó 70 pruebas en conjunto con BUILD SUCCESS y, después del último ajuste
de normalización de IDs binarios, las cuatro pruebas de IdentidadEventoTest y el empaquetado
volvieron a aprobar; el conjunto final contiene 71 casos distintos. Ambos módulos compilaron
y generaron sus JAR ejecutables.

Las 38 pruebas PostgreSQL de Reservas y las 14 de Notificaciones usaron bases temporales reales.
OutboxPostgresTest y dos casos de ConfiabilidadPostgresTest también utilizaron RabbitMQ real.
Las pruebas SMTP de infraestructura emplearon un servidor local controlado (250, 451, 550,
timeout y recuperación), sin enviar correos externos. Otras pruebas de lógica usan mocks:
no se presentan como evidencia de comportamiento del broker o PostgreSQL.

El script SQL se ejecutó dos veces en una base temporal separada con un historial previo,
conservando la fila y ampliando ambas columnas a text. Docker Compose pasó config --quiet.
No se ejecutaron pruebas de navegador ni contextLoads generales, ni se inició la aplicación
contra las bases de desarrollo. No hubo cambios en frontend y no correspondía compilarlo.

Los contenedores de pruebas fueron temporales y separados de los servicios de desarrollo.
Se verificó que el índice Git y los archivos preexistentes ajenos a esta etapa se conservaran.
No se agregaron dependencias ni se hicieron commits o pushes.

## Archivos de esta etapa

El siguiente inventario distingue esta implementación de los cambios de etapas previas
que ya existían sin commit en el workspace:

```text
backend/docker-compose.yml
backend/microservice-notificaciones/CONFIABILIDAD.md
backend/microservice-notificaciones/src/main/java/com/microservice/notificaciones/config/RabbitMQConfig.java
backend/microservice-notificaciones/src/main/java/com/microservice/notificaciones/entity/EventoRecibido.java
backend/microservice-notificaciones/src/main/java/com/microservice/notificaciones/entity/HistorialNotificacion.java
backend/microservice-notificaciones/src/main/java/com/microservice/notificaciones/enums/EstadoProcesamiento.java
backend/microservice-notificaciones/src/main/java/com/microservice/notificaciones/enums/TipoEventoRecibido.java
backend/microservice-notificaciones/src/main/java/com/microservice/notificaciones/listener/NotificacionListener.java
backend/microservice-notificaciones/src/main/java/com/microservice/notificaciones/repository/EventoRecibidoRepository.java
backend/microservice-notificaciones/src/main/java/com/microservice/notificaciones/service/EmailService.java
backend/microservice-notificaciones/src/main/java/com/microservice/notificaciones/service/FalloEnvio.java
backend/microservice-notificaciones/src/main/java/com/microservice/notificaciones/service/IdentidadEvento.java
backend/microservice-notificaciones/src/main/java/com/microservice/notificaciones/service/NotificacionesDispatcher.java
backend/microservice-notificaciones/src/main/java/com/microservice/notificaciones/service/ProcesadorNotificacion.java
backend/microservice-notificaciones/src/main/java/com/microservice/notificaciones/service/RecepcionEventoService.java
backend/microservice-notificaciones/src/main/resources/application.properties
backend/microservice-notificaciones/src/main/resources/db/manual/3B_eventos_recibidos.sql
backend/microservice-notificaciones/src/test/java/com/microservice/notificaciones/listener/NotificacionListenerTest.java
backend/microservice-notificaciones/src/test/java/com/microservice/notificaciones/service/ConfiabilidadPostgresTest.java
backend/microservice-notificaciones/src/test/java/com/microservice/notificaciones/service/IdentidadEventoTest.java
backend/microservice-notificaciones/src/test/java/com/microservice/notificaciones/service/ProcesadorNotificacionTest.java
backend/microservice-notificaciones/src/test/java/com/microservice/notificaciones/service/ServidorSmtpPrueba.java
backend/microservice-reservas/OUTBOX.md
backend/microservice-reservas/src/main/java/com/microservice/reservas/service/OutboxPublisher.java
backend/microservice-reservas/src/main/resources/application.properties
backend/microservice-reservas/src/test/java/com/microservice/reservas/service/OutboxPostgresTest.java
backend/microservice-reservas/src/test/java/com/microservice/reservas/service/OutboxPublisherTest.java
```
