-- The renewal reminders already sent (spec 018, phase 4): one per church, paid period and kind, so
-- a daily job that runs twice, or is retried, never sends the same reminder twice.
CREATE TABLE billing_reminder (
    organization_id uuid NOT NULL REFERENCES organization(id),
    -- The last paid day the reminder is about; a new payment moves it and the reminders start over.
    paid_until date NOT NULL,
    kind character varying(20) NOT NULL,
    sent_at timestamp(6) without time zone NOT NULL,
    PRIMARY KEY (organization_id, paid_until, kind),
    CHECK (kind IN ('SEVEN_DAYS', 'ONE_DAY', 'GRACE'))
);
