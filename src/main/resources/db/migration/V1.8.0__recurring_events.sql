-- 009-recurring-events-calendar-sync: events repeat like in Google Calendar (every N days, weeks,
-- months or years; ending never, on a date or after N times) and each user gets a private calendar
-- feed. There is no production data yet, so the old model (single-table inheritance with a
-- recurrent parent row, weekly weekdays and a mandatory end date) is replaced, not migrated.

TRUNCATE TABLE person_event_attendance, event_attendance, event_registration, event_share,
    event_speakers, event CASCADE;

ALTER TABLE event
    DROP COLUMN event_type_id,
    DROP COLUMN days_of_week,
    DROP COLUMN recurrent_event_id,
    DROP COLUMN end_recur;

-- The rule of a repeating event. Its occurrences are rows of event (series_id) created ahead up to
-- a rolling horizon: materialized_until, extended by a scheduled job while the series never ends.
CREATE TABLE event_series (
    id uuid PRIMARY KEY,
    name character varying(255),
    address character varying(255),
    description character varying(255),
    is_a_course boolean,
    organization_id uuid NOT NULL REFERENCES organization(id),
    start_local timestamp(6) without time zone NOT NULL,
    duration_seconds bigint NOT NULL,
    time_zone character varying(64) NOT NULL,
    frequency character varying(10) NOT NULL
        CHECK (frequency IN ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY')),
    recur_interval integer NOT NULL CHECK (recur_interval >= 1),
    -- Weekly only, 0 = Sunday ... 6 = Saturday; empty means the weekday of the first occurrence.
    days_of_week smallint[],
    monthly_mode character varying(20)
        CHECK (monthly_mode IN ('DAY_OF_MONTH', 'NTH_WEEKDAY')),
    end_type character varying(10) NOT NULL CHECK (end_type IN ('NEVER', 'UNTIL', 'COUNT')),
    until_date date,
    occurrence_count integer,
    materialized_until date NOT NULL,
    fully_materialized boolean NOT NULL DEFAULT false,
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone,
    deleted_time timestamp(6) without time zone
);

CREATE INDEX event_series_to_extend_idx ON event_series(materialized_until)
    WHERE fully_materialized = false AND deleted_time IS NULL;

CREATE TABLE event_series_speakers (
    event_series_id uuid NOT NULL REFERENCES event_series(id),
    speakers_id uuid NOT NULL REFERENCES person(id)
);

ALTER TABLE event ADD COLUMN series_id uuid REFERENCES event_series(id);
CREATE INDEX event_series_id_idx ON event(series_id, start_time);
CREATE INDEX event_start_time_idx ON event(start_time);

-- Private address of the calendar feed of a user (Google Calendar, Apple Calendar...). Calendar
-- apps cannot send a token header, so the token travels in the URL; a new one revokes the old.
CREATE TABLE calendar_feed (
    user_id uuid PRIMARY KEY REFERENCES app_user(id),
    token character varying(32) NOT NULL,
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone,
    CONSTRAINT calendar_feed_token_key UNIQUE (token)
);
