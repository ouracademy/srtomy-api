-- 012-composite-events: notices of an event, shown on its page for whoever can open it, with or
-- without an account (not everyone reads email). Written by the system when an event changes or a
-- sign-up stops counting, and by its organizers as announcements.
CREATE TABLE event_notice (
    id uuid PRIMARY KEY,
    event_id uuid NOT NULL REFERENCES event(id),
    kind character varying(20) NOT NULL
        CHECK (kind IN ('ANNOUNCEMENT', 'CHANGED', 'CANCELLED', 'REGISTRATION_CHANGED')),
    message text NOT NULL,
    created_at timestamp(6) without time zone NOT NULL
);

CREATE INDEX event_notice_event_idx ON event_notice(event_id, created_at DESC);
