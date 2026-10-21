-- Initial schema, squashed to use UUID (v7, assigned by the application) primary keys
-- instead of autoincremental bigint/serial ids. There is no data in any environment yet,
-- so this replaces the whole migration history instead of converting column types in place.

CREATE TABLE party (
    id uuid PRIMARY KEY,
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone,
    address text,
    description text,
    name character varying(255),
    phone_number character varying(255)
);

CREATE TABLE person (
    id uuid PRIMARY KEY REFERENCES party(id),
    age integer,
    birthdate date,
    has_been_baptized boolean,
    first_name character varying(255),
    last_name character varying(255)
);

CREATE TABLE organization (
    id uuid PRIMARY KEY REFERENCES party(id),
    type character varying(255)
);

CREATE TABLE accountability_type (
    id uuid PRIMARY KEY,
    name character varying(255) NOT NULL,
    party_id uuid REFERENCES party(id),
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone,
    deleted_time timestamp(6) without time zone,
    category character varying(50)
);

CREATE TABLE accountability (
    id uuid PRIMARY KEY,
    child_id uuid NOT NULL REFERENCES party(id),
    parent_id uuid NOT NULL REFERENCES party(id),
    type uuid REFERENCES accountability_type(id),
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone,
    deleted_time timestamp(6) without time zone
);

CREATE TABLE event (
    id uuid PRIMARY KEY,
    event_type_id smallint DEFAULT 1,
    name character varying(255),
    start_time timestamp(6) with time zone,
    end_time timestamp(6) with time zone,
    address character varying(255),
    description character varying(255),
    organization_id uuid NOT NULL REFERENCES organization(id),
    is_a_course boolean,
    days_of_week smallint[],
    recurrent_event_id uuid REFERENCES event(id),
    end_recur date,
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone
);

CREATE TABLE event_speakers (
    event_id uuid NOT NULL REFERENCES event(id),
    speakers_id uuid NOT NULL REFERENCES person(id)
);

CREATE TABLE event_attendance (
    id uuid PRIMARY KEY REFERENCES event(id),
    date_time timestamp(6) without time zone NOT NULL
);

CREATE TABLE person_event_attendance (
    event_attendance_id uuid NOT NULL REFERENCES event_attendance(id),
    person_id uuid NOT NULL REFERENCES person(id),
    date_time timestamp(6) without time zone,
    PRIMARY KEY (event_attendance_id, person_id)
);

CREATE TABLE complaint (
    id uuid PRIMARY KEY,
    first_name character varying(255) NOT NULL,
    last_name character varying(255) NOT NULL,
    dni character varying(255),
    address character varying(255),
    email character varying(255) NOT NULL,
    comment text,
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone
);

-- System-defined accountability types. Their ids are fixed UUIDs hardcoded in
-- org.ouracademy.srtomy.person.SystemAccountabilityTypes.
INSERT INTO
    accountability_type (id, name, create_date, last_modified_date, category)
VALUES
    ('9c89bc94-38b6-4ff6-a08e-2c8f308b462f', 'VISITOR', now(), now(), 'MEMBER_PROGRESS_STATE'),
    ('2e3de685-c713-4a33-b862-27672d0710de', 'MEMBERSHIP_IN_PROGRESS', now(), now(), 'MEMBER_PROGRESS_STATE'),
    ('7ed35dab-dad3-42ca-a3f8-e90a10e1681c', 'MEMBER', now(), now(), 'MEMBER_PROGRESS_STATE'),
    ('1d6e887a-9cd0-461e-b11d-c5d357497ce2', 'IS_PART_OF', now(), now(), NULL),
    ('6e0cebd8-4a7a-4ac5-b9dd-658c23708672', 'LEADER', now(), now(), NULL),
    ('e8a28a06-4560-4077-aa3c-79838b581ef1', 'PASTOR', now(), now(), 'ROLE'),
    ('c10a7a54-1ed1-4773-aa61-c3526ac09740', 'WORSHIP_LEADER', now(), now(), 'ROLE'),
    ('b504cabb-c35d-4e92-9048-972ae69ce88f', 'USHER', now(), now(), 'ROLE');

-- seed root organization
INSERT INTO
    party (id, create_date, last_modified_date, name)
VALUES
    ('bb8d09a1-ce40-496a-9c89-f52ab7b3d7b8', now(), now(), 'LCDP');

INSERT INTO
    organization (id, type)
VALUES
    ('bb8d09a1-ce40-496a-9c89-f52ab7b3d7b8', 'CHURCH');
