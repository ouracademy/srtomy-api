-- 012-composite-events: a sub-event with its own sign-up (and its own seats and price) can ask for
-- the sign-up of its mega event first: only who already holds a place there can take one here.
ALTER TABLE event ADD COLUMN requires_container_registration boolean NOT NULL DEFAULT false;
