-- The old LEADER accountability is gone: leading a group or ministry is a position (cargo) like
-- any other, and a position already gives its permissions in the node and below (the POSITION
-- branch of user_access). The view loses its LEADER branch and the rows and type are removed.
CREATE OR REPLACE VIEW user_access AS
SELECT rb.user_id AS user_id,
       rb.organization_id AS organization_id,
       'DIRECT' AS source,
       rb.id AS binding_id,
       CAST(NULL AS character varying) AS position_name,
       r.id AS role_id,
       r.permissions AS permissions
FROM role_binding rb
JOIN iam_role r ON r.id = rb.role_id AND r.deleted_time IS NULL
UNION ALL
SELECT up.user_id,
       a.parent_id,
       'POSITION',
       CAST(NULL AS uuid),
       t.name,
       CAST(NULL AS uuid),
       ARRAY(
           SELECT x.permission FROM (
               SELECT unnest(r.permissions) AS permission
               FROM position_role pr
               JOIN iam_role r ON r.id = pr.role_id AND r.deleted_time IS NULL
               WHERE pr.position_id = t.id
               UNION
               SELECT pp.permission FROM position_permission pp WHERE pp.position_id = t.id
           ) x ORDER BY x.permission)
FROM user_person up
JOIN accountability a ON a.child_id = up.person_id AND a.deleted_time IS NULL
JOIN accountability_type t ON t.id = a.type
     AND t.category = 'POSITION'
     AND t.deleted_time IS NULL
WHERE EXISTS (SELECT 1 FROM position_permission pp WHERE pp.position_id = t.id)
   OR EXISTS (SELECT 1 FROM position_role pr
              JOIN iam_role r ON r.id = pr.role_id AND r.deleted_time IS NULL
              WHERE pr.position_id = t.id)
UNION ALL
SELECT up.user_id,
       a.parent_id,
       'MEMBER',
       CAST(NULL AS uuid),
       CAST(NULL AS character varying),
       r.id,
       r.permissions
FROM user_person up
JOIN accountability a ON a.child_id = up.person_id AND a.deleted_time IS NULL
     AND a.type = '1d6e887a-9cd0-461e-b11d-c5d357497ce2'
JOIN iam_role r ON r.id = '0195f0a0-0000-7000-8000-000000000005' AND r.deleted_time IS NULL;

DELETE FROM accountability
WHERE type = '6e0cebd8-4a7a-4ac5-b9dd-658c23708672';

DELETE FROM accountability_type
WHERE id = '6e0cebd8-4a7a-4ac5-b9dd-658c23708672';
