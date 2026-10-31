-- Whether the person accepts WhatsApp notices from their church (spec 014). On by default; it is
-- turned off when the person asks to stop or is edited. Notices are only queued when it is true.
ALTER TABLE person ADD COLUMN whatsapp_consent boolean NOT NULL DEFAULT true;
