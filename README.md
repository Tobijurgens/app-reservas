# AgendApp - API de Gestión de Reservas Multitenant

AgendApp es un sistema backend orientado a la gestión de turnos para múltiples negocios simultáneos (multitenancy). El proyecto aplica principios de diseño guiado por el dominio y arquitectura de microservicios para resolver la lógica de concurrencia y disponibilidad de recursos físicos o humanos.

La problemática central que resuelve el sistema abstrae escenarios reales (como clínicas, talleres o centros de estética) donde se requiere cruzar dinámicamente los horarios de apertura de un establecimiento con las franjas de trabajo individuales de sus recursos, previniendo solapamientos de manera transaccional.

## Arquitectura del Sistema

El sistema está dividido en servicios independientes para aislar responsabilidades críticas:

* **API Gateway (Spring Cloud Gateway):** Único punto de entrada a la red interna. Centraliza el enrutamiento y la resolución de políticas CORS.
* **Core de Reservas:** Monolito modular que expone la API RESTful principal. Gestiona la autenticación (JWT stateless) y la transaccionalidad de las entidades de negocio.
* **Servicio de Notificaciones:** Consumidor AMQP. Se encarga del procesamiento asíncrono y despacho de correos electrónicos (confirmaciones, cancelaciones, invitaciones) para evitar bloqueos en el hilo principal de peticiones HTTP.

## Lógica del Dominio y Algoritmo de Disponibilidad

En lugar de utilizar una cuadrícula de turnos estática, el sistema calcula la disponibilidad al vuelo. El algoritmo:
1. Extrae los rangos operativos del negocio (apertura/cierre y tiempos de descanso).
2. Extrae los rangos operativos del recurso solicitado.
3. Calcula la intersección de ambas franjas horarias.
4. Consulta a la base de datos los turnos con estado `CONFIRMADO` en dicha franja.
5. Devuelve al cliente únicamente los bloques de tiempo contiguos que soportan la duración (en minutos) del servicio requerido, validando concurrencia a nivel de base de datos para prevenir *race conditions*.

## Stack Tecnológico

* **Lenguaje y Framework:** Java 17, Spring Boot 3
* **Persistencia:** PostgreSQL, Spring Data JPA, Hibernate
* **Seguridad:** Spring Security, JSON Web Tokens (JWT)
* **Mensajería Asíncrona:** RabbitMQ, Spring AMQP
* **Infraestructura:** Docker, Docker Compose
* **Testing:** JUnit 5, Mockito

## Ejecución Local

El proyecto incluye la configuración necesaria para levantar toda la infraestructura y las aplicaciones mediante contenedores.

1. Clonar el repositorio.
2. Generar un archivo `.env` en la raíz del proyecto para inyectar las credenciales SMTP del servicio de notificaciones:
   ```env
   MAIL_USERNAME=tu_correo@gmail.com
   MAIL_PASSWORD=tu_clave_de_aplicacion
3. Ejecutar el orquestador:
   `docker-compose up -d --build`
4. Acceder a la documentación interactiva (Swagger) para probar los endpoints en:
   `http://localhost:8081/swagger-ui/index.html`