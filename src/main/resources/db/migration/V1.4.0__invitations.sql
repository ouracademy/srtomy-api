-- 005-invitations: a person may have an email, and a leader can invite someone by email to use the
-- app. Accepting links the invited user to a person and gives them a position. See
-- specs/005-invitations/data-model.md.

ALTER TABLE person ADD COLUMN email character varying(255);

-- Who may invite: Owner, Administrator and Leader. Positions already created keep their own copy
-- of permissions, so they do not change.
UPDATE iam_role
SET permissions = array_append(permissions, 'invitations.send')
WHERE id IN ('0195f0a0-0000-7000-8000-000000000001',
             '0195f0a0-0000-7000-8000-000000000002',
             '0195f0a0-0000-7000-8000-000000000003')
  AND NOT ('invitations.send' = ANY (permissions));

CREATE TABLE invitation (
    id uuid PRIMARY KEY,
    email character varying(255) NOT NULL,
    organization_id uuid NOT NULL REFERENCES organization(id),
    -- The position (cargo) the invited person will hold in the node. Null: only links the account.
    position_id uuid REFERENCES accountability_type(id),
    -- An existing person to link; null creates one from first_name and last_name when accepted.
    person_id uuid REFERENCES person(id),
    first_name character varying(255),
    last_name character varying(255),
    -- Only the SHA-256 of the token is stored; the token itself travels in the email link.
    token_hash character varying(64) NOT NULL UNIQUE,
    status character varying(20) NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    invited_by uuid NOT NULL REFERENCES app_user(id),
    accepted_by uuid REFERENCES app_user(id),
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone,
    CHECK (status IN ('PENDING', 'ACCEPTED', 'REVOKED'))
);

CREATE INDEX invitation_organization_id_idx ON invitation(organization_id);
CREATE INDEX invitation_email_idx ON invitation(lower(email));
