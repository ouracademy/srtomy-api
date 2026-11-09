-- 014-whatsapp-connection: why a number stopped working, so the portal can say what to do. Null while
-- connected (and for connections made before this column existed).
ALTER TABLE whatsapp_connection ADD COLUMN disconnect_reason character varying(40);
