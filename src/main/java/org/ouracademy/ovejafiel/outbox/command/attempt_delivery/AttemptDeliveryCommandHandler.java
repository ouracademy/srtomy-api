package org.ouracademy.ovejafiel.outbox.command.attempt_delivery;

public interface AttemptDeliveryCommandHandler {
  AttemptDeliveryResult handle(AttemptDeliveryCommand command);
}
