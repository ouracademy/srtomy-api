-- 006-visibility: who sees the people of a church, group or ministry, a directory of the church
-- and a public page per node. See specs/006-visibility/data-model.md.

-- PUBLIC: anyone with the link; CHURCH: any user with access in the church; MEMBERS: the members
-- of the node and whoever can read its people; LEADERS: only whoever can read its people.
ALTER TABLE organization
    ADD COLUMN member_visibility character varying(20) NOT NULL DEFAULT 'PUBLIC'
        CHECK (member_visibility IN ('PUBLIC', 'CHURCH', 'MEMBERS', 'LEADERS')),
    ADD COLUMN show_org_chart boolean NOT NULL DEFAULT true,
    ADD COLUMN schedule text,
    -- Unguessable address of the public page; the page is the link, there is no listing.
    ADD COLUMN slug character varying(32);

UPDATE organization SET slug = substr(replace(gen_random_uuid()::text, '-', ''), 1, 20);

ALTER TABLE organization
    ALTER COLUMN slug SET NOT NULL,
    ADD CONSTRAINT organization_slug_key UNIQUE (slug);
