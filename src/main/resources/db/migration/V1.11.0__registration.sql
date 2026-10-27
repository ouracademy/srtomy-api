-- 011-registration: signing up is its own process (tickets, seats, waitlist), not a role of the
-- share link. See specs/011-registration/data-model.md.
--
-- The product owner asked to replace, not migrate, what 007 stored: the sign-ups made through a
-- course link and the REGISTRANT link role are dropped.
DROP TABLE event_registration;

ALTER TABLE event DROP COLUMN link_role;

-- An event that has this row accepts sign-ups; one without it has no registration.
CREATE TABLE event_registration_setup (
    id uuid PRIMARY KEY,
    event_id uuid NOT NULL UNIQUE REFERENCES event(id),
    open boolean NOT NULL DEFAULT true,
    requires_account boolean NOT NULL DEFAULT false,
    waitlist_enabled boolean NOT NULL DEFAULT false,
    -- Total seats for the event; null is unlimited.
    capacity integer CHECK (capacity > 0),
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone
);

-- A kind of ticket. A free event has one with price 0. The price is informational: nothing is
-- charged yet.
CREATE TABLE ticket_type (
    id uuid PRIMARY KEY,
    event_id uuid NOT NULL REFERENCES event(id),
    name character varying(100) NOT NULL,
    price_cents integer NOT NULL DEFAULT 0 CHECK (price_cents >= 0),
    currency character varying(3) NOT NULL DEFAULT 'PEN',
    -- Seats of this ticket; null is unlimited.
    quantity integer CHECK (quantity > 0),
    sales_start timestamp(6) with time zone,
    sales_end timestamp(6) with time zone,
    position integer NOT NULL DEFAULT 0,
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone,
    CHECK (sales_start IS NULL OR sales_end IS NULL OR sales_start < sales_end)
);

CREATE UNIQUE INDEX ticket_type_event_name_key ON ticket_type(event_id, lower(name));

-- Someone with a seat (CONFIRMED), waiting for one (WAITLISTED) or who gave it up (CANCELLED, kept
-- as history). cancel_code lets them cancel without an account; ticket_code is what the ticket QR
-- carries, so whoever scans it can check them in but not cancel.
CREATE TABLE event_registration (
    id uuid PRIMARY KEY,
    event_id uuid NOT NULL REFERENCES event(id),
    ticket_type_id uuid NOT NULL REFERENCES ticket_type(id),
    user_id uuid REFERENCES app_user(id),
    first_name character varying(255) NOT NULL,
    last_name character varying(255) NOT NULL,
    email character varying(255) NOT NULL,
    phone_number character varying(50),
    status character varying(20) NOT NULL
        CHECK (status IN ('CONFIRMED', 'WAITLISTED', 'CANCELLED')),
    cancel_code character varying(32) NOT NULL UNIQUE,
    ticket_code character varying(32) NOT NULL UNIQUE,
    checked_in_at timestamp(6) without time zone,
    check_in_source character varying(20)
        CHECK (check_in_source IN ('MANUAL', 'QR_EVENT', 'QR_TICKET')),
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone
);

CREATE UNIQUE INDEX event_registration_active_email_key
    ON event_registration(event_id, lower(email)) WHERE status <> 'CANCELLED';
CREATE INDEX event_registration_user_idx ON event_registration(user_id);
CREATE INDEX event_registration_ticket_idx ON event_registration(ticket_type_id);
