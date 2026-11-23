---
name: test-pyramid
description: Usar siempre que escribas, cambies, muevas o revises pruebas en srtomy-api (JUnit, Mockito, @WebMvcTest, @SpringBootTest), y al corregir un bug. Dice en qué nivel de la pirámide va cada prueba (unit, component, integration, smoke) y cómo se llama la clase para que el CI la corra en su etapa.
---

# Pirámide de pruebas en srtomy-api (Java, Spring Boot)

Desarrolla el principio II de la constitución (_Test at the Right Level_, `.specify/memory/constitution.md`). Si este skill y la constitución difieren, gana la constitución.

## Reglas comunes

Estas reglas son las mismas en srtomy-api, srtomy-ui y srtomy-agent. Lo que cambia entre repos son las herramientas y los nombres de archivo (más abajo).

### Los niveles

De abajo hacia arriba: muchas pruebas abajo, pocas arriba. El CI los corre en este orden y se detiene en el primero que falla, así un error se ve en segundos y no después de los e2e.

| Nivel              | Qué prueba                                                                                                                                    | Cuántas    |
| ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| **unit**           | Lógica pura: una función, una regla de negocio, una validación. Sin framework, sin red, sin base de datos.                                    | La mayoría |
| **component**      | Un módulo con sus bordes simulados: un controlador, una pantalla, una ruta HTTP con dobles.                                                   | Muchas     |
| **integration**    | Con infraestructura real: Postgres, el contexto completo, la cadena de seguridad, las migraciones.                                            | Pocas      |
| **smoke**          | Que lo compilado arranca y responde lo básico. Menos de un minuto entre todas.                                                                | Un puñado  |
| **e2e `@story`**   | Una historia de usuario: un criterio de aceptación de un spec (`specs/NNN-*`), en una pantalla, desde un estado preparado.                    | Pocas      |
| **e2e `@journey`** | El recorrido de una persona por varias funciones, de principio a fin («la iglesia crea un evento, alguien se inscribe, se marca su ingreso»). | Muy pocas  |

Las pruebas de **contrato** entre repos (que la copia del OpenAPI de la UI y del agente coincida con la API) son trabajo futuro: están en la lista de deuda técnica.

### Cómo elegir el nivel

1. **Pregúntate qué se rompería** y escribe la prueba en el nivel más bajo que lo detecta. Una regla de negocio se rompe en una función: unit. Un mapeo HTTP se rompe en el controlador: component. Una consulta SQL se rompe contra Postgres: integration.
2. **Una validación, un mensaje de error o un permiso sobre un botón nunca son e2e.** Son unit o component.
3. **No repitas arriba lo que ya prueba abajo.** El nivel de arriba prueba que las piezas se conectan (el camino feliz y uno de error), no cada caso de la regla.
4. **Un e2e es `@story` o `@journey`**, y lo dice en su etiqueta. Una `@story` cita su spec. Antes de agregar un e2e, comprueba que no basta con uno de component.
5. **Un bug se reproduce en el nivel más bajo posible**, con una prueba que falla sin la corrección.
6. **Cada prueba dice su nivel** en el nombre del archivo o en su etiqueta (ver la tabla de este repo). Los hooks y el CI lo usan para correrla en su etapa.
7. **Prueba comportamiento, no implementación**, y sin red: los servicios externos se reemplazan por dobles.

### Al reescribir una prueba que está en un nivel demasiado alto

Muévela al nivel correcto en vez de borrarla: escribe la prueba nueva abajo, comprueba que falla si rompes lo que prueba, y recién entonces quita la de arriba. Si en el nivel de arriba quedaba algo que solo él probaba (la conexión entre piezas), deja un caso que lo cubra.

## En este repo

| Nivel           | Herramienta                                                                                                          | Nombre de la clase  | Ejemplo                                        |
| --------------- | -------------------------------------------------------------------------------------------------------------------- | ------------------- | ---------------------------------------------- |
| **unit**        | JUnit 5 y AssertJ; colaboradores con Mockito (`@ExtendWith(MockitoExtension.class)`). Sin Spring.                    | `*Tests`            | un `*CommandHandler`, un validador, el dominio |
| **component**   | `@WebMvcTest` con `MockMvcTester` y los handlers simulados (`@MockitoBean`).                                         | `*Tests`            | `AttendanceCheckInControllerTests`             |
| **integration** | `@SpringBootTest` con el Postgres de `docker compose` / del CI.                                                      | `*IntegrationTests` | `SecurityConfigIntegrationTests`               |
| **smoke**       | `@SpringBootTest(webEnvironment = RANDOM_PORT)`: el contexto arranca, `/actuator/health` y `/v3/api-docs` responden. | `*SmokeTests`       | —                                              |
| **e2e**         | No hay en este repo: los recorridos completos se prueban en srtomy-ui (`tests/`).                                    | —                   | —                                              |

- El sufijo es el nivel: `*IntegrationTests` y `*SmokeTests` cargan el contexto completo y son lentos; todo lo demás es `*Tests` y no lo carga. Una clase con `@SpringBootTest` que no termina en uno de esos dos sufijos está mal nombrada.
- **Una regla de dominio** (fechas que deben caer dentro de un evento, cupos, estados permitidos) se prueba en unit, sobre el dominio o el handler. En `*IntegrationTests` queda el camino feliz y un caso de error que muestre que la regla está conectada (el 400 llega), no cada variante de la regla.
- **Un controlador** se prueba con `@WebMvcTest`: el mapeo, la validación del cuerpo, los códigos HTTP y el permiso que pide (`AuthorizeQueryHandler` simulado).
- **`@SpringBootTest` solo cuando hace falta el contexto completo**: la cadena de filtros de seguridad de punta a punta, Flyway, transacciones que cruzan módulos, consultas JPA no triviales, el outbox.
- Los datos de prueba se arman con los `*Fixtures` que ya existen en cada módulo; no copies inserts entre clases.

### Comandos

```sh
./mvnw test -Dtest=FooTests                 # una clase
./mvnw test -Dtest='!*IntegrationTests,!*SmokeTests'   # unit y component, sin contexto
./mvnw verify                               # todo, como el CI
```
