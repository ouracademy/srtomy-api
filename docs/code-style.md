# Estructura por módulos de dominio

El código vive bajo `org.ouracademy.ovejafiel.<modulo>`. Los módulos actuales son:
`party`, `person`, `organization`, `accountability`, `event`, `attendance`,
`complaint`, `user`, `iam`, `invitation`, `notification`, `directory`, y `shared` (no es un módulo de dominio, ver más abajo).

Son paquetes Java dentro del mismo proyecto Maven/JAR — no hay `module-info.java`
ni multi-módulo Maven. La regla de aislamiento entre módulos no la impone el
compilador: la impone `src/test/java/org/ouracademy/ovejafiel/ArchitectureTests.java`
(ArchUnit), que corre como parte de `./mvnw test`. Esa clase es la fuente de
verdad ejecutable de las reglas de este documento — si algo acá y el test
difieren, gana el test (y hay que corregir uno de los dos).

## Estructura interna de un módulo

```
<modulo>/
├── domain/              # entidades JPA, records de id tipados, excepciones de dominio
├── command/
│   ├── <accion>/                            # una subcarpeta por acción, en snake_case
│   │   ├── <Accion>Command.java            # record, solo datos
│   │   ├── <Accion>CommandHandler.java      # interfaz, único método handle(...)
│   │   ├── <Accion>CommandHandlerImpl.java  # @Service, implementación
│   │   └── <Accion>Result.java              # DTO de salida
│   └── <Helper>.java                        # solo si lo usa más de una acción
├── query/
│   ├── <consulta>/
│   │   ├── <Consulta>Query.java
│   │   ├── <Consulta>QueryHandler.java      # interfaz, único método handle(...)
│   │   ├── <Consulta>QueryHandlerImpl.java
│   │   └── <Consulta>Result.java
│   └── <Helper>.java
├── repository/             # Spring Data JPA, usado SOLO dentro del propio módulo
└── controller/
    ├── <Modulo>Controller.java
    └── dto/                 # Request/Response del endpoint HTTP
```

Dentro de `command/` y `query/`, cada acción tiene su propia subcarpeta con
sus 3-4 archivos. El nombre de la subcarpeta sale del nombre simple de la
clase `Command`/`Query` sin el sufijo, pasado de UpperCamelCase a
snake_case: `CreateChurchCommand` → `command/create_church/`,
`FindOrganizationByIdQuery` → `query/find_organization_by_id/`. Un `Result`
usado por una sola acción vive en la carpeta de esa acción aunque no se
llame `<Accion>Result` (ej. `MemberStateResult` en
`organization/query/get_member_state/`).

Excepción: una clase usada por más de una acción del mismo `command/` o
`query/` (un `Result` compartido o un helper tipo `*Info`/`*Writer`/
`*Resolver`) queda en la raíz de `command/`/`query/`, no dentro de la
carpeta de una de ellas, y es `public` para que las subcarpetas la vean.
Ejemplos reales: `organization/command/OrganizationWriter`,
`organization/query/PersonDetailsResolver`,
`accountability/query/AccountabilityTypeResult`,
`person/query/FindPersonResult`. Los `*HandlerImpl` siguen siendo
package-private, así que sus tests viven en la misma subcarpeta (un test por
acción) y una acción que usa otra depende de su `*Handler` (interfaz), nunca
de su `*HandlerImpl`.

Para módulos que llaman servicios externos se usa `gateway/` en vez de
`repository/`, con la misma idea: la llamada externa queda encapsulada y solo
se usa dentro de su propio módulo.

## Autorización

Cada endpoint que toca datos de una iglesia llama primero a
`iam.query.authorize.AuthorizeQueryHandler` con el `sub` del JWT, el nodo
(iglesia, sede, grupo o ministerio) sobre el que actúa y el `Permission` que
necesita. Si el endpoint recibe un evento, una persona o un cargo, primero
resuelve su nodo con el `QueryHandler` del módulo dueño. Sin permiso, el
handler lanza `AccessDeniedException` y la respuesta es 403. Los roles se
heredan hacia abajo por `organization.parent_id`, como en IAM de GCP. Ver
`specs/003-identity-iam/`.

## Naming — sin excepciones

- Nada de `UseCase` ni `Interactor` en ningún nombre de clase.
- Escrituras: `<Accion>Command` + `<Accion>CommandHandler`. Lecturas:
  `<Consulta>Query` + `<Consulta>QueryHandler`.
- El único método de cualquier handler se llama `handle(...)`. Nunca
  `execute`, `process`, `run` ni otra variante.
