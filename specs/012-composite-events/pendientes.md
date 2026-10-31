# Eventos compuestos: qué falta y qué limitaciones tiene

**Fecha**: 2026-10-31

**Alcance**: API ([`srtomy-api#74`](https://github.com/ouracademy/srtomy-api/pull/74)) y UI ([`srtomy-ui#93`](https://github.com/ouracademy/srtomy-ui/pull/93)), rama `ccr-5c538956-covxf8` en ambos repos. Spec: [`spec.md`](./spec.md). Las filas de deuda técnica que se citan están en `docs/redesign.md` §10 (API: 39 a 42; UI: 32).

Cada punto dice qué pasa hoy, qué se propone y de qué tamaño es (S: horas, M: un día o dos, L: más). «API» y «UI» indican en qué repo se hace.

## 1. Estado actual

Está hecho y probado: crear un evento de varios días («Fecha» y «Hasta»), llegar a su pantalla, armar un programa opcional con sub-eventos y cursos, inscribirse al mega evento y entrar a los sub-eventos que lo heredan, ver el programa en la página pública y en Mi espacio, y editar y borrar con las reglas del mega evento.

Pruebas: la API pasa `./mvnw clean verify` (1027 pruebas) y la UI `pnpm check:fast` (702). Los e2e de eventos y accesibilidad pasan, pero solo en Chromium.

Lo demás de este documento es lo que **no** está hecho.

## 2. Corrección pedida: el ingreso con el ticket del mega evento (prioridad 1)

**Lo pedido por el dueño del producto:** en un sub-evento, escanear el ticket del mega evento debe marcar la inscripción **igual que antes, tenga cuenta o no**.

**Qué pasa hoy:** en un sub-evento que hereda, el ingreso solo se registra si la cuenta de la inscripción está vinculada a una persona de la iglesia (se marca a esa persona en la asistencia del sub-evento). Si la inscripción no tiene cuenta, o la cuenta no está vinculada a una persona, la API responde 400 («anótela como visitante») y no queda ninguna marca. Las inscripciones sin cuenta son el caso normal (la inscripción pública no pide cuenta), así que esto falla justo en el caso común. Es la fila 40 de la deuda de la API, que hay que reescribir: deja de ser una limitación aceptada y pasa a ser un defecto.

**Lo que también falla por lo mismo:**

- El QR del evento (autoasistencia) escaneado dentro de un sub-evento no reconoce la inscripción del mega evento: una persona con inscripción pero sin cuenta queda como visitante nuevo o suma al contador de «sin nombre».
- El organizador de un sub-evento que hereda no ve quién está esperado: el panel de inscripción se oculta y la lista de inscritos es la del mega evento.
- «Asistí» en Mi espacio solo cuenta el sub-evento si hay una persona vinculada.

**Propuesta:** guardar el ingreso **por inscripción y por sub-evento**, sin tocar el ingreso de la inscripción del mega evento.

- **API (M):**
  - Tabla nueva `event_registration_check_in` con `registration_id`, `event_id`, `checked_in_at`, `check_in_source` y `marked_by`, única por `(registration_id, event_id)`. Migración con la siguiente versión libre de `main` (hoy `V1.19.0`).
  - `check_in_registration_by_ticket`: en un sub-evento que hereda guarda esa fila, es idempotente («ya ingresó», con la hora de la primera vez) y, si además hay persona vinculada, la marca en la asistencia como hoy.
  - `set_registration_check_in` (marcar o desmarcar a mano) y `check_in_user_registration` (QR del evento) buscan la inscripción en el mega evento cuando el sub-evento hereda.
  - `GET /events/{id}/registrations` de un sub-evento que hereda devuelve las inscripciones del mega evento con el `checkedInAt` de **ese** sub-evento.
- **UI (M):** el panel de inscripción de un sub-evento que hereda pasa de ocultarse a mostrar la lista de esperados e ingresados (sin la configuración, que es del mega evento) con el escáner de tickets; «Asistí» en Mi espacio une esa marca.
- **Pruebas:** handler, integración (inscripción sin cuenta que ingresa a un sub-evento, repite y no cambia la del mega evento), panel de la UI y e2e.
- **Decisión pendiente:** qué cuenta como «asistentes» de un sub-evento, si personas marcadas, inscripciones ingresadas, o ambas sin duplicar (ver §9).

## 3. Otras limitaciones de inscripción y asistencia

| Qué pasa hoy | Por qué importa | Propuesta |
| --- | --- | --- |
| **Cambiar el modo de un sub-evento no valida nada.** De «la del evento» a «propia», nadie puede inscribirse hasta configurarla (la API responde que no tiene inscripción). De «propia» a «la del evento» o «sin inscripción», las inscripciones propias quedan guardadas, sin uso y ocultas, sin aviso ni cancelación. | Un organizador puede dejar a gente inscrita en un lugar que ya no existe. | Avisar en la UI al cambiar el modo, y decidir qué pasa con las inscripciones propias existentes (cancelar con aviso o impedir el cambio). M, API y UI. |
| **Un sub-evento que hereda no tiene cupo propio.** Para tener aforo de sala hay que usar «propia», y entonces se pierde la entrada con el ticket del mega evento. | «Entrada general + cupo por sala» (el caso de un curso con 30 lugares dentro de un congreso) no se puede modelar. | Permitir ticket del mega evento **y** cupo propio en el mismo sub-evento. L, API y UI; decidir con el dueño del producto. |
| **Un solo ticket vale en todos los sub-eventos que heredan** (fila 41). No hay tickets que den acceso a solo algunos ni un resumen de a cuántos sub-eventos fue cada inscrito. | Congresos con entradas por día o por taller. | Tickets por sub-evento y asistencia consolidada por inscrito. L. |
| **Cancelar la inscripción al mega evento quita el lugar en todos los sub-eventos.** Solo hay un aviso en Mi espacio. | Se pierde de golpe todo el programa. | Cancelación parcial (por sub-evento). M, API y UI. |
| **Los pagos siguen siendo informativos** (el precio no se cobra; deuda anterior a esta feature). | Un congreso de pago no se puede cobrar desde la plataforma. | Fuera de esta feature. |

## 4. Fechas, edición y repetición

| Qué pasa hoy | Propuesta |
| --- | --- |
| **Ni el mega evento ni los sub-eventos pueden repetirse** (fila 39). El congreso anual se arma de nuevo cada año, y no hay «duplicar con su programa». | Acción «duplicar» que copie el mega evento y sus sub-eventos a otra fecha. M, API y UI. |
| **Mover un mega evento completo no se puede** (fila 42): un mega evento no puede editarse dejando a un sub-evento fuera, así que hay que ampliarlo, mover los sub-eventos uno por uno y acortarlo. | Acción «mover todo» que desplace también a los sub-eventos. M, API y UI. |
| **Un solo nivel.** Un curso con varias clases dentro del congreso son varios sub-eventos sueltos, sin agrupar. | Segundo nivel (mega evento, curso, sesiones) si se necesita. L. |
| **No se puede convertir un evento existente en sub-evento, sacarlo ni cambiarlo de mega evento.** La API responde 400 y la UI no ofrece la acción. | «Mover a otro evento» desde la pantalla del evento. M, API y UI. |
| **Zonas horarias y horario de verano sin probar** en la banda del calendario (se usa la zona del navegador). | Probar cruces de horario de verano. S. |

## 5. Compartir y permisos

| Qué pasa hoy | Por qué importa | Propuesta |
| --- | --- | --- |
| **El acceso se copia al crear el sub-evento y no se hereda.** Cambiar después el acceso del mega evento no cambia el de los sub-eventos. | La página pública del mega evento solo lista los sub-eventos que el visitante puede abrir: uno que quedó restringido desaparece del programa sin explicación. | Una acción «aplicar a todo el programa» o heredar en vez de copiar. M, API y UI. |
| **Compartir el mega evento con una persona no la comparte con sus sub-eventos.** | Quien recibe el enlace del congreso no puede abrir los talleres. | Mismo cambio de arriba. |

## 6. Calendario y pantallas

- **No se vio la UI en un navegador.** La banda del calendario, la fila «Varios días», la sección «Programa» y el celular solo están cubiertos por pruebas. Antes de fusionar conviene abrir la pantalla de un evento de varios días. S.
- **El mega evento solo se distingue en el calendario si dura más de 24 horas.** Un mega evento de un día con sub-eventos se ve como un evento común, porque la lista del calendario no trae «tiene sub-eventos». M, API y UI.
- **«Programa» aparece en todo evento que no se repite** (fila 32), incluso un culto de un día. Decisión pendiente (§9). S.
- **El programa es solo de lectura y de alta.** No se reordena, no se edita ni se elimina un sub-evento desde la lista: hay que entrar a su pantalla. Los sub-eventos se crean solo desde el programa, no desde el calendario. M, UI.
- **Celular:** en la lista semanal no se pueden elegir varios días arrastrando; queda el campo «Hasta». Sin probar. S.
- **Accesibilidad:** la página pública con programa pasa axe. La pantalla del evento no se revisa con axe (deuda 25, contraste de los botones Evento/Curso).

## 7. Avisos, rendimiento y reportes

- **Borrar un mega evento o un sub-evento no avisa a los inscritos.** Ya pasaba con los eventos sueltos; con el congreso se nota más. M, API.
- **Rendimiento de la página pública:** para listar el programa se consulta el acceso de cada sub-evento por separado. Con unos 40 sub-eventos puede ser lento. Hay que medirlo y, si hace falta, resolverlo en una sola consulta. S a M, API.
- **Reportes:** no revisé si las métricas de crecimiento (fila 35, que reproduce los eventos en memoria) o el directorio cuentan los sub-eventos como eventos aparte. S de revisar.
- **Correos al mover un sub-evento que hereda:** va un correo por persona inscrita al mega evento, en la misma transacción (cola de salida). Con miles de inscritos no se probó.

## 8. Pruebas y verificación que no se hicieron

- Los e2e corrieron solo en Chromium (Firefox y WebKit no). Usé una configuración temporal fuera del repo porque el Playwright del proyecto espera otra versión de Chromium que la instalada en mi entorno.
- No miré el resultado del CI de los dos PR después de crearlos.
- La migración `V1.18.0` solo corrió contra un Postgres local vacío, nunca contra una copia de datos reales.
- Las pruebas de la UI simulan la API; el contrato coincide con el que genera la rama de la API (comparado byte a byte), pero no se probó la UI contra la API corriendo.
- No hay captura visual de ninguna pantalla.

## 9. Preguntas abiertas para el dueño del producto

1. **Asistencia de un sub-evento:** ¿cuentan las personas marcadas, las inscripciones ingresadas, o ambas sin duplicar?
2. **«Programa» en todo evento:** ¿se muestra en cualquier evento que no se repite, solo en los de varios días, o detrás de un botón?
3. **Cupo por sala con entrada general:** ¿se necesita (un curso con 30 lugares dentro de un congreso)?
4. **Compartir:** ¿se hereda del mega evento o se copia al crear, como hoy?
5. **Duplicar y mover:** ¿cuál pesa más, «duplicar el congreso con su programa» (cada año) o «mover todo»?
6. **Cambiar el modo de inscripción de un sub-evento ya inscrito:** ¿se impide, o se cancelan las inscripciones con aviso?

## 10. Orden sugerido y relación con la deuda técnica

| Orden | Qué | Fila de deuda | Tamaño |
| --- | --- | --- | --- |
| 1 | Ingreso con el ticket del mega evento, tenga cuenta o no (§2) | 40 (se reescribe como defecto) | M a L |
| 2 | Compartir a todo el programa (§5) | nueva | M |
| 3 | Duplicar el congreso con su programa y mover todo (§4) | 42 | M |
| 4 | Aviso al cambiar el modo de inscripción y al borrar (§3 y §7) | nueva | M |
| 5 | Medir y arreglar el rendimiento de la página pública (§7) | nueva | S a M |
| 6 | Tickets por sub-evento y cupo propio (§3) | 41 | L |
| 7 | Repetir un congreso y segundo nivel (§4) | 39 | L |

Las filas «nueva» todavía no están en la tabla de deuda de `docs/redesign.md`: cuando el dueño del producto confirme el orden, se agregan allí (regla del repo: lo pendiente va a esa tabla y no se hace nada de ella sin consultarlo).
