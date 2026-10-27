package org.ouracademy.srtomy.outbox.gateway;

import org.ouracademy.srtomy.outbox.domain.OutboxChannel;
import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;

/**
 * Schedules the delivery of a message: something will call {@code POST
 * /v1/internal/outbox/{id}/attempt} soon, and again if it fails.
 */
public interface TaskGateway {

  /**
   * Creating the same task twice is not an error. Called inside the transaction that saves the
   * message: if it throws, nothing is saved.
   *
   * @throws RuntimeException when the task could not be created
   */
  void schedule(OutboxMessageId id, OutboxChannel channel);
}
