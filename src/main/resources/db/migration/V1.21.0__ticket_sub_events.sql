-- 012-composite-events: a ticket of a mega event can give access to only some of its sub-events
-- ("Pase sábado"). A ticket with no rows here gives access to the whole program.
CREATE TABLE ticket_type_sub_event (
    ticket_type_id uuid NOT NULL REFERENCES ticket_type(id) ON DELETE CASCADE,
    event_id uuid NOT NULL REFERENCES event(id),
    PRIMARY KEY (ticket_type_id, event_id)
);
