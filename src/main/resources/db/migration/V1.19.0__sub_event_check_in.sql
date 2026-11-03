-- 012-composite-events: the entry of someone who signed up to a mega event, recorded in each
-- sub-event that inherits the sign-up. The sign-up itself (event_registration.checked_in_at) keeps
-- meaning "came to the mega event". One row per sign-up and sub-event.
CREATE TABLE event_registration_check_in (
    id uuid PRIMARY KEY,
    registration_id uuid NOT NULL REFERENCES event_registration(id) ON DELETE CASCADE,
    event_id uuid NOT NULL REFERENCES event(id),
    checked_in_at timestamp(6) without time zone NOT NULL,
    check_in_source character varying(20) NOT NULL
        CHECK (check_in_source IN ('MANUAL', 'QR_EVENT', 'QR_TICKET')),
    CONSTRAINT event_registration_check_in_key UNIQUE (registration_id, event_id)
);

CREATE INDEX event_registration_check_in_event_idx ON event_registration_check_in(event_id);
