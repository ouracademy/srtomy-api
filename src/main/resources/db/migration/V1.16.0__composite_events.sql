-- 012-composite-events: an event can contain other events and courses (a three-day conference with
-- short events and courses inside). One level only: parent_event_id points to the container and a
-- child cannot have children. Additive: events without a parent behave as before.
--
-- registration_mode only matters for a child. INHERIT: the ticket of the parent's registration is
-- valid. OWN: the child has its own registration setup and tickets. OPEN: no registration needed.
ALTER TABLE event
    ADD COLUMN parent_event_id uuid REFERENCES event(id),
    ADD COLUMN registration_mode character varying(7) NOT NULL DEFAULT 'INHERIT'
        CHECK (registration_mode IN ('INHERIT', 'OWN', 'OPEN'));

CREATE INDEX event_parent_id_idx ON event(parent_event_id, start_time);
