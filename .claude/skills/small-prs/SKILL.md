---
name: small-prs
description: Usar siempre antes de abrir un pull request o de empujar una rama para revisión, y al planear un cambio grande. Mantiene cada PR corto (unas 300 líneas, un solo tema) para que una persona lo lea de una sentada, y apila PRs (uno encima del otro) cuando el cambio no entra en uno.
---

# PRs cortos

Un PR lo revisa una persona. Si no lo puede leer de una sentada, no lo revisa de verdad. Esta regla es la misma en srtomy-api, srtomy-ui y srtomy-agent.

## La regla

- **Un tema por PR.** Un PR hace una sola cosa que se explica en una frase: «agrega el skill», «parte el CI en etapas», «baja las pruebas de validación a componente». Si la descripción necesita «y además», son dos PRs.
- **Unas 300 líneas cambiadas como máximo** (agregadas + borradas), sin contar lo que nadie revisa línea por línea: lockfiles, código generado, la especificación OpenAPI copiada (`openapi/*.json`), snapshots. Míralo con el script de este skill:

  ```sh
  bash .claude/skills/small-prs/pr-size.sh            # contra origin/main
  bash .claude/skills/small-prs/pr-size.sh origin/mi-rama-anterior   # en un PR apilado
  ```

  Sale con código 1 si pasa el límite (`PR_SIZE_LIMIT`, por defecto 300).

- **Cada PR deja `main` funcionando**: compila, pasa sus checks y no depende del PR siguiente para tener sentido.
- **Sin mezclar**: un renombre o un movimiento mecánico de archivos va en su propio PR, aunque sea grande; dilo en la descripción («solo mueve archivos, sin cambios de contenido»). Lo mismo para un cambio de formato.

Pasarse un poco está bien si partirlo lo haría más difícil de entender (por ejemplo, una prueba con lo que prueba). Dilo en la descripción y explica por qué.

## Cómo partir un cambio grande

En este orden, cada paso un PR:

1. **Preparar** sin cambiar el comportamiento: refactor, mover, renombrar, extraer.
2. **El cambio**, con sus pruebas.
3. **Lo que lo usa**: la pantalla, el CI, la documentación.
4. **Limpiar** lo viejo, si el dueño del producto lo aprobó (ver «Pending work and cleanup» en `AGENTS.md`).

Otras formas que funcionan: por módulo o carpeta, por nivel de pruebas, por capa (API, servicio, pantalla).

## PRs apilados

Cuando un cambio necesita varios PRs, cada uno va encima del anterior:

```
main ◀── rama        (PR 1, base: main)
           ◀── rama-2 (PR 2, base: rama)
                 ◀── rama-3 (PR 3, base: rama-2)
```

- Crea `rama-2` desde `rama`, y abre su PR **con base `rama`**, no `main`: así el diff muestra solo lo nuevo.
- En la descripción de cada PR va la pila completa: «Parte 2 de 3. Encima de #12. Sigue #14.»
- Se fusionan en orden, de abajo hacia arriba. Al fusionar el PR 1, GitHub cambia la base del PR 2 a `main` si la rama 1 se borra; si no, cámbiala a mano.
- Si corriges algo en un PR de abajo, lleva el cambio hacia arriba con `git merge` en cada rama de encima (no reescribas historia de una rama que ya tiene PR).
- Si un PR de abajo se fusionó con _squash_, rebasa la rama de encima: `git rebase --onto origin/main <rama-de-abajo> <rama>`.

## La descripción del PR

- **Qué y por qué**, en dos o tres frases.
- **Cómo revisarlo**: por dónde empezar, qué archivos son mecánicos.
- **La pila**, si es apilado.
- **Qué queda fuera** y dónde quedó anotado (la lista de deuda técnica).

El título sigue Conventional Commits, como los commits.
