# Implementation Plan: Visibilidad

**Spec**: [spec.md](./spec.md)

## Diseño

1. **`organization`**: `MemberVisibility`, `showOrgChart`, `schedule` y `slug` (`Slugs.random`) en la entidad; `OrganizationInfo` y las peticiones los aceptan; `OrganizationResponse` los devuelve.
2. **`directory`** (módulo nuevo, solo lectura):
   - `DirectoryRepository` (JPQL de lectura sobre `Accountability` y `Person`, permitido por la excepción 3 de `docs/code-style.md`).
   - `DirectoryReader` arma nombres con el filtro de edad (`AgeRules`): `Audience.CHURCH` omite a los menores conocidos y `Audience.PUBLIC` solo nombra a los adultos conocidos.
   - `ChurchAccess`: pertenecer a la iglesia (algún acceso en su árbol), permisos por nodo y ser miembro de un nodo (persona vinculada con alguna acreditación allí).
   - Consultas `get_church_directory`, `get_directory_entry`, `get_public_page`; `DirectoryController`.
3. La ruta `GET /v1/public/**` ya es pública desde la 005.

## Constitution Check

| Principio | Cómo |
| --- | --- |
| II. Tests | Pruebas unitarias de cada consulta y del controlador; integración sobre el esquema real (visibilidad, menores, organigrama). |
| III. Seguridad | Slug aleatorio de 96 bits; nunca se devuelven datos sensibles de personas; la regla de visibilidad se evalúa en cada petición. |
| IV. Código en inglés | Sí. |
| V. Simplicidad | Sin caché ni tablas nuevas. |

## Endpoints

Ver [contracts/http.md](./contracts/http.md).
