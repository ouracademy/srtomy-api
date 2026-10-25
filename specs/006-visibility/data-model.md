# Data model: Visibilidad

Migración `V1.5.0__visibility.sql`, sobre `organization`:

| Columna | Detalle |
| --- | --- |
| `member_visibility` | `PUBLIC` (por defecto), `CHURCH`, `MEMBERS`, `LEADERS` (CHECK). |
| `show_org_chart` | boolean, `true` por defecto. |
| `schedule` | texto libre, nulo. |
| `slug` | único, 20 caracteres hexadecimales en los nodos existentes y 16 URL-safe aleatorios en los nuevos. |
