-- 007-sharing: share events and courses like Drive does, with a link, and sign people up through
-- it. See specs/007-sharing/data-model.md.

-- RESTRICTED: whoever has access through the tree or was shared directly; CHURCH: any user with
-- access in the church; ANYONE_WITH_LINK: whoever has the link. link_role says what the link
-- allows: VIEWER sees the event, REGISTRANT can also sign up (courses only).
ALTER TABLE event
    ADD COLUMN general_access character varying(20) NOT NULL DEFAULT 'RESTRICTED'
        CHECK (general_access IN ('RESTRICTED', 'CHURCH', 'ANYONE_WITH_LINK')),
    ADD COLUMN link_role character varying(20) NOT NULL DEFAULT 'VIEWER'
        CHECK (link_role IN ('VIEWER', 'REGISTRANT')),
    -- Unguessable address of the shared page; a new one revokes the old link.
    ADD COLUMN share_slug character varying(32);

UPDATE event SET share_slug = substr(replace(gen_random_uuid()::text, '-', ''), 1, 20);

ALTER TABLE event
    ALTER COLUMN share_slug SET NOT NULL,
    ADD CONSTRAINT event_share_slug_key UNIQUE (share_slug);

-- Shared with a person by email. The share applies to the user whose verified email it is, even if
-- they sign up later.
CREATE TABLE event_share (
    id uuid PRIMARY KEY,
    event_id uuid NOT NULL REFERENCES event(id),
    email character varying(255) NOT NULL,
    role character varying(20) NOT NULL CHECK (role IN ('VIEWER', 'EDITOR')),
    shared_by uuid REFERENCES app_user(id),
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone
);

CREATE UNIQUE INDEX event_share_event_email_key ON event_share(event_id, lower(email));
CREATE INDEX event_share_email_idx ON event_share(lower(email));

-- Someone who signed up for an event or course through its link. It is not a person of the church
-- yet: the organizers see the list and decide.
CREATE TABLE event_registration (
    id uuid PRIMARY KEY,
    event_id uuid NOT NULL REFERENCES event(id),
    user_id uuid REFERENCES app_user(id),
    first_name character varying(255) NOT NULL,
    last_name character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    phone_number character varying(50),
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone
);

CREATE UNIQUE INDEX event_registration_event_email_key ON event_registration(event_id, lower(email));
