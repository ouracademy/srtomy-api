-- 004b-positions-with-access: a position (cargo) carries an access level, and a user gets access
-- through the positions of the person that represents them. See
-- specs/004b-positions-with-access/data-model.md.

-- No real data exists: direct grants other than Owner give way to positions.
DELETE FROM role_binding WHERE role_id <> '0195f0a0-0000-7000-8000-000000000001';

ALTER TABLE accountability_type ADD COLUMN iam_role_id uuid REFERENCES iam_role(id);

-- One column cannot hold one person per church, so the reserved (unused) column is replaced by
-- a link table.
ALTER TABLE app_user DROP COLUMN person_id;

CREATE TABLE user_person (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES app_user(id),
    person_id uuid NOT NULL UNIQUE REFERENCES person(id),
    church_id uuid NOT NULL REFERENCES organization(id),
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone,
    UNIQUE (user_id, church_id)
);

-- Every access a user holds on a node, whatever its source. Callers inherit it down the tree.
CREATE VIEW user_access AS
SELECT rb.user_id AS user_id,
       rb.role_id AS role_id,
       rb.organization_id AS organization_id,
       'DIRECT' AS source,
       rb.id AS binding_id,
       CAST(NULL AS character varying) AS position_name
FROM role_binding rb
UNION ALL
SELECT up.user_id,
       t.iam_role_id,
       a.parent_id,
       'POSITION',
       CAST(NULL AS uuid),
       t.name
FROM user_person up
JOIN accountability a ON a.child_id = up.person_id AND a.deleted_time IS NULL
JOIN accountability_type t ON t.id = a.type
     AND t.category = 'POSITION'
     AND t.deleted_time IS NULL
     AND t.iam_role_id IS NOT NULL
UNION ALL
SELECT up.user_id,
       CAST('0195f0a0-0000-7000-8000-000000000003' AS uuid),
       a.parent_id,
       'LEADER',
       CAST(NULL AS uuid),
       CAST(NULL AS character varying)
FROM user_person up
JOIN accountability a ON a.child_id = up.person_id AND a.deleted_time IS NULL
     AND a.type = '6e0cebd8-4a7a-4ac5-b9dd-658c23708672';
