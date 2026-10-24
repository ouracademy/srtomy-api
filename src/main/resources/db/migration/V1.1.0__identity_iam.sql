-- 003-identity-iam: users, GCP-style access roles inherited down the organization tree, and
-- ministerial positions owned by each church. See specs/003-identity-iam/data-model.md.

-- No environment holds real data yet (confirmed by the product owner), so every domain row is
-- dropped, including the 'LCDP' seed church, instead of being migrated.
DELETE FROM person_event_attendance;
DELETE FROM event_attendance;
DELETE FROM event_speakers;
UPDATE event SET recurrent_event_id = NULL;
DELETE FROM event;
DELETE FROM accountability;
DELETE FROM accountability_type WHERE category = 'ROLE';
DELETE FROM person;
DELETE FROM organization;
DELETE FROM party;

-- Organization tree: the parent link becomes a column so ancestors can be walked with a
-- recursive CTE at any depth.
ALTER TABLE organization
    ADD COLUMN parent_id uuid REFERENCES organization(id),
    ADD COLUMN website character varying(500),
    ADD COLUMN facebook_url character varying(500),
    ADD COLUMN instagram_url character varying(500),
    ADD COLUMN youtube_url character varying(500),
    ADD COLUMN tiktok_url character varying(500);

CREATE INDEX organization_parent_id_idx ON organization(parent_id);

-- Ministerial positions (cargos) are labels owned by a church; they grant no access.
UPDATE accountability_type SET category = 'POSITION' WHERE category = 'ROLE';

CREATE UNIQUE INDEX accountability_type_position_name_uq
    ON accountability_type(party_id, lower(name))
    WHERE category = 'POSITION' AND deleted_time IS NULL;

CREATE TABLE app_user (
    id uuid PRIMARY KEY,
    auth0_sub character varying(255) NOT NULL UNIQUE,
    email character varying(320),
    email_verified boolean NOT NULL DEFAULT false,
    person_id uuid REFERENCES person(id),
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone
);

CREATE TABLE iam_role (
    id uuid PRIMARY KEY,
    name character varying(100) NOT NULL,
    description text,
    church_id uuid REFERENCES organization(id),
    permissions text[] NOT NULL,
    deleted_time timestamp(6) without time zone,
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone
);

CREATE TABLE role_binding (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES app_user(id),
    role_id uuid NOT NULL REFERENCES iam_role(id),
    organization_id uuid NOT NULL REFERENCES organization(id),
    granted_by uuid REFERENCES app_user(id),
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone,
    UNIQUE (user_id, role_id, organization_id)
);

CREATE INDEX role_binding_organization_id_idx ON role_binding(organization_id);

-- Predefined roles. Their ids are fixed UUIDs hardcoded in
-- org.ouracademy.srtomy.iam.domain.PredefinedRoles.
INSERT INTO
    iam_role (id, name, description, permissions, create_date, last_modified_date)
VALUES
    ('0195f0a0-0000-7000-8000-000000000001', 'Propietario',
     'Control total de la iglesia, incluida la gestión de accesos.',
     ARRAY['org.read', 'org.update', 'org.create_child', 'org.delete', 'members.read',
           'members.write', 'events.read', 'events.write', 'attendance.write',
           'positions.manage', 'iam.read', 'iam.manage'], now(), now()),
    ('0195f0a0-0000-7000-8000-000000000002', 'Administrador',
     'Administra la iglesia o el nodo: estructura, personas, eventos, cargos y accesos.',
     ARRAY['org.read', 'org.update', 'org.create_child', 'org.delete', 'members.read',
           'members.write', 'events.read', 'events.write', 'attendance.write',
           'positions.manage', 'iam.read', 'iam.manage'], now(), now()),
    ('0195f0a0-0000-7000-8000-000000000003', 'Líder',
     'Gestiona su sede, grupo o ministerio y todo lo que cuelga de él.',
     ARRAY['org.read', 'org.update', 'org.create_child', 'members.read', 'members.write',
           'events.read', 'events.write', 'attendance.write', 'iam.read'], now(), now()),
    ('0195f0a0-0000-7000-8000-000000000004', 'Colaborador',
     'Consulta personas y eventos, y toma asistencia.',
     ARRAY['org.read', 'members.read', 'events.read', 'attendance.write'], now(), now()),
    ('0195f0a0-0000-7000-8000-000000000005', 'Miembro',
     'Ve la información básica y los eventos.',
     ARRAY['org.read', 'events.read'], now(), now());
