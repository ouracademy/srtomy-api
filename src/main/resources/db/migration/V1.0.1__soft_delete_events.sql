ALTER TABLE event ADD COLUMN deleted_time timestamp(6) without time zone;
ALTER TABLE event_attendance ADD COLUMN deleted_time timestamp(6) without time zone;
