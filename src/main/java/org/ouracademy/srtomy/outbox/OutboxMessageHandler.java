package org.ouracademy.srtomy.outbox;

import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;

/**
 * Does the work of one kind of outbox message. A module that needs something done outside the
 * database implements this and enqueues messages of its {@link #type()}.
 */
public interface OutboxMessageHandler {

  String type();

  /**
   * Must be safe to repeat: a message is delivered at least once. The id is stable, so it can be
   * used as the idempotency key of the provider.
   *
   * @throws RuntimeException when it did not work; the message is retried later
   */
  void handle(OutboxMessageId id, String payload);
}
