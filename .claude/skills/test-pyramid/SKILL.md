---
name: test-pyramid
description: El estándar de pruebas de srtomy-api. Usar siempre que escribas, cambies, muevas o revises pruebas (JUnit, Mockito, @WebMvcTest, @SpringBootTest) o un doble de prueba, y al corregir un bug. Dice en qué nivel de la pirámide va cada prueba (unit, component, integration, smoke), cómo se llama la clase para que el CI la corra en su etapa y qué tipo de doble usar (dummy, stub, spy, mock, fake) y cómo nombrarlo.
---

# Estándar de pruebas de srtomy-api (Java, Spring Boot)

Este skill es **el** estándar de pruebas del repo: la única fuente de los niveles, los nombres, los dobles y las herramientas. La constitución (`.specify/memory/constitution.md`, principio II) tiene la regla y su porqué; `AGENTS.md` y `docs/code-style.md` solo apuntan aquí.

## Común a los tres repos

> Este bloque es **idéntico** en srtomy-api, srtomy-ui y srtomy-agent. Si lo cambias, cámbialo en los tres en el mismo momento. Lo propio de cada repo va en «En este repo», más abajo.

### Los niveles

De abajo hacia arriba: muchas pruebas abajo, pocas arriba. El CI los corre en este orden y se detiene en el primero que falla, así un error se ve en segundos y no después de los e2e.

| Nivel              | Qué prueba                                                                                                                                    | Cuántas    |
| ------------------ | --------------------------------------------------------------------------------------------------------------------------------------------- | ---------- |
| **unit**           | Lógica pura: una función, una regla de negocio, una validación. Sin framework, sin red, sin base de datos.                                    | La mayoría |
| **component**      | Un módulo con sus bordes reemplazados por dobles: un controlador, una pantalla, una ruta HTTP.                                                | Muchas     |
| **integration**    | Con infraestructura real: Postgres, el contexto completo, la cadena de seguridad, las migraciones.                                            | Pocas      |
| **smoke**          | Que lo compilado arranca y responde lo básico. Menos de un minuto entre todas.                                                                | Un puñado  |
| **e2e `@story`**   | Una historia de usuario: un criterio de aceptación de un spec (`specs/NNN-*`), en una pantalla, desde un estado preparado.                    | Pocas      |
| **e2e `@journey`** | El recorrido de una persona por varias funciones, de principio a fin («la iglesia crea un evento, alguien se inscribe, se marca su ingreso»). | Muy pocas  |

Las pruebas de **contrato** entre repos son trabajo futuro (lista de deuda técnica de srtomy-ui).

### Cómo elegir el nivel

1. **Todo cambio de lógica trae su prueba**, junto al archivo que prueba; el hook Stop de Claude Code la pide si falta.
2. **Pregúntate qué se rompería** y escribe la prueba en el nivel más bajo que lo detecta. Una regla de negocio se rompe en una función: unit. Un mapeo HTTP se rompe en el controlador: component. Una consulta SQL se rompe contra Postgres: integration.
3. **Una validación, un mensaje de error, un estado de carga o un permiso sobre un botón nunca son e2e.** Son unit o component.
4. **No repitas arriba lo que ya prueba abajo.** El nivel de arriba prueba que las piezas se conectan (el camino feliz y un caso de error), no cada variante de la regla.
5. **Un e2e es `@story` o `@journey`**, y lo dice en su etiqueta. Una `@story` cita su spec. Antes de agregar un e2e, comprueba que no basta con uno de component.
6. **Un bug se reproduce en el nivel más bajo posible**, con una prueba que falla sin la corrección.
7. **Cada prueba dice su nivel** en el nombre del archivo o en su etiqueta (ver «En este repo»). El CI y los hooks lo usan para correrla en su etapa, y lo comprueban.
8. **Prueba comportamiento, no implementación**, y sin red: lo que está fuera del repo se reemplaza por un doble.

### Dobles de prueba

Se usa el vocabulario de _xUnit Test Patterns_ (Gerard Meszaros), en la versión web del libro: <http://xunitpatterns.com/Test%20Double%20Patterns.html>. Un **doble de prueba** reemplaza, solo en la prueba, algo de lo que depende el código bajo prueba. **Cada doble es de un solo tipo, y el tipo lo da para qué lo usa la prueba**, no cómo está hecho:

| Tipo             | Para qué lo usa la prueba                                                                                                                                   | Quién verifica y cuándo                                                                                              |
| ---------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------- |
| **Dummy Object** | Llenar un argumento que el código bajo prueba no usa. Si alguien lo llama, debería fallar.                                                                  | Nadie lo verifica: la prueba mira el resultado del código bajo prueba.                                               |
| **Test Stub**    | Darle al código bajo prueba sus _entradas indirectas_: un valor fijo (_Responder_) o un error o valor inesperado (_Saboteur_), para llevarlo por un camino. | La prueba, después, por el resultado o el estado. Al stub no se le pregunta nada.                                    |
| **Test Spy**     | Anotar las _salidas indirectas_ del código bajo prueba (lo que le envía) para revisarlas después. Puede además devolver valores: sigue siendo un spy.       | **La prueba**, **después** de ejercitar, con lo que el spy anotó.                                                    |
| **Mock Object**  | Comprobar las salidas indirectas con expectativas fijadas **antes** de ejercitar.                                                                           | **El mock mismo**, **durante** la ejecución (falla en la primera llamada que no coincide), y una verificación final. |
| **Fake Object**  | Reemplazar una dependencia por una implementación de verdad, más liviana (por ejemplo en memoria), sin usarla para controlar ni para observar.              | La prueba, después, por el estado, como con la dependencia real.                                                     |

