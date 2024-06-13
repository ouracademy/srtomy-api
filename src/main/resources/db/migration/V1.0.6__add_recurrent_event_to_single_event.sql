ALTER TABLE
    public.event
add
    column recurrent_event_id int8;

alter table
    public.event
add
    constraint recurrent_event_fk foreign key (recurrent_event_id) references public.event (id);