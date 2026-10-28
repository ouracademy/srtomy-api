-- 011b-registration-simplified: one permission for each thing that can be done with the
-- registration of an event, and the seats of an event are only the quantity of its tickets.

-- Who could manage the events (events.write) could manage their registration; they keep doing so.
-- Roles of any kind, predefined or created by the church.
UPDATE iam_role
SET permissions = permissions
    || ARRAY(
        SELECT p
        FROM unnest(ARRAY['registrations.read', 'registrations.configure',
                          'registrations.close', 'registrations.remove']) AS p
        WHERE NOT (p = ANY (permissions)))
WHERE 'events.write' = ANY (permissions);

-- The same for the permissions a position carries on its own (outside any role).
INSERT INTO position_permission (position_id, permission)
SELECT pp.position_id, p.permission
FROM position_permission pp
CROSS JOIN (VALUES ('registrations.read'), ('registrations.configure'),
                   ('registrations.close'), ('registrations.remove')) AS p(permission)
WHERE pp.permission = 'events.write'
ON CONFLICT DO NOTHING;

-- The total seats of an event is no longer asked for: with one ticket it is the quantity of that
-- ticket. (With several, the old total keeps limiting, since a ticket cannot say it.)
UPDATE ticket_type t
SET quantity = LEAST(COALESCE(t.quantity, s.capacity), s.capacity)
FROM event_registration_setup s
WHERE t.event_id = s.event_id
  AND s.capacity IS NOT NULL
  AND (SELECT count(*) FROM ticket_type x WHERE x.event_id = s.event_id) = 1;

UPDATE event_registration_setup s
SET capacity = NULL
WHERE s.capacity IS NOT NULL
  AND (SELECT count(*) FROM ticket_type x WHERE x.event_id = s.event_id) = 1;

-- The waitlist is not offered for now (nobody is told when a seat frees up); whoever already waits
-- keeps their place and is still promoted when a seat frees up.
UPDATE event_registration_setup SET waitlist_enabled = false;
