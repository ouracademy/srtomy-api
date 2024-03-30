create table accountability (
    id serial,
    person_id int8 NOT NULL,
    organization_id int8 NOT NULL,
    type varchar(250),
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone,
    CONSTRAINT accountability_pkey PRIMARY KEY (id)
);

ALTER TABLE
    accountability
ADD
    CONSTRAINT accountability_person_id_fk FOREIGN KEY (person_id) REFERENCES person(id);

ALTER TABLE
    accountability
ADD
    CONSTRAINT accountability_organization_id_fk FOREIGN KEY (organization_id) REFERENCES organization(id);

insert into
    accountability (person_id, organization_id, type)
select
    id,
    organization_id,
    'VISITOR'
from
    person;

alter table
    person drop column organization_id;