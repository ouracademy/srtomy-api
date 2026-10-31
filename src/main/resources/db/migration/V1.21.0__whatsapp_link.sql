-- 016-whatsapp-link: a person who signed in to the portal proves that a WhatsApp number is theirs
-- by sending, from that number, a one-time code the portal showed them. From then on the bot acts
-- for them with everything their account can do (not only the Member level).

-- The code waiting to be sent. One per user: asking for another replaces it. Only its hash is kept.
CREATE TABLE whatsapp_link_code (
    user_id uuid PRIMARY KEY REFERENCES app_user(id) ON DELETE CASCADE,
    church_id uuid NOT NULL REFERENCES organization(id),
    code_hash character varying(64) NOT NULL UNIQUE,
    expires_at timestamp(6) without time zone NOT NULL,
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone
);

-- The numbers already proven. A number belongs to one account per church: proving it again with
-- another account moves it.
CREATE TABLE whatsapp_identity (
    user_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    church_id uuid NOT NULL REFERENCES organization(id),
    phone_digits character varying(30) NOT NULL,
    verified_at timestamp(6) without time zone NOT NULL,
    PRIMARY KEY (user_id, church_id),
    UNIQUE (church_id, phone_digits)
);

-- Wrong codes sent from a number, to stop anyone from guessing: only a few are allowed per hour.
CREATE TABLE whatsapp_link_attempt (
    phone_digits character varying(30) NOT NULL,
    attempted_at timestamp(6) without time zone NOT NULL
);
CREATE INDEX whatsapp_link_attempt_idx ON whatsapp_link_attempt (phone_digits, attempted_at);
