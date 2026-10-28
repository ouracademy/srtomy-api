-- 011c-no-waitlist: the waitlist and the total of seats of an event are gone. Nobody told who waited
-- when a seat freed up, so the waitlist did not work; the seats are the quantity of each ticket.

-- A total that V1.13.0 could not move to a ticket (the event has several) now limits each ticket:
-- none may sell more than the old total.
UPDATE ticket_type t
SET quantity = LEAST(COALESCE(t.quantity, s.capacity), s.capacity)
FROM event_registration_setup s
WHERE t.event_id = s.event_id
  AND s.capacity IS NOT NULL;

-- Whoever waited loses the place (they never got a seat); like any cancelled sign-up it stays as
-- history and the email can sign up again.
UPDATE event_registration SET status = 'CANCELLED' WHERE status = 'WAITLISTED';

ALTER TABLE event_registration DROP CONSTRAINT event_registration_status_check;
ALTER TABLE event_registration
    ADD CONSTRAINT event_registration_status_check CHECK (status IN ('CONFIRMED', 'CANCELLED'));

ALTER TABLE event_registration_setup
    DROP COLUMN waitlist_enabled,
    DROP COLUMN capacity;
