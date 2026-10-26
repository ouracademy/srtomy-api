package org.ouracademy.srtomy.outbox.command.enqueue_message;

/**
 * Saves a message in the caller's transaction: it exists if and only if the caller's change does.
 */
public interface EnqueueMessageCommandHandler {
  EnqueueMessageResult handle(EnqueueMessageCommand command);
}
