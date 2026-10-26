-- Outbox: work against systems outside the database (emails) is saved in the same transaction as
-- the change that asked for it and done afterwards, with retries. See "Operaciones con sistemas
-- externos" in docs/architecture.md.
CREATE TABLE outbox_message (
    id uuid PRIMARY KEY,
    type character varying(50) NOT NULL,
    payload text NOT NULL,
    status character varying(20) NOT NULL,
    attempts integer NOT NULL DEFAULT 0,
    next_attempt_at timestamp(6) without time zone NOT NULL,
    last_error character varying(1000),
    sent_at timestamp(6) without time zone,
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone,
    CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX outbox_message_due_idx ON outbox_message(next_attempt_at) WHERE status = 'PENDING';

-- The email of an invitation, to show whether it left. Null for invitations sent before the outbox.
ALTER TABLE invitation ADD COLUMN email_message_id uuid REFERENCES outbox_message(id);
