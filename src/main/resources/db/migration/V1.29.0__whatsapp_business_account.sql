-- 014-whatsapp-connection: the WhatsApp Business Account of the church, needed to create its message
-- templates (they belong to the account, not to the number). Known once the number is connected.
ALTER TABLE whatsapp_connection ADD COLUMN business_account_id character varying(100);
