-- A person removed from the church is kept (their attendance and past positions are history); the
-- time says when. What makes a person visible in a node is an open accountability: removing someone
-- closes them (accountability.deleted_time) instead of deleting rows.
ALTER TABLE person ADD COLUMN deleted_time timestamp(6) without time zone;

-- Deleting people is its own permission. Whoever could delete the organization keeps it for people.
UPDATE iam_role
SET permissions = permissions || ARRAY['members.delete']
WHERE 'org.delete' = ANY (permissions)
  AND NOT ('members.delete' = ANY (permissions));

INSERT INTO position_permission (position_id, permission)
SELECT pp.position_id, 'members.delete'
FROM position_permission pp
WHERE pp.permission = 'org.delete'
ON CONFLICT DO NOTHING;
