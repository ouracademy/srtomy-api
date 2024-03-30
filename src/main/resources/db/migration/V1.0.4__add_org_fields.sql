ALTER TABLE
    organization
ADD
    description text;

ALTER TABLE
    organization
ADD
    type varchar(255);

alter table
    organization drop column parent_id;

update
    organization
set
    type = 'LOCATION';