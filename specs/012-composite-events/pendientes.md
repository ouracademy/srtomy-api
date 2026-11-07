# Eventos compuestos: qué falta y qué limitaciones tiene

**Fecha**: 2026-10-31

**Alcance**: API ([`srtomy-api#74`](https://github.com/ouracademy/srtomy-api/pull/74)) y UI ([`srtomy-ui#93`](https://github.com/ouracademy/srtomy-ui/pull/93)), rama `ccr-5c538956-covxf8` en ambos repos. Spec: [`spec.md`](./spec.md). Las filas de deuda técnica que se citan están en `docs/redesign.md` §10 (API: 39 a 41; UI: 32).

Cada punto dice qué pasa hoy, qué se propone y de qué tamaño es (S: horas, M: un día o dos, L: más). «API» y «UI» indican en qué repo se hace.

**Marcas:** ~~tachado~~ = ya hecho o desestimado por el dueño del producto (cada uno lo dice al lado). Sin tachar = pendiente.

## 1. Estado actual

Está hecho y probado: crear un evento de varios días («Fecha» y «Hasta»), llegar a su pantalla, armar un programa opcional con sub-eventos y cursos, inscribirse al mega evento y entrar a los sub-eventos que lo heredan, ver el programa en la página pública y en Mi espacio, y editar y borrar con las reglas del mega evento.

Pruebas: la API pasa `./mvnw clean verify` (1027 pruebas) y la UI `pnpm check:fast` (702). Los e2e de eventos y accesibilidad pasan, pero solo en Chromium.

Lo demás de este documento es lo que **no** está hecho.

## 2. ~~Corrección pedida: el ingreso con el ticket del mega evento (prioridad 1)~~ HECHO (3 nov)

> **Hecho en la API (#74) y en la UI (#93).** Lo que sigue describe el defecto y la propuesta tal como estaban; ahora el ingreso se guarda por inscripción y sub-evento (`event_registration_check_in`, migración `V1.19.0`), el ticket, la marca a mano y el QR del evento lo usan, el sub-evento que hereda lista a los esperados con su ingreso y «Asistí» lo cuenta. Queda pendiente la vista de «asistentes» consolidada (personas marcadas + inscripciones ingresadas sin duplicar) como número único.

**Lo pedido por el dueño del producto:** en un sub-evento, escanear el ticket del mega evento debe marcar la inscripción **igual que antes, tenga cuenta o no**.

**Qué pasa hoy:** en un sub-evento que hereda, el ingreso solo se registra si la cuenta de la inscripción está vinculada a una persona de la iglesia (se marca a esa persona en la asistencia del sub-evento). Si la inscripción no tiene cuenta, o la cuenta no está vinculada a una persona, la API responde 400 («anótela como visitante») y no queda ninguna marca. Las inscripciones sin cuenta son el caso normal (la inscripción pública no pide cuenta), así que esto falla justo en el caso común. Es la fila 40 de la deuda de la API, que hay que reescribir: deja de ser una limitación aceptada y pasa a ser un defecto.

**Lo que también falla por lo mismo** (los tres se arreglan con el mismo cambio):

- **QR del evento (autoasistencia) dentro de un sub-evento que hereda.** El sistema busca la inscripción de quien escanea (por su cuenta o por su correo verificado) entre las del sub-evento, y no hay ninguna: la inscripción es del mega evento. Quien tiene cuenta no vinculada a una persona queda como visitante nuevo, o suma al contador de «sin nombre». *Aclaración:* quien escanea el QR **sin cuenta ni correo verificado** tampoco se reconoce en un evento normal (el sistema no sabe quién es), así que eso no es un defecto de los sub-eventos. El defecto es para quien sí se puede reconocer.
- **El organizador de un sub-evento que hereda no ve quién está esperado.** En un evento normal el panel de inscripción muestra a los inscritos con su estado (ingresó o falta). En un sub-evento que hereda ese panel se oculta, porque la inscripción es del mega evento: el organizador del taller no sabe cuánta gente se espera ni quién ya entró; solo ve la lista de asistencia a medida que marca.
- **«Asistí» en Mi espacio.** En un evento normal se enciende si la inscripción está marcada o si la persona vinculada está en la lista. En un sub-evento que hereda solo cuenta la persona vinculada: alguien con cuenta sin persona vinculada que entró con su ticket no aparece como asistente.

**Casos de la vida real que esto debe cubrir** (dicho por el dueño del producto):

1. **Lo más común: un mega evento y ya se tiene acceso a todo.** Todos los sub-eventos heredan (`INHERIT`). Es el caso que este defecto rompe.
2. **Un mega evento donde uno o dos sub-eventos tienen cupo y precio aparte.** Esos sub-eventos usan inscripción propia (`OWN`), con su cupo y su ticket. Funciona hoy y no se toca: su ingreso es el de un evento normal (marca de la inscripción propia). El precio es informativo; no se cobra desde la plataforma (§3).
3. **Mezcla de los dos en el mismo congreso.** Quien se inscribió al mega evento entra a los sub-eventos que heredan con el ticket del mega evento, y a los de inscripción propia con el ticket de ese sub-evento. Un sub-evento sin inscripción (`OPEN`) no pide ticket.

Por eso el cambio de abajo solo aplica a los sub-eventos que **heredan**: los de inscripción propia siguen como un evento normal y los libres no tienen inscripciones.

**Decisión tomada (3 nov): los asistentes de un sub-evento son las personas marcadas y las inscripciones ingresadas, sin duplicar** (si una inscripción ingresada es de una persona que además está marcada, cuenta una sola vez).

**Propuesta:** guardar el ingreso **por inscripción y por sub-evento**, sin tocar el ingreso de la inscripción del mega evento.

- **API (M):**
  - Tabla nueva `event_registration_check_in` con `registration_id`, `event_id`, `checked_in_at`, `check_in_source` y `marked_by`, única por `(registration_id, event_id)`. Migración con la siguiente versión libre de `main` (hoy `V1.19.0`).
  - `check_in_registration_by_ticket`: en un sub-evento que hereda guarda esa fila, es idempotente («ya ingresó», con la hora de la primera vez) y, si además hay persona vinculada, la marca en la asistencia como hoy.
  - `set_registration_check_in` (marcar o desmarcar a mano) y `check_in_user_registration` (QR del evento) buscan la inscripción en el mega evento cuando el sub-evento hereda.
  - `GET /events/{id}/registrations` de un sub-evento que hereda devuelve las inscripciones del mega evento con el `checkedInAt` de **ese** sub-evento.
- **UI (M):** el panel de inscripción de un sub-evento que hereda pasa de ocultarse a mostrar la lista de esperados e ingresados (sin la configuración, que es del mega evento) con el escáner de tickets; «Asistí» en Mi espacio une esa marca.
- **Pruebas:** handler, integración (inscripción sin cuenta que ingresa a un sub-evento, repite y no cambia la del mega evento), panel de la UI y e2e.
- **Asistentes:** personas marcadas más inscripciones ingresadas, sin duplicar (decidido, ver arriba). Una inscripción ingresada cuya cuenta está vinculada a una persona ya marcada cuenta una vez.

## 3. Lo que falta, con ejemplos de uso

Criterio de diseño: ser flexible para cualquier tipo de evento, como lo hacen Eventbrite o los sistemas de congresos. En esos sistemas: (a) hay **varios tipos de ticket** («Pase completo», «Un día», «Solo taller»), (b) cada ticket **da acceso a una parte del programa**, (c) algunas sesiones tienen **cupo o precio extra** encima del pase general, (d) el organizador **duplica** el evento del año anterior, y (e) la persona ve **su propia agenda**. Todo lo de abajo apunta a eso, sin obligar a usarlo: un culto de un día sigue siendo un evento simple.

Personajes de los ejemplos: **Marta**, organizadora del congreso «Avivamiento 2030» (3 días, 9 sub-eventos); **Pedro**, pastor de jóvenes que organiza un taller dentro del congreso; **Lucía**, asistente con cuenta; **Tomás**, asistente sin cuenta; **Rosa**, ujier.

Marcas: **HECHO** = ya está en las ramas, con pruebas. Lo marcado «local» se subió en un solo push al final (7 nov). **[DECIDE]** = necesita tu decisión antes de hacerlo. **[DISEÑO]** = solo queda el diseño por ahora.

### ~~P1. Compartir a todo el programa~~ HECHO (local, sin subir)

> **Qué quedó hecho:** en «Compartir» del mega evento aparece la casilla «Aplicar este acceso también a los N sub-eventos del programa». Al guardar, el acceso general se aplica al congreso y a todos sus sub-eventos. **Lo que no hace todavía:** no marca como «Acceso propio» a los sub-eventos que Pedro hubiera restringido a mano (los pisa si se marca la casilla; si no se marca, no los toca), y compartir con una **persona** (por correo) sigue siendo solo del evento en el que se hace.


**Hoy.** El acceso se copia al crear cada sub-evento y no se hereda.
*Ejemplo:* Marta crea el congreso «solo con enlace» y arma 9 sub-eventos (heredan «solo con enlace»). Una semana después lo abre a «toda la iglesia». El congreso se ve, pero los 9 talleres siguen restringidos: Lucía abre la página pública del congreso y el programa aparece **vacío**, sin explicación. Y si Marta comparte el congreso con el pastor invitado, él no puede abrir ningún taller.

**Cómo debe ser.** Al cambiar el acceso de un mega evento, la pantalla de compartir pregunta: «Este evento tiene 9 sub-eventos. ¿Aplicar el mismo acceso a todo el programa?» con dos opciones: *solo al congreso* o *al congreso y a todo su programa*. Un sub-evento que se haya cambiado a mano antes se **muestra como distinto** («Acceso propio») y no se pisa sin confirmar.

**Flujo.** Marta abre «Compartir» del congreso, elige «Toda la iglesia», ve el aviso «Aplicar a los 9 sub-eventos», acepta. Lucía abre el congreso y ve los 9 talleres. Si Pedro había restringido su taller a su grupo, el aviso dice «1 sub-evento tiene acceso propio y no cambiará».

**Qué arregla.** Programas públicos que se ven vacíos y talleres que nadie puede abrir.
**Tamaño:** M, API y UI.

### ~~P2. Duplicar el congreso con su programa~~ DESESTIMADO (7 nov, dueño del producto)

> No se necesita. Se había hecho (copia privada con todo el programa) y se **eliminó** por decisión del dueño del producto; no queda código. «Mover todo» el programa a otra fecha tampoco se hará por ahora: si un congreso se corre de fecha, se mueven los sub-eventos uno por uno arrastrándolos en el calendario del programa (el mega evento primero se amplía).

### ~~P3. «Programa» solo cuando hace falta~~ HECHO (local, sin subir)

> **Qué quedó hecho:** en un evento de un solo día sin sub-eventos, «Programa» aparece como el botón «Armar un programa»; en uno de varios días, o que ya tiene sub-eventos, la sección se ve abierta.


**Hoy.** La sección «Programa» aparece en todo evento que no se repite, incluso en un culto de una hora (deuda UI 32).
*Ejemplo:* Pedro abre su reunión de oración de un día y ve «Programa: es opcional, agrega eventos y cursos». No entiende qué es y lo ve en todos sus eventos.

**Cómo debe ser.** El programa se ve siempre en un evento que ya lo tiene o dura varios días. En un evento de un solo día aparece como un botón discreto, «Armar un programa», que despliega la sección. El que no lo necesita no lo ve.

**Flujo.** Pedro abre su reunión de oración: no hay «Programa». Marta abre una jornada de un día con 4 charlas y pulsa «Armar un programa»: aparece la sección con «Agregar sub-evento».
**Tamaño:** S, UI.

### ~~P4. Aviso al cambiar el modo de inscripción o al borrar con inscritos~~ HECHO (7 nov)

> **Qué quedó hecho (elegiste cancelar con aviso):**
> - **Cambiar el modo:** si un sub-evento de inscripción propia pasa a «la del evento» o «sin inscripción», la pantalla avisa «se cancelarán las inscripciones propias y se avisará por correo». Al confirmar, esas inscripciones se cancelan y cada persona recibe un correo que dice cómo entrar ahora.
> - **Borrar:** la confirmación dice «se avisará por correo a quienes estén inscritos». Al borrar un evento (o un congreso con su programa), cada persona recibe **un solo** correo con lo que se canceló. No se avisa de eventos que ya pasaron.
> - **Lo que no hace:** pasar de «la del evento» a «propia» no avisa a nadie (los esperados del congreso ya no entrarían a ese sub-evento con su ticket); está anotado como mejora.
> - El texto de abajo describe el escenario original.

**Hoy.** Cambiar el modo de un sub-evento ya inscrito no valida nada, y borrar un evento no avisa a los inscritos.
*Ejemplo:* Pedro tiene su taller con inscripción propia y 25 inscritos. Lo cambia a «la del congreso» por error: las 25 inscripciones quedan guardadas, sin uso y ocultas, y nadie se entera. O Marta borra un taller con 30 inscritos y ninguno recibe aviso.

**Cómo debe ser** (propuesta, a confirmar). Al cambiar el modo, la pantalla advierte «25 personas están inscritas en este taller» y ofrece **cancelarlas con aviso por correo** o **mantener el modo actual**. Al borrar un evento con inscritos, se les avisa por correo que fue cancelado. Es el comportamiento estándar de los sistemas de eventos.

**Qué arregla.** Gente inscrita a un lugar que ya no existe. **Tamaño:** M, API y UI.
**Pregunta para ti (§9.5):** ¿impedir el cambio, o cancelar las inscripciones con aviso?

### P5. Tickets por sub-evento, cupo propio y «pase del congreso + extra» — A y B HECHOS (7 nov), C pendiente

> **Escenario A (entradas por día) — HECHO.** En la configuración de inscripción del congreso, con más de un tipo de ticket, cada ticket tiene «Da acceso a»: vacío es todo el programa; si eliges sub-eventos, solo abre esos. El ujier que escanea un «Pase sábado» el viernes recibe «El ticket “Pase sábado” no da acceso a “Viernes”». Cada sub-evento solo lista como esperados a los inscritos cuyo ticket le da acceso, y Mi espacio solo muestra los sub-eventos a los que el ticket da acceso.
> **Escenario B (taller extra que exige el pase) — HECHO.** Un sub-evento con inscripción propia (con su cupo y precio) puede marcarse «Solo para quienes ya se inscribieron al congreso». Quien intenta inscribirse sin tener un lugar confirmado en el congreso recibe «Para inscribirte aquí primero necesitas inscribirte a “…”». Se reconoce por la cuenta o por el correo.
> **Escenario C (soltar un día) — pendiente** (ver P6).
> **Límites de esta primera versión:** (1) si se borran todos los sub-eventos a los que daba acceso un ticket, ese ticket vuelve a valer para todo; (2) la página pública no sabe qué ticket tendrá la persona, así que sigue diciendo «Incluido en tu ticket» a todos los sub-eventos que heredan; (3) el precio sigue siendo informativo.

**Hoy.** Un ticket del congreso vale en todos los sub-eventos que lo heredan. Un sub-evento con cupo y precio aparte (inscripción propia) funciona, pero **no acepta el ticket del congreso**, y no se puede exigir estar inscrito al congreso para inscribirse a uno propio.
*Ejemplos:*
- Marta vende «Pase completo» (3 días) y «Pase sábado» (solo sábado). Hoy los dos valen en todo el programa: el «Pase sábado» entra el viernes.
- El taller de Pedro tiene 30 lugares y cuesta S/ 20 extra. Hoy Tomás puede inscribirse al taller **sin** haberse inscrito al congreso, y Lucía, que ya tiene el pase completo, no puede usar su ticket para el taller: tiene que hacer una inscripción aparte.

**Cómo debe ser.** (1) Cada tipo de ticket del congreso **dice a qué sub-eventos da acceso** (todos, o solo los marcados). (2) Un sub-evento puede ser **«incluido con cupo»** (acepta el pase del congreso y tiene aforo propio) o **«extra»** (inscripción propia que **exige** tener el pase del congreso). (3) La persona ve su **agenda**: los sub-eventos a los que su ticket da acceso y los que tiene aparte.

**Flujo.** Lucía compra el «Pase completo» y en su agenda ve los 8 talleres incluidos; para el taller de Pedro ve «Cupo limitado: 12 lugares — Reservar mi lugar» (gratis, por ser parte de su pase). Tomás compra el «Pase sábado» y su agenda solo muestra los del sábado. Rosa escanea el ticket de cada uno en cada puerta y el sistema acepta o rechaza según el día.

**Qué arregla.** Entradas por día, talleres con aforo, extras de pago y control real de la puerta. **Tamaño:** L, API y UI. Es el cambio más grande; conviene hacerlo después de decidir §9.2.

### P6. Cancelación parcial y cambio de ticket [PENDIENTE — decidiste dejarlo para después]

**Hoy.** Cancelar la inscripción al congreso quita el lugar en **todos** los sub-eventos; solo hay un aviso en Mi espacio.
*Ejemplo:* Lucía solo puede venir el sábado. No puede quitar los otros días sin cancelar todo y perder su lugar.

**Cómo debe ser.** En Mi espacio, Lucía puede **soltar un sub-evento** (libera el lugar para otra persona) o cambiar su pase, sin cancelar el congreso. **Tamaño:** M, API y UI; depende de P5.

### P7. ~~Etiqueta de agrupación~~ HECHA (7 nov) · Segundo nivel [DISEÑO]

> **Etiqueta: HECHA.** Al agregar o editar un sub-evento hay un campo «Curso o sala (opcional)», por ejemplo «Curso de liderazgo» o «Sala 2». En el programa (pantalla del evento y página pública) cada sesión muestra su etiqueta junto al tipo. **No hace todavía** una vista agrupada o con filtro por etiqueta: hoy se ve como una insignia en cada sesión, en orden de hora. El segundo nivel real sigue sin hacerse (solo si lo pide un caso que la etiqueta no cubra).

**Hoy.** Un solo nivel. Un curso de 5 clases dentro del congreso son 5 sub-eventos sueltos, sin agrupar.
*Ejemplo:* el «Curso de liderazgo» tiene 5 clases en 3 días y aparece como 5 filas sin relación en el programa.

**Cómo debe ser** (recomendación). En vez de anidar sin límite, agregar una **etiqueta de agrupación** (curso, sala o pista) que el programa muestra agrupada. Es lo que usan los congresos («Pista A», «Sala 2»); es más simple que un segundo nivel y no rompe nada de lo hecho. Un segundo nivel real solo si lo pide un caso concreto. **Tamaño:** M con etiqueta; L con segundo nivel.

### ~~P8. Calendario: ver mejor los congresos~~ HECHO (7 nov)

> **Hecho:** los eventos que tienen programa salen en el calendario con «(programa: N)» y un borde inferior grueso, aunque duren menos de 24 horas. Al elegir un horario en el calendario que cae dentro de un congreso o de un evento con programa, el formulario ofrece «Agregar al programa de “…”», que abre el formulario de sub-evento con esa hora.

- **Hoy:** un mega evento de un día con sub-eventos se ve como evento común, porque la lista del calendario no dice que tiene sub-eventos. *Ejemplo:* la «Jornada de un día» con 4 charlas no se distingue de un culto. **Debe:** marcar con un distintivo a todo evento que tiene programa. M, API y UI.
- **Hoy:** los sub-eventos se crean solo desde el programa, no desde el calendario general. *Ejemplo:* Marta está en el calendario viendo el sábado y quiere agregar una charla ahí. **Debe:** al elegir un horario dentro de un congreso, ofrecer «Agregar al programa de…». S, UI.

### ~~P9. Rendimiento y reportes~~ REVISADOS (7 nov)

> **Rendimiento — medido:** con 40 sub-eventos, la página pública del congreso respondió en 16 a 23 ms para un visitante sin sesión y en 23 a 67 ms con sesión iniciada (medido en una base local de pruebas). No hace falta optimizar ahora; si un congreso real fuera lento, se revisa entonces.
> **Reportes — revisados:** la métrica de crecimiento cuenta cambios de estado de los **miembros**, no eventos, así que los sub-eventos no la afectan. No encontré otro reporte que cuente eventos. Nada que cambiar.

- La página pública consulta el acceso de cada sub-evento por separado: con unos 40 sub-eventos puede ser lenta. **Hay que medir** y, si hace falta, resolverlo en una sola consulta. S a M, API.
- No revisé si las métricas de crecimiento y el directorio cuentan los sub-eventos como eventos aparte. *Ejemplo:* un congreso de 9 sub-eventos podría contar como 10 eventos en el reporte de la iglesia. S de revisar.
- Un correo por persona al mover un sub-evento que hereda, dentro de la misma transacción: con miles de inscritos no se probó. S, API.

### P10. Pagos [FUERA DE ESTA FEATURE]

El precio es informativo y no se cobra desde la plataforma (Culqi sin conectar). Un congreso de pago o un taller con extra no puede cobrarse aquí todavía. Depende de la integración de pagos y de P5.

## 4. Asistentes de un sub-evento: cómo contarlos (decidido)

Los asistentes de un sub-evento son **las personas marcadas más las inscripciones ingresadas, sin duplicar**.
*Ejemplo:* en el taller de Pedro, Lucía (con cuenta vinculada a una persona) entra con su ticket: queda como inscripción ingresada **y** como persona marcada; cuenta **una** vez. Tomás (sin cuenta) entra con su ticket: cuenta una vez como inscripción ingresada. Una persona que llegó sin inscribirse y Rosa la anotó como visitante: cuenta una vez como persona. **HECHO (7 nov):** la pantalla de cualquier evento muestra «Asistieron N personas» con el detalle «En la lista», «Solo con ticket» y «Visitantes», contando una sola vez a quien está en la lista y además entró con ticket. Se actualiza cada 15 segundos.

## 5. Pruebas y verificación que no se hicieron

- Los e2e corrieron solo en Chromium; no corrí los de las últimas tandas (calendario del programa y esperados).
- Nunca vi la UI en un navegador: la banda del calendario, el calendario del programa, el celular.
- La migración `V1.19.0` y `V1.18.0` solo corrieron en una base local vacía, no contra datos reales.
- La UI se probó con la API simulada, no contra la API corriendo.
- Horario de verano y zonas horarias en la banda del calendario: sin probar.
- Accesibilidad: la pantalla del evento no se revisa con axe (deuda 25).

## 6. Preguntas abiertas para el dueño del producto

1. ~~Asistencia de un sub-evento~~ **Resuelta:** personas marcadas e inscripciones ingresadas, sin duplicar.
2. ¿Quieres **tickets que den acceso a solo parte del programa** («Pase sábado») y **talleres extra que exijan el pase del congreso**? Es P5 y define casi todo lo grande que queda.
3. ¿El acceso se **hereda** siempre del congreso, o se **copia** al crear y se aplica a todo con una confirmación (propuesta de P1)?
4. ~~¿Duplicar el congreso o mover todo?~~ **Resuelta (7 nov):** ninguna de las dos se hará.
5. Al cambiar el modo de inscripción de un sub-evento ya inscrito: ¿se impide, o se cancelan las inscripciones con aviso por correo?
6. ¿Prefieres una **etiqueta de agrupación** (pista, sala, curso) o un **segundo nivel** real? (P7)

## 7. Orden sugerido y relación con la deuda técnica

| Orden | Qué | Estado |
| --- | --- | --- |
| ~~1~~ | ~~Ingreso con el ticket del mega evento, tenga cuenta o no~~ | **HECHO** |
| ~~2~~ | ~~Compartir a todo el programa (P1)~~ | **HECHO** |
| ~~3~~ | ~~Duplicar el congreso y mover todo (P2)~~ | **DESESTIMADO** |
| ~~4~~ | ~~«Programa» solo cuando hace falta (P3)~~ | **HECHO** |
| ~~5~~ | ~~Aviso al cambiar el modo y al borrar (P4)~~ | **HECHO** |
| ~~6~~ | ~~Número único de asistentes (§4)~~ | **HECHO** |
| ~~7~~ | ~~Tickets por parte del programa y extras con pase (P5 A y B)~~ | **HECHO** |
| 8 | Soltar un día / cancelación parcial (P5 C, P6) | pendiente |
| ~~9~~ | ~~Calendario (P8), rendimiento y reportes (P9), etiqueta (P7)~~ | **HECHO / REVISADO** |
| 10 | Segundo nivel real, vista agrupada por etiqueta | pendiente, solo si hace falta |
| ~~11~~ | ~~Repetir un congreso automáticamente~~ | **DESESTIMADO** |
| 12 | Pagos (Culqi) | fuera de esta feature |

Las filas «nueva» todavía no están en la tabla de deuda de `docs/redesign.md`: cuando el dueño del producto confirme el orden, se agregan allí (regla del repo: lo pendiente va a esa tabla y no se hace nada de ella sin consultarlo).
