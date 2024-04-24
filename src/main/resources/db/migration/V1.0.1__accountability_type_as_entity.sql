create table accountability_type (
    id serial,
    name varchar(255) NOT NULL party_id bigint,
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone,
    deleted_time timestamp(6) without time zone
);

ALTER TABLE
    accountability_type
ADD
    CONSTRAINT accountability_type_party_fk FOREIGN KEY (party_id) REFERENCES party(id);

INSERT INTO
    accountability_type (name, create_date, last_modified_date)
values
    ('VISITOR', now(), now()),
    ('MEMBERSHIP_IN_PROGRESS', now(), now()),
    ('MEMBER', now(), now()),
    ('IS_PART_OF', now(), now()),
    ('LEADER', now(), now()),
    ('PASTOR', now(), now()),
    ('WORSHIP_LEADER', now(), now()),
    ('USHER', now(), now());

alter table
    accountability
add
    column type_temp bigint;

alter table
    accountability
add
    constraint accountability_accountability_type_fk foreign key (type_temp) references accountability_type(id);

update
    accountability a
set
    type_temp = t.id
from
    accountability_type t
where
    t.name = a.type;

alter table
    accountability drop column type;

alter table
    accountability rename column type_temp to type;

-- soft delete
alter table
    accountability
add
    column deleted_time datetime;