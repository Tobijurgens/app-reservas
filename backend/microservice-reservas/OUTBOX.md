# Etapa 3A: outbox transaccional

## Diagnóstico del flujo anterior

Los cuatro productores (`TurnoService`, `RecursoService`, `AuthService` y `UsuarioService`)
llamaban a `RabbitTemplate.convertAndSend` dentro de la transacción de PostgreSQL, antes
del commit. El template no tenía publisher confirms ni una coordinación atómica con la DB:

- Un error síncrono de conexión podía provocar rollback de la operación de negocio.
- Que `convertAndSend` retornara no demostraba enrutamiento ni aceptación persistente del broker.
- RabbitMQ podía aceptar un evento y PostgreSQL fallar después, dejando un correo sobre una
  operación revertida. Dos o tres envíos dentro de una operación también podían quedar parciales.
- Mover el envío a después del commit eliminaría el rollback por RabbitMQ, pero dejaría una
  ventana de pérdida si el proceso cae entre el commit y el envío, sin registro para recuperar.
- El fallo SMTP ocurre en Notificaciones, de forma asíncrona. Allí se captura y se guarda
  como ERROR; nunca hubo una transacción distribuida hasta el envío del correo.

Clasificación de eventos existentes:

| Evento | Responsabilidad |
| --- | --- |
| InvitacionEmpleadoEvent | Funcional: entrega el enlace necesario para decidir sobre la asociación. |
| UsuarioRegistradoEvent | Funcional: entrega el enlace de verificación de la cuenta. |
| TurnoEvent, confirmación/cancelación | Informa una reserva ya decidida por Reservas; el correo no autoriza ni confirma la DB. |
| AvisoInternoEvent | Informativo para el historial/campanita. |
| UsuarioEliminadoEvent | Solicita la limpieza del historial de la cuenta en Notificaciones. |

Se usa el mismo mecanismo para todos, sustituyendo solo la publicación. No se agregan eventos
a operaciones que antes no los emitían, ni se cambian reglas, permisos, tokens o endpoints.

## Funcionamiento

1. El productor llama `OutboxService.encolar` dentro de su transacción existente. El método
   exige una transacción (`MANDATORY`), usa el converter JSON existente y guarda payload,
   exchange, routing key, tipo e identificador UUID en `eventos_outbox`. No contacta RabbitMQ.
2. El commit guarda conjuntamente negocio y eventos. Si la transacción falla, no queda
   ninguno de sus eventos. Un evento sin commit no es visible para el publicador.
3. `OutboxDispatcher`, en un hilo programado separado de las peticiones HTTP, consulta lotes
   acotados. No es un microservicio ni un proceso externo nuevo.
4. Cada llamada a `OutboxPublisher.publicarSiguiente` abre su propia transacción `REQUIRES_NEW`.
   Toma una sola fila pendiente y vencida con `FOR UPDATE SKIP LOCKED`. Otras instancias pueden
   procesar filas distintas, sin publicar concurrentemente la misma fila.
5. Envía un mensaje persistente con el JSON y `__TypeId__` originales. `messageId` y el header
   `eventId` contienen el UUID estable del evento. La correlación de confirms es única por intento, para que un ACK
   tardío no se confunda con el ACK de un reintento.
6. Espera el publisher confirm correlacionado. Solo un ACK **sin devolución mandatory** permite
   establecer `publicado_en`. Un ACK con `basic.return` por falta de ruta sigue pendiente.
   Spring AMQP informa ese return antes de completar el future del confirm.
7. Error de conexión, NACK, return, timeout o interrupción conservan el evento pendiente.
   Se registra el intento, un código de error y el próximo intento con backoff exponencial.
   El registro del negocio ya confirmado no participa de esta transacción.

No se reemiten tokens ni se extienden vencimientos al reintentar. La invitación permanece
PENDIENTE y conserva el token original y sus siete días. Los payloads y routing keys de
Notificaciones no cambian; el identificador se agrega en las propiedades AMQP, no en el JSON.

