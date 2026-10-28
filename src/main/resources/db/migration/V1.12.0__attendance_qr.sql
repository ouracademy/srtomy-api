-- 011-registration: attendance by QR code and visitors. See specs/011-registration/data-model.md.

-- Visitors who gave no name (or whom the usher did not write down) are only counted.
ALTER TABLE event_attendance
    ADD COLUMN guest_count integer NOT NULL DEFAULT 0 CHECK (guest_count >= 0);

-- Where each mark came from, and who made it (null when the person marked themselves).
ALTER TABLE person_event_attendance
    ADD COLUMN source character varying(20) NOT NULL DEFAULT 'MANUAL'
        CHECK (source IN ('MANUAL', 'QR_EVENT', 'QR_TICKET')),
    ADD COLUMN marked_by uuid REFERENCES app_user(id);
