CREATE TABLE complaint (
    id serial,
    first_name varchar(255) NOT NULL,
    last_name varchar(255) NOT NULL,
    dni varchar(255) NOT NULL,
    address varchar(255),
    email varchar(255) NOT NULL,
    comment text,
    
    create_date timestamp(6) without time zone,
    last_modified_date timestamp(6) without time zone
);