## Garantías y límites reales

- La intención de publicar es atómica con la operación de negocio, mientras PostgreSQL pueda
  confirmar. Si no es posible serializar/guardar el evento, la operación debe fallar: confirmar
  sin una intención durable volvería a permitir pérdida silenciosa. El error de serialización
  se registra con metadatos seguros y no expone el payload en la respuesta.
- Una caída de RabbitMQ o un error SMTP no deshace una reserva o asignación ya confirmada.
- Hay entrega **al menos una vez** al broker con una ruta válida, condicionada a que se
  restablezcan DB/broker/topología y continúe funcionando el publicador. Los pendientes no se
  descartan por alcanzar un número de intentos.
- Si el broker aceptó el mensaje pero se perdió el ACK, o el proceso/commit de DB falla después
  del ACK, se reintentará y puede haber un duplicado. No se promete exactamente una vez.
- `publicado_en` acredita ACK y ausencia de return, no procesamiento por Notificaciones ni
  entrega SMTP. Las colas actuales deben conservar su configuración durable.
- La etapa 3B agregó deduplicación y trabajos SMTP persistentes en Notificaciones. Los fallos
  SMTP se reintentan allí, sin volver a publicar desde el outbox. Subsiste la ventana de duplicado
  si SMTP acepta y el resultado no alcanza a confirmarse en PostgreSQL; no hay exactamente una vez.
- No se garantiza orden estricto entre tipos, colas, réplicas o reintentos. Un evento de un
  hecho confirmado puede llegar después de otro hecho posterior. No es un sistema de snapshots.
- Una caída prolongada puede hacer llegar un correo con token ya vencido. No se renueva
  automáticamente ni se modifica el flujo de aceptación/rechazo o de verificación.
- El bloqueo se mantiene durante el envío y espera de confirmación, pero solo sobre una fila
  del outbox, nunca sobre una reserva/recurso en esa transacción de publicación.
- Esta etapa no reconstruye eventos históricos ni borra filas publicadas. Su retención debe
  definirse posteriormente; el payload contiene los datos necesarios para los correos, incluidos
  tokens. La tabla permanece interna, sin endpoint público ni logs de payloads.

## PostgreSQL y despliegue

Se agrega únicamente `eventos_outbox` y su índice `idx_outbox_pendientes`.
El script aditivo e idempotente está en `src/main/resources/db/manual/3A_outbox.sql`.
Incluye UUID, exchange/routing key/tipo, JSON en text, intentos, error sanitizado y timestamps
técnicos con zona horaria (Instant). No modifica tablas existentes ni fechas de negocio.

Si se administra el esquema manualmente, aplicar ese script antes de desplegar Reservas.
La configuración existente `spring.jpa.hibernate.ddl-auto=update` también crea la tabla al iniciar.
No se agregó Flyway ni otra dependencia. El script no se ejecuta automáticamente por su ubicación.
Durante el desarrollo solo se aplicó en una base temporal vacía, dos veces para verificar idempotencia;
no se aplicó sobre `turnos_db` ni sobre los volúmenes de AgendApp.

## Configuración

Se mantienen host, puerto y credenciales existentes. Se habilitan:

```properties
spring.rabbitmq.publisher-confirm-type=correlated
spring.rabbitmq.publisher-returns=true
spring.rabbitmq.template.mandatory=true
spring.rabbitmq.connection-timeout=3s

app.outbox.enabled=true
app.outbox.poll-ms=3000
app.outbox.batch-size=10
app.outbox.confirm-timeout-ms=5000
app.outbox.retry-initial-ms=60000
app.outbox.retry-max-ms=300000
```

El delay de 3 segundos corre después de terminar el ciclo anterior. Cada ciclo intenta hasta
10 filas (configurable entre 1 y 100). Desde 3B los fallos esperan 60, 120, 240 y luego 300 segundos,
sin límite de cantidad. Los eventos nuevos permanecen elegibles inmediatamente. Los tiempos de confirmación y reintento deben
ser positivos. No deshabilitar confirms, returns o mandatory al operar el outbox.

