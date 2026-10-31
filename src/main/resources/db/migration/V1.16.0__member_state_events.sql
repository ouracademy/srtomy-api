-- The history of how involved each person is with each node (church, location, group, ministry):
-- every time someone joins a node, changes state there (visitor -> attendee -> member) or leaves it,
-- one row is kept. The current state is still the open accountability; this table is what lets a
-- church see how it grows. from_state is null when the person joined; to_state is null when they left.
CREATE TABLE member_state_event (
    id uuid PRIMARY KEY,
    person_id uuid NOT NULL REFERENCES party(id),
    organization_id uuid NOT NULL REFERENCES party(id),
    from_state character varying(30),
    to_state character varying(30),
    occurred_at timestamp(6) without time zone NOT NULL DEFAULT now(),
    CONSTRAINT member_state_event_changes CHECK (from_state IS DISTINCT FROM to_state)
);

CREATE INDEX member_state_event_organization_idx
    ON member_state_event (organization_id, occurred_at);
CREATE INDEX member_state_event_person_idx
    ON member_state_event (person_id);

-- There is no real data yet, but keep what the tables already say: whoever is in a node today joined it.
INSERT INTO member_state_event (id, person_id, organization_id, from_state, to_state, occurred_at)
SELECT gen_random_uuid(), a.child_id, a.parent_id, NULL, t.name,
       coalesce(a.create_date, now())
FROM accountability a
JOIN accountability_type t ON t.id = a.type
WHERE t.category = 'MEMBER_PROGRESS_STATE'
  AND a.deleted_time IS NULL;
