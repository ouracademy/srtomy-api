# Decisiones de arquitectura y producto (ADR)

Cada archivo guarda algo que el dueño del producto **decidió no hacer** (o hacer de cierta forma), con su contexto y su motivo, para que nadie lo vuelva a proponer sin saber por qué se descartó. Lo pendiente vive en [`../tech-debt-and-future-work.md`](../tech-debt-and-future-work.md).

Formato: `NNNN-titulo-corto.md` con Estado, Contexto, Decisión, Motivo y Consecuencias. Los números no se reutilizan. Si una decisión cambia, se agrega un ADR nuevo que reemplaza al anterior y el viejo pasa a «Reemplazada por NNNN».

| #    | Decisión                                                                       |
| ---- | ------------------------------------------------------------------------------ |
| 0001 | [Sin zona horaria por organización](0001-no-organization-timezone.md)          |
| 0002 | [No mover todo un mega evento con sus sub-eventos](0002-no-move-whole-mega-event.md) |
| 0003 | [No repetir mega eventos](0003-no-repeating-mega-events.md)                    |