`app.outbox.enabled=false` detiene el dispatcher; las operaciones siguen guardando pendientes.
No elimina ni confirma eventos. Puede ser útil para una ventana de mantenimiento.

## Diagnóstico sin exponer secretos

Los logs muestran UUID, ruta, intento, confirmación, código de error y próximo intento.
Errores: `PUBLICACION_FALLIDA`, `TIMEOUT_CONFIRMACION`, `NACK`, `SIN_RUTA`, `INTERRUMPIDO`.
La serialización registra clase y ruta, sin objeto, mensaje de excepción ni stack con datos.
No habilitar logging de cuerpos AMQP, parámetros SQL ni payloads en producción.

Consultas operativas seguras:

```sql
SELECT routing_key, ultimo_error, count(*) AS pendientes,
       min(creado_en) AS mas_antiguo, min(proximo_intento) AS proximo
FROM eventos_outbox
WHERE publicado_en IS NULL
GROUP BY routing_key, ultimo_error;

SELECT id, routing_key, intentos, ultimo_error, creado_en, proximo_intento, publicado_en
FROM eventos_outbox
ORDER BY creado_en DESC
LIMIT 30;
```

No seleccionar ni copiar `payload` para monitoreo cotidiano: puede contener tokens y emails.

## Pruebas

Desde `backend`, las pruebas sin infraestructura:

```powershell
mvn.cmd -pl microservice-reservas '-Dtest=ReservasSecurityTest,TurnoServiceTest,DisponibilidadServiceTest,JornadaUtilTest,RecursoHorarioTest,OutboxPublisherTest,OutboxServiceTest' test
mvn.cmd -pl microservice-notificaciones '-Dtest=NotificacionSecurityTest,NotificacionListenerTest,EmailInvitacionTest' test
```

Las pruebas previas de servicios/HTTP ahora simulan el registro del outbox, no una entrega a
RabbitMQ. Los tests unitarios del publicador simulan ACK/NACK/return/timeout y no prueban el broker.

La integración usa PostgreSQL 16 y RabbitMQ 3 reales en contenedores temporales distintos
de los de la aplicación, con puertos efímeros ligados a `127.0.0.1`. No usa los volúmenes de AgendApp.
`OutboxPostgresTest` exige la variable `AGENDAPP_TEST_OUTBOX_POSTGRES_URL`, con la base local
`agendapp_outbox_test`. Su caso de broker real exige además `AGENDAPP_TEST_RABBIT_PORT`, distinto
de 5672. Las regresiones requieren sus bases de pruebas de 1D/2A/2B:

```powershell
$env:AGENDAPP_TEST_OUTBOX_POSTGRES_URL = 'jdbc:postgresql://127.0.0.1:PUERTO_PG/agendapp_outbox_test'
$env:AGENDAPP_TEST_TURNOS_POSTGRES_URL = 'jdbc:postgresql://127.0.0.1:PUERTO_PG/agendapp_turnos_test'
$env:AGENDAPP_TEST_POSTGRES_URL = 'jdbc:postgresql://127.0.0.1:PUERTO_PG/agendapp_invitaciones_test'
$env:AGENDAPP_TEST_RABBIT_PORT = 'PUERTO_RABBIT'
mvn.cmd -pl microservice-reservas '-Dtest=OutboxPostgresTest,TurnoPostgresTest,InvitacionPostgresTest' '-DargLine=-Duser.timezone=GMT-03:00' package
```

Sin esas variables, las respectivas integraciones se omiten. Los tests generales `contextLoads`
no se incluyen porque utilizan configuración e infraestructura de la aplicación.

