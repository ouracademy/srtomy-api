-- 014-whatsapp-connection: the event reminders already queued, one per registration, so a second
-- run of the daily job (or a retry of it) never sends the same reminder twice.
CREATE TABLE whatsapp_reminder (
    registration_id uuid PRIMARY KEY REFERENCES event_registration(id) ON DELETE CASCADE,
    queued_at timestamp(6) without time zone NOT NULL
);
