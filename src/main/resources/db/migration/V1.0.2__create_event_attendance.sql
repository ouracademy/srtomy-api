drop table attendance;

CREATE TABLE event_attendance (
    id int8 NOT NULL,
    number_of_visitors int8,
    visitors_description text,
    date_time timestamp(6) NOT NULL,
    CONSTRAINT event_attendance_pkey PRIMARY KEY (id)
);

ALTER TABLE
    event_attendance
ADD
    CONSTRAINT event_attendance_id_fk FOREIGN KEY (id) REFERENCES public.event(id);

create table person_event_attendance (
    event_attendance_id int8 NOT NULL,
    person_id int8 NOT NULL,
    date_time timestamp(6) NOT NULL,
    CONSTRAINT person_event_attendance_pkey PRIMARY KEY (event_attendance_id, person_id)
);

ALTER TABLE
    person_event_attendance
ADD
    CONSTRAINT person_event_attendance_person_id_fk FOREIGN KEY (person_id) REFERENCES person(id);

ALTER TABLE
    person_event_attendance
ADD
    CONSTRAINT person_event_attendance_event_attendance_id_fk FOREIGN KEY (event_attendance_id) REFERENCES event_attendance(id);