- Todo `handle(...)` de un Command/QueryHandler devuelve un tipo cuyo nombre
  termina en `Result` (nunca `Response`/`Output`, y nunca una entidad
  `@Entity` directamente). Puede estar envuelto en `Optional<...Result>` o en
  una colección (`List<...Result>`) — eso sí está permitido.
- `repository/`/`gateway/` no necesitan una interfaz "port" adicional — ya son
  la implementación concreta, y las usan directo los Handlers de su propio
  módulo.

## Comunicación entre módulos — regla estricta

La única forma en que un módulo puede comunicarse con otro es invocando el
`CommandHandler`/`QueryHandler` público del otro módulo. Ningún módulo puede
importar el `repository`, `gateway` o `controller` de otro módulo.

Las relaciones de objeto de dominio dentro del mismo módulo (atributos JPA
tipo FK) se mantienen sin cambios. Para relaciones que CRUZAN módulos: el
módulo que necesita el dato llama al `QueryHandler` del módulo dueño para
validar existencia (el `QueryHandler` devuelve un `Result`, nunca la
entidad), y luego usa `EntityManager.getReference(OtraEntidad.class, id)`
para fijar la referencia JPA al persistir — nunca el `repository` del otro
módulo (eso rompería el aislamiento). Ejemplos reales: `event` validando
`organizationId` vía `organization.FindOrganizationByIdQueryHandler` y luego
usando `EntityManager.getReference(Organization.class, id)`;
`accountability` haciendo lo mismo con `party.domain.Party` para sus
relaciones `child`/`parent` genéricas.

### Excepciones documentadas (las únicas permitidas)

1. **Herencia de clase**: extender `shared.domain.*` (ej. `DomainObject`) o
   `party.domain.Party` directamente desde cualquier módulo está permitido —
   son clases base, no lógica de negocio. `Person`/`Organization extends
   Party` es el ejemplo real.
2. **Tipo concreto exigido por JPA**: un campo `@ManyToOne`/`@OneToOne`/
   `@OneToMany` puede tipar contra la clase `@Entity` concreta de otro
   módulo (bajo su paquete `..domain..`), porque JPA lo exige — pero nunca
   contra su `repository`/`gateway`/`controller`. Ejemplo:
   `event.domain.Event.organization` es de tipo
   `organization.domain.Organization`.
3. **JPQL de solo lectura entre tablas**: una `@Query` JPQL dentro del propio
   `repository` de un módulo puede unir (`join`/`TREAT`) contra la tabla de
   otro módulo para una lectura (ej. `person.repository.PersonRepository`
   uniendo `Accountability`), siempre que no importe la *clase* repository
   del otro módulo — solo sus entidades de dominio. Es una concesión
   pragmática de rendimiento/lectura, no una forma general de saltarse la
   regla de arriba.

Estas tres excepciones son exactamente lo que `ArchitectureTests` permite al
no restringir el sub-paquete `domain`. Cualquier otra forma de cruzar
módulos (incluyendo violaciones temporales durante una migración) se
registra explícitamente en el mapa `PENDING_MIGRATION_EXCEPTIONS` de ese
test, con un comentario de qué paso la resuelve — nunca queda implícita.

## Lo compartido

- Clases base comunes (`DomainObject`, `EntityId`, `IdGenerator`,
  `EntityIdJavaType`, `EntityIdTypeContributor`) van en `shared/domain/`.
- Configuración global de Spring y manejo global de excepciones van en
  `shared/` (ej. `SecurityConfig`, `GlobalExceptionHandler`,
  `WebConfiguration`, `OvejafielApplication`).
- `shared` no es un módulo de dominio: no tiene `command`/`query`/
  `controller` propios con reglas de aislamiento — es la base que todos los
  módulos pueden importar libremente.

## Entity ids

Cada entidad JPA tiene su propia clase de id tipada (un `record implements
EntityId`, ej. `PartyId`, `EventId`), no un `UUID`/`Long` crudo ni un id
genérico compartido entre entidades no relacionadas. La clase de id vive
junto a su entidad, en `<modulo>/domain/`, no en un paquete compartido. Ver
`org.ouracademy.ovejafiel.party.domain.PartyId` (y su entidad `Party`) para el
patrón, y `org.ouracademy.ovejafiel.shared.domain` para el plumbing compartido
(`EntityId`, `IdGenerator`, `EntityIdJavaType`, `EntityIdTypeContributor`)
del que depende cada tipo de id. Todos los ids son UUIDv7, generados en Java
vía un field initializer `@Id XxxId id = XxxId.generate();` — sin
`@GeneratedValue`, sin `AttributeConverter` (Hibernate los rechaza en campos
`@Id`).
