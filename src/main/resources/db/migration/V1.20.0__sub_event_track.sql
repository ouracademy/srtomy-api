-- 012-composite-events: a sub-event can say which course, room or track of the program it belongs
-- to ("Curso de liderazgo", "Sala 2"), so the program can group the sessions. Free text, optional.
ALTER TABLE event ADD COLUMN track character varying(100);
