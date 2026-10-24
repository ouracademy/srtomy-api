-- 004b-positions-with-access: a position (cargo) carries permissions, from roles and/or loose
-- ones, and a user gets access through the positions of the person that represents them. See
-- specs/004b-positions-with-access/data-model.md.

-- No real data exists: direct grants other than Owner give way to positions.
DELETE FROM role_binding WHERE role_id <> '0195f0a0-0000-7000-8000-000000000001';

-- Finer predefined roles, to build positions from. Their ids are hardcoded in PredefinedRoles.
INSERT INTO
    iam_role (id, name, description, permissions, create_date, last_modified_date)
VALUES
    ('0195f0a0-0000-7000-8000-000000000006', 'Ver personas',
     'Ve las personas.',
     ARRAY['org.read', 'members.read'], now(), now()),
    ('0195f0a0-0000-7000-8000-000000000007', 'Gestor de personas',
     'Ve, crea y edita personas.',
     ARRAY['org.read', 'members.read', 'members.write'], now(), now()),
    ('0195f0a0-0000-7000-8000-000000000008', 'Gestor de eventos',
     'Ve, crea y edita eventos.',
     ARRAY['org.read', 'events.read', 'events.write'], now(), now()),
    ('0195f0a0-0000-7000-8000-000000000009', 'Asistencia',
     'Ve eventos y toma asistencia.',
     ARRAY['org.read', 'events.read', 'attendance.write'], now(), now()),
    ('0195f0a0-0000-7000-8000-00000000000a', 'Gestor de estructura',
     'Edita los datos del lugar y crea grupos y ministerios.',
     ARRAY['org.read', 'org.update', 'org.create_child'], now(), now());

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

-- What a position gives: the permissions of its roles (linked, so editing a role changes every
-- position that uses it) plus loose permissions. The effective access is the union.
CREATE TABLE position_role (
    position_id uuid NOT NULL REFERENCES accountability_type(id),
    role_id uuid NOT NULL REFERENCES iam_role(id),
    PRIMARY KEY (position_id, role_id)
);

CREATE TABLE position_permission (
    position_id uuid NOT NULL REFERENCES accountability_type(id),
    permission character varying(100) NOT NULL,
    PRIMARY KEY (position_id, permission)
);

-- Every access a user holds on a node, whatever its source, with the permissions it gives.
-- Callers inherit it down the tree. Accesses that give nothing are left out.
CREATE VIEW user_access AS
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
       'LEADER',
       CAST(NULL AS uuid),
       CAST(NULL AS character varying),
       r.id,
       r.permissions
FROM user_person up
JOIN accountability a ON a.child_id = up.person_id AND a.deleted_time IS NULL
     AND a.type = '6e0cebd8-4a7a-4ac5-b9dd-658c23708672'
JOIN iam_role r ON r.id = '0195f0a0-0000-7000-8000-000000000003' AND r.deleted_time IS NULL;
