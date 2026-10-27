package org.ouracademy.srtomy.outbox.command.enqueue_message;

/**
 * Saves a message in the caller's transaction and schedules its delivery. It exists if and only if
 * the caller's change does: when the delivery cannot be scheduled this throws, and the caller's
 * transaction rolls back.
 */
public interface EnqueueMessageCommandHandler {
  EnqueueMessageResult handle(EnqueueMessageCommand command);
}
