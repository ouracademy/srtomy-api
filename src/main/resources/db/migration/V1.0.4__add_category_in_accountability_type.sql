ALTER TABLE
    accountability_type
add
    column category varchar(50);

update
    accountability_type
set
    category = 'ROLE'
where
    id in (6, 7, 8);

update
    accountability_type
set
    category = 'MEMBER_PROGRESS_STATE'
where
    id in (1, 2, 3);