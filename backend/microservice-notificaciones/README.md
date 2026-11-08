# Seguridad HTTP de Notificaciones

Todos los endpoints HTTP requieren `Authorization: Bearer <JWT>` incluso al acceder
directamente al puerto 8082. El JWT debe estar firmado con la misma clave HMAC que
usa Reservas, no estar vencido y contener `sub` (email) y `exp`.

Configurar `JWT_SECRET_KEY` con el mismo valor en ambos servicios. Docker Compose
ya pasa esa variable a los dos contenedores. Para ejecución desde el IDE, también
debe estar presente en el entorno de Notificaciones. No hay clave por defecto.
Se conserva la representación de la clave como texto, sin decodificación Base64.

`X-User-Email` se ignora. El historial y el contador se filtran por la identidad
autenticada. Marcar una notificación consulta por ID y destinatario juntos;
devuelve 404 si no pertenece al usuario o no existe, sin revelar cuál es el caso.
Marcar todas afecta únicamente al destinatario autenticado.

El DTO del historial conserva los campos que usa la campanita, pero excluye
`detalleError` y `emailDestinatario`. RabbitMQ, SMTP y las entidades no cambian.
Esta validación es local: no consulta el estado actual de la cuenta en Reservas
ni implementa revocación de JWT, que pertenecen a otra corrección.

## Pruebas automatizadas

Desde `backend`, con Java 17 y Maven:

```powershell
mvn.cmd -pl microservice-notificaciones -am test "-Dtest=NotificacionSecurityTest,NotificacionListenerTest" "-Dsurefire.failIfNoSpecifiedTests=false"
mvn.cmd -pl microservice-notificaciones -am package -DskipTests
```

Las pruebas indicadas no usan PostgreSQL, RabbitMQ ni SMTP reales. El test
`contextLoads` preexistente requiere la infraestructura y configuración del
entorno; no forma parte de este comando aislado.

## Verificación manual

Usar dos cuentas verificadas A y B con notificaciones existentes. Obtener sus JWT
mediante el login habitual y cargarlos localmente como `TOKEN_A` y `TOKEN_B`, sin
compartirlos ni versionarlos. Sustituir `<ID_DE_B>` por una notificación de B.

```powershell
# Sin JWT: 401, aunque se envíe un correo.
curl.exe -i http://localhost:8082/api/notificaciones -H "X-User-Email: b@example.com"

# Token inválido: 401.
curl.exe -i http://localhost:8082/api/notificaciones -H "Authorization: Bearer invalido"

# Con token de A: solo historial de A, aunque se envíe correo de B.
curl.exe -i http://localhost:8082/api/notificaciones -H "Authorization: Bearer $env:TOKEN_A" -H "X-User-Email: b@example.com"

# Solo cuenta las no leídas de A.
curl.exe -i http://localhost:8082/api/notificaciones/no-leidas/count -H "Authorization: Bearer $env:TOKEN_A"

# Una notificación de B con token de A: 404 y B permanece sin cambios.
curl.exe -i -X PUT "http://localhost:8082/api/notificaciones/<ID_DE_B>/leer" -H "Authorization: Bearer $env:TOKEN_A" -H "X-User-Email: b@example.com"

# Marca todas las de A; no modifica B.
curl.exe -i -X PUT http://localhost:8082/api/notificaciones/leer-todas -H "Authorization: Bearer $env:TOKEN_A"
```

Repetir contra el Gateway cambiando 8082 por 8080. Con B, comprobar que su
historial y contador no cambiaron. Un JWT vencido debe recibir 401. En React,
verificar carga de la campanita, contador, lectura individual y lectura de todas;
en la pestaña Network debe enviarse `Authorization`, sin `X-User-Email`.
