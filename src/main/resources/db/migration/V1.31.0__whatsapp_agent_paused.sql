-- 014-whatsapp-connection: the pastor can pause the assistant of the church (the number stays
-- connected, the church answers by hand from the WhatsApp Business app) and resume it later.
ALTER TABLE whatsapp_connection ADD COLUMN agent_paused boolean NOT NULL DEFAULT false;
