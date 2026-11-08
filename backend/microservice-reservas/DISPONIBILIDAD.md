# Etapa 2B: disponibilidad y jornadas nocturnas

## Problemas corregidos y reglas conservadas

La consulta usaba únicamente los horarios del recurso; la creación también validaba el negocio.
Esto ofrecía horarios fuera de la apertura o dentro de un descanso del negocio. Además, recortar
el fin del bloque a `23:59:59.999...` eliminaba reservas que terminaban exactamente a medianoche
o después. El cálculo actual conserva intervalos completos y filtra solo por la fecha de inicio.

La comparación de horas aisladas al guardar recursos permitía casos como un recurso 22:30–01:30
en un negocio 09:00–18:00 y rechazaba madrugadas válidas del negocio del día anterior. Ahora
compara intervalos con fechas. Al guardar semanas se validan jornadas y descansos activos,
incluidos descansos incompletos o fuera de jornada; no se modifica información existente.

Ya eran correctos y se conservan: intervalos semiabiertos, consultas de solapamiento de 2A,
duración calculada por el backend, validación de fechas futuras y estados activos,
`READ_COMMITTED`, bloqueo pesimista del recurso y prohibición de reconfirmar cancelados.
No se modificaron los repositorios ni sus consultas: se corrigió el rango temporal consultado
para cubrir también el final del bloque después de medianoche.

## Reglas compartidas

`JornadaUtil` convierte la jornada y su descanso en intervalos absolutos. `TurnoService`
utiliza una misma función para intersectar bloques laborables de recurso y negocio, tanto
al consultar disponibilidad como al validar una creación bajo el bloqueo de 2A.

- El día semanal es el día calendario **de inicio** de la jornada.
- Si el cierre es menor que la apertura, el cierre pertenece al día siguiente.
- Los extremos de descanso anteriores a la apertura se ubican al día siguiente.
- Un descanso debe tener ambos extremos, duración positiva y estar dentro de la jornada.
- Se incluyen jornadas iniciadas el día anterior, aunque el día consultado esté cerrado.
- El intervalo completo del servicio debe entrar en la intersección de un bloque del negocio
  y uno del recurso. Ambos descansos se descuentan.
- Finalizar al comienzo de un descanso o comenzar a su final es válido.
- La consulta del día D devuelve solo inicios en D, aunque su fin pertenezca a D+1.
- Se conserva el avance por duración del servicio dentro de cada bloque, y desde el final
  de las ocupaciones existentes. La medianoche no reinicia artificialmente esa secuencia.
- Los inicios se ordenan y deduplican por fecha y hora completas. Una consulta de ocupaciones
  abarca todos los bloques del día, incluyendo los extremos de jornadas nocturnas.

Ejemplo: negocio viernes 22:00–02:00 y recurso viernes 22:30–01:30 permiten una reserva
viernes 23:45–sábado 00:15. Esa reserva es una sola fila con inicio y fin en fechas diferentes.
La generación de opciones conserva la secuencia por duración: 23:45 se ofrece si es un inicio
de esa secuencia, por ejemplo después de una ocupación que termina a las 23:45. Un inicio
válido enviado directamente a la API también se valida contra los mismos bloques.

La disponibilidad es una lectura, no una retención del horario. Una nueva ocupación posterior
a la consulta sigue causando HTTP 409 al confirmar y el modal conserva el manejo de 2A.

## Fecha y zona horaria

No cambia la convención existente del servicio: horarios locales del negocio en Argentina,
`LocalDate` para días, `LocalTime` para configuración semanal y `LocalDateTime` para reservas.
PostgreSQL mantiene los timestamps locales sin zona horaria del modelo existente.
El servidor Reservas conserva su zona GMT-03:00; no se modificó configuración global.

React envía el día elegido como `yyyy-MM-dd` a partir de sus componentes de calendario,
sin convertirlo a UTC con `toISOString`. Para la fecha mínima y el mes inicial usa la fecha
de Buenos Aires mediante `Intl`. Los valores `fechaHoraISO` recibidos del backend se envían
sin agregar `Z`, offset ni conversiones de zona. El filtro local por día activo se eliminó:
los días llenos del backend contemplan la madrugada que queda de la jornada anterior.
Las respuestas de consultas anteriores se descartan si el usuario cambió la fecha o cerró
el modal. La etiqueta de madrugada ya no atribuye todos los horarios antes de las 08:00
a la noche anterior.

## Modelo y límites

No hay cambios de esquema ni dependencias nuevas. El modelo existente representa una jornada
continua de menos de 24 horas y un descanso por día, para negocio y recurso. Inicio igual a fin
se rechaza: no se interpreta implícitamente como atención de 24 horas. Tampoco se agregan
horarios partidos, múltiples descansos, feriados, excepciones por fecha ni zonas por negocio.
No se fusionan jornadas semanales distintas para habilitar reservas que atraviesen sus límites;
el intervalo completo debe caber en los bloques configurados. Una configuración antigua inválida
puede devolver 400 y debe corregirse desde la edición existente; no se reescriben sus filas.

