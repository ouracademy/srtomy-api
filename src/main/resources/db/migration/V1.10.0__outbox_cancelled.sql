-- A message whose reason is gone (an invitation that was cancelled or replaced) is CANCELLED while
-- it still waits, so the email with a dead link never goes out.
ALTER TABLE outbox_message DROP CONSTRAINT outbox_message_status_check;
ALTER TABLE outbox_message
    ADD CONSTRAINT outbox_message_status_check
    CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CANCELLED'));
