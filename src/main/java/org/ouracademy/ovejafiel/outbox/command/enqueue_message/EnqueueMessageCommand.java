package org.ouracademy.ovejafiel.outbox.command.enqueue_message;

import org.ouracademy.ovejafiel.outbox.domain.OutboxChannel;

/**
 * {@code dispatchNow} false only for a bulk of messages (a fan-out job): they are saved without a
 * task and the sweep ({@code DispatchPendingCommand}) schedules them.
 */
public record EnqueueMessageCommand(
    OutboxChannel channel, String type, String recipient, String payload, boolean dispatchNow) {

  public EnqueueMessageCommand(
      OutboxChannel channel, String type, String recipient, String payload) {
    this(channel, type, recipient, payload, true);
  }
}
