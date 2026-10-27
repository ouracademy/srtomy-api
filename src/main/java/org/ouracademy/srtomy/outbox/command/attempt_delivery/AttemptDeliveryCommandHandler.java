package org.ouracademy.srtomy.outbox.command.attempt_delivery;

public interface AttemptDeliveryCommandHandler {
  AttemptDeliveryResult handle(AttemptDeliveryCommand command);
}
