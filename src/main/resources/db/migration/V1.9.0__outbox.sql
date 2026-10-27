-- Outbox: work against systems outside the database (emails today, WhatsApp later) is saved in the
-- same transaction as the change that asked for it, and done afterwards by a Cloud Tasks task that
-- calls back the API (retries are the queue's). See "Trabajo programado y sistemas externos" in
-- docs/code-style.md.
CREATE TABLE outbox_message (
    id uuid PRIMARY KEY,
    -- How it is delivered (picks the handler and the queue): EMAIL, WHATSAPP.
    channel character varying(20) NOT NULL,
    -- What it is about, for people reading the table: INVITATION, EVENT_SHARE, BIRTHDAY...
    type character varying(50) NOT NULL,
    recipient character varying(255) NOT NULL,
    -- What to send. May hold a secret (the link of an invitation), so it is erased once the
    -- message is SENT or FAILED.
    payload text,
    status character varying(20) NOT NULL,
    attempts integer NOT NULL DEFAULT 0,
    last_error character varying(1000),
    -- The id the provider gave it, to match its delivery receipts later.
    provider_message_id character varying(255),
    sent_at timestamp(6) without time zone,
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone,
    CHECK (channel IN ('EMAIL', 'WHATSAPP')),
    CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

-- The daily sweep looks for what never got a task.
CREATE INDEX outbox_message_pending_idx ON outbox_message(last_modified_date) WHERE status = 'PENDING';

-- The email of an invitation, to show whether it left. Null for invitations sent before the outbox.
ALTER TABLE invitation ADD COLUMN email_message_id uuid REFERENCES outbox_message(id);
