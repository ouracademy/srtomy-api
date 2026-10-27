package org.ouracademy.srtomy.outbox;

import org.ouracademy.srtomy.outbox.domain.OutboxChannel;
import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;

/** Delivers the messages of one channel through its provider. */
public interface OutboxMessageHandler {

  OutboxChannel channel();

  /**
   * Must be safe to repeat: a message is delivered at least once. The id is stable, so it can be
   * used as the idempotency key of the provider.
   *
   * @return the id the provider gave the message, or null when it gives none
   * @throws PermanentDeliveryException when trying again cannot work (the provider refuses the
   *     message or the recipient)
   * @throws RuntimeException for any other failure; the queue calls again later
   */
  String deliver(OutboxMessageId id, String recipient, String payload);
}
