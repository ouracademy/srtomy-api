package org.ouracademy.ovejafiel.outbox.command.cancel_message;

/**
 * Withdraws a message that has not left yet: its task will find it done and send nothing. Call it
 * in the transaction of the change that makes the message pointless (a cancelled invitation).
 */
public interface CancelMessageCommandHandler {
  CancelMessageResult handle(CancelMessageCommand command);
}