La integración comprueba commit y rollback conjuntos, invisibilidad previa al commit, exclusión
real de dos publicadores mediante SKIP LOCKED, recuperación de cancelaciones e invitaciones sin
regenerar tokens y persistencia de intentos. El caso RabbitMQ real verifica ACK, mensaje persistente,
return por falta de ruta y recuperación después de una conexión realmente rechazada. La caída se
simula temporalmente con un puerto cerrado y luego se restablece el endpoint del broker temporal;
no se detiene el RabbitMQ de AgendApp. No se envían emails reales.

Resultados ejecutados en esta etapa: 194 pruebas unitarias/HTTP de Reservas, 32 pruebas de
integración con PostgreSQL (8 outbox, 22 reservas/disponibilidad, 2 invitaciones) y 30 de regresión
de Notificaciones: 256 pruebas exitosas, sin omisiones en las suites seleccionadas.
Uno de los 8 casos del outbox incluyó RabbitMQ real, confirms, devolución y recuperación de conexión.
Reservas compiló y se empaquetó correctamente. Ambos contenedores temporales se detuvieron al finalizar.
No se modificó el frontend ni fue necesario volver a ejecutar su build.

Archivos de esta etapa, relativos a `backend/microservice-reservas`:

```text
OUTBOX.md
src/main/java/com/microservice/reservas/config/RabbitMQConfig.java
src/main/java/com/microservice/reservas/entity/EventoOutbox.java
src/main/java/com/microservice/reservas/repository/EventoOutboxRepository.java
src/main/java/com/microservice/reservas/security/AuthService.java
src/main/java/com/microservice/reservas/service/OutboxDispatcher.java
src/main/java/com/microservice/reservas/service/OutboxPublisher.java
src/main/java/com/microservice/reservas/service/OutboxService.java
src/main/java/com/microservice/reservas/service/RecursoService.java
src/main/java/com/microservice/reservas/service/TurnoService.java
src/main/java/com/microservice/reservas/service/UsuarioService.java
src/main/resources/application.properties
src/main/resources/db/manual/3A_outbox.sql
src/test/java/com/microservice/reservas/security/ReservasSecurityTest.java
src/test/java/com/microservice/reservas/service/DisponibilidadServiceTest.java
src/test/java/com/microservice/reservas/service/InvitacionPostgresTest.java
src/test/java/com/microservice/reservas/service/OutboxPostgresTest.java
src/test/java/com/microservice/reservas/service/OutboxPublisherTest.java
src/test/java/com/microservice/reservas/service/OutboxServiceTest.java
src/test/java/com/microservice/reservas/service/RecursoHorarioTest.java
src/test/java/com/microservice/reservas/service/TurnoPostgresTest.java
src/test/java/com/microservice/reservas/service/TurnoServiceTest.java
```

## Comprobación manual de recuperación

En el entorno de desarrollo, con el esquema y Reservas actualizados:

1. Crear una reserva con RabbitMQ operativo. Debe confirmarse y los eventos pasar de pendientes
   a publicados después del ACK. Comprobar recepción normal en Notificaciones.
2. Detener solo el broker: `docker compose -f backend/docker-compose.yml stop rabbitmq`.
3. Crear otra reserva, cancelar una propia y enviar una invitación desde el flujo existente.
   Las operaciones deben confirmarse. La invitación debe permanecer PENDIENTE y conservar sus
   siete días; el outbox debe registrar intentos/error sin perder sus filas.
4. Restablecerlo: `docker compose -f backend/docker-compose.yml start rabbitmq`.
5. Esperar el próximo intento (60 segundos inicialmente, hasta 5 minutos después de varios fallos,
   más el tiempo de ciclo). Los eventos deben publicarse sin repetir las operaciones de negocio.
6. Comprobar `publicado_en`, logs de ACK y la recepción de notificaciones. La invitación debe
   seguir usando el token original. No renovar manualmente la asignación para probar el retry.

El frontend y Notificaciones no requieren cambios para este contrato. La prueba manual de
React/SMTP del sistema completo queda a cargo del entorno de desarrollo; aquí se verificaron
broker y base temporales, además de las regresiones automatizadas existentes.