- **El nombre dice el tipo**: `StubIdentity`, `SpyKapso`, `FakeSeen`, `DummyToken`, y lo mismo en variables y funciones que los crean (`stubFetch`, `spyLog`). `TestX`, o `FakeX` para algo que no es un Fake Object, no dice qué es.
- **Stub o spy**: si la prueba solo usa las respuestas del doble, es un stub. Si revisa lo que el doble anotó, es un spy, aunque también responda. Un objeto no es «stub y spy a la vez».
- **Prefiere verificar el estado** (stub, fake) antes que las llamadas (spy, mock): una prueba que fija la secuencia exacta de llamadas se rompe con cualquier cambio interno. Verifica llamadas solo cuando la llamada es el comportamiento: un correo que se envía, un mensaje que sale, un pago que se cobra.
- **Las librerías no dan el tipo con su nombre.** Mockito (`mock()`, `@Mock`, `@MockitoBean`), Vitest (`vi.fn`, `vi.mock`) y `MockLanguageModelV4` crean dobles de cualquier tipo. Con `given(...).willReturn(...)` y sin `verify`, es un stub. Con `verify(...)` **después** de ejercitar, la prueba revisa lo anotado, así que es un spy. Es un mock solo si las expectativas se fijan antes y el doble falla por su cuenta.
- Un Dummy Object no es un Null Object: el dummy no se usa, y el null object sí se usa pero no hace nada.

### Al bajar una prueba de nivel

Muévela en vez de borrarla: escribe la prueba nueva abajo, comprueba que falla si rompes lo que prueba, y recién entonces quita la de arriba. Si arriba quedaba algo que solo ella probaba (la conexión entre piezas), deja un caso que lo cubra.

## En este repo

| Nivel           | Herramienta                                                                                                          | Nombre de la clase  | Ejemplo                                        |
| --------------- | -------------------------------------------------------------------------------------------------------------------- | ------------------- | ---------------------------------------------- |
| **unit**        | JUnit 5 y AssertJ; colaboradores con Mockito (`@ExtendWith(MockitoExtension.class)`). Sin Spring.                    | `*Tests`            | un `*CommandHandler`, un validador, el dominio |
| **component**   | `@WebMvcTest` con `MockMvcTester` y los handlers simulados (`@MockitoBean`).                                         | `*Tests`            | `AttendanceCheckInControllerTests`             |
| **integration** | `@SpringBootTest` con el Postgres de `docker compose` / del CI.                                                      | `*IntegrationTests` | `SecurityConfigIntegrationTests`               |
| **smoke**       | `@SpringBootTest(webEnvironment = RANDOM_PORT)`: el contexto arranca, `/actuator/health` y `/v3/api-docs` responden. | `*SmokeTests`       | —                                              |
| **e2e**         | No hay en este repo: los recorridos completos se prueban en srtomy-ui (`tests/`).                                    | —                   | —                                              |

- El sufijo es el nivel, y `ArchitectureTests` lo comprueba (`full_context_tests_say_so_in_their_name`): `*IntegrationTests` y `*SmokeTests` cargan el contexto completo y son lentos; todo lo demás es `*Tests` y no lo carga. Una clase con `@SpringBootTest` que no termina en uno de esos dos sufijos está mal nombrada.
- **Una regla de dominio** (fechas que deben caer dentro de un evento, cupos, estados permitidos) se prueba en unit, sobre el dominio o el handler. En `*IntegrationTests` queda el camino feliz y un caso de error que muestre que la regla está conectada (el 400 llega), no cada variante de la regla.
- **Un controlador** se prueba con `@WebMvcTest`: el mapeo, la validación del cuerpo, los códigos HTTP y el permiso que pide (`AuthorizeQueryHandler` simulado).
- **`@SpringBootTest` solo cuando hace falta el contexto completo**: la cadena de filtros de seguridad de punta a punta, Flyway, transacciones que cruzan módulos, consultas JPA no triviales, el outbox.
- Los datos de prueba se arman con los `*Fixtures` que ya existen en cada módulo; no copies inserts entre clases.

### Comandos

Los mismos del CI, que corre las etapas en este orden y se detiene en la primera que falla (`.github/workflows/ci.yml`):

```sh
./mvnw test -Dtest=FooTests                                        # una clase
./mvnw verify -Dtest='!*IntegrationTests,!*SmokeTests'             # 1: unit y component, formato y JaCoCo (sin base de datos)
./mvnw test -Dtest='*IntegrationTests' -Djacoco.skip=true          # 2: integración (Postgres)
./mvnw test -Dtest='*SmokeTests' -Djacoco.skip=true                # 3: smoke
./mvnw verify                                                      # todo junto, antes de abrir el PR
```

La cobertura (el piso de JaCoCo) se mide solo en la etapa 1.