## Pruebas reproducibles

Desde `backend`:

```powershell
mvn.cmd -pl microservice-reservas '-Dtest=ReservasSecurityTest,TurnoServiceTest,DisponibilidadServiceTest,JornadaUtilTest,RecursoHorarioTest' test
```

Se agregaron pruebas diurnas, nocturnas, intersecciones, descansos de ambos tipos de jornada,
límites exactos, duración insuficiente, ocupaciones a ambos lados de medianoche y paridad:
cada horario ofrecido por los escenarios pasa la validación de creación con los mismos datos.
Estas pruebas unitarias simulan los repositorios y no demuestran bloqueos.

Para las consultas, persistencia y concurrencia, `TurnoPostgresTest` exige PostgreSQL real,
local y dedicado a pruebas. Incluye los 19 casos de 2A y 3 nuevos casos nocturnos.
Nunca utilizar la base `turnos_db` de la aplicación:

```powershell
docker run --detach --rm --name agendapp-disponibilidad-test --publish 127.0.0.1::5432 --env POSTGRES_HOST_AUTH_METHOD=trust --env POSTGRES_DB=agendapp_turnos_test --tmpfs /var/lib/postgresql/data postgres:16
docker port agendapp-disponibilidad-test 5432
# Reemplazar PUERTO por el puerto informado:
$env:AGENDAPP_TEST_TURNOS_POSTGRES_URL = 'jdbc:postgresql://127.0.0.1:PUERTO/agendapp_turnos_test'
mvn.cmd -pl microservice-reservas '-Dtest=TurnoPostgresTest' '-DargLine=-Duser.timezone=GMT-03:00' package
Remove-Item Env:AGENDAPP_TEST_TURNOS_POSTGRES_URL
docker stop agendapp-disponibilidad-test
```

La integración usa JPA y transacciones reales, comprueba las fechas persistidas y observa
esperas de bloqueo en `pg_stat_activity`/`pg_blocking_pids`. Solo RabbitTemplate se simula.
Sin la variable indicada, esa suite se omite. Los `contextLoads` generales no se incluyen
porque usan la configuración e infraestructura de la aplicación.

Desde `frontend`:

```powershell
.\node_modules\.bin\eslint.cmd src/features/publico/components/ModalReserva.jsx src/features/publico/utils/fechasReserva.js src/features/publico/utils/fechasReserva.test.js
$env:TZ = 'Asia/Tokyo'
node --test src/features/publico/utils/fechasReserva.test.js
$env:TZ = 'America/Argentina/Buenos_Aires'
node --test src/features/publico/utils/fechasReserva.test.js
npm.cmd run build
```

Las tres pruebas de fecha se ejecutan en ambas zonas y verifican que no se cambia el día
seleccionado y que la fecha mínima cambia a medianoche argentina.

Validación realizada: 183 pruebas unitarias/HTTP y 22 con PostgreSQL 16 real, todas exitosas
(205 en total, sin omisiones en las suites seleccionadas). También pasaron las 3 pruebas de
fecha en cada una de las dos zonas, el lint de los 3 archivos frontend afectados, el build
de React y el empaquetado de Reservas. Vite mantiene el aviso de bundle mayor a 500 kB.
El contenedor PostgreSQL temporal de esta validación se detuvo al terminar.

## Verificación manual desde AgendApp

1. Con Reservas y React actualizados, configurar un viernes futuro: negocio 22:00–02:00,
   recurso 22:30–01:30 y servicio de 30 minutos. Dejar cerrado el sábado.
2. Consultar el viernes: debe aparecer 23:30 aunque termine el sábado a las 00:00. Consultar
   el sábado: debe permitir elegir la madrugada del viernes, aunque el sábado esté cerrado.
   Cada opción corresponde a la fecha calendario seleccionada, no a una fecha UTC desplazada.
3. Para observar 23:45–00:15 desde la pantalla sin alterar duraciones, usar recurso
   22:15–01:30: el avance de 30 minutos ofrece 23:45. Confirmarlo y comprobar en Mis Reservas
   o en la respuesta de la API que el fin pertenece al día siguiente.
4. Agregar descanso del negocio 23:45–00:15. Las opciones que lo atraviesan deben desaparecer;
   el fin exacto a las 23:45 y el inicio exacto a las 00:15 siguen siendo válidos. Repetir
   con el descanso configurado en el recurso.
5. Reducir el cierre o usar un servicio cuya duración exceda el tiempo restante: no debe
   aparecer ese inicio. Una duración que termina exactamente al cierre sí debe ofrecerse.
6. Reservar en dos sesiones el mismo horario: la segunda confirmación debe seguir mostrando
   el 409 de 2A, limpiar la selección y actualizar las opciones. No se habilita reconfirmación.

Estas pruebas automatizadas no sustituyen la comprobación manual del navegador ni la
entrega real de correo. No se enviaron mensajes al broker de AgendApp ni se migraron sus bases.
