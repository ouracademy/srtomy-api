# 0002. No mover todo un mega evento con sus sub-eventos

- **Estado:** Aceptada (decidida por el dueño del producto, 7 nov 2026).
- **Antes:** ítem 41 de la deuda técnica; también «P2» y la pregunta 4 de `specs/012-composite-events/pendientes.md`.

## Contexto

Un mega evento no puede editarse dejando a un sub-evento fuera de sus fechas. Para correr todo el programa (por ejemplo, al año siguiente) hay que ampliar el mega evento, mover los sub-eventos uno por uno y luego acortarlo. Se pensó una acción «mover todo» que desplazara también a los sub-eventos, y «duplicar el congreso con su programa». Esta última se llegó a construir.

## Decisión

No se hará ninguna de las dos. La copia del congreso con su programa se **eliminó** y no queda código.

## Motivo

El dueño del producto dijo que no se necesita. Si un congreso se corre de fecha, el flujo aceptado es ampliar el mega evento y arrastrar los sub-eventos en el calendario del programa.

## Consecuencias

- Mover un programa completo cuesta un arrastre por sub-evento.
- No hay endpoint de duplicar ni de «mover todo».
