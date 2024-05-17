ALTER TABLE
    event
add
    column event_type_id smallint default 1;

ALTER TABLE
    event
add
    column days_of_week smallint[];