package org.ouracademy.ovejafiel.outbox.command.attempt_delivery;

import org.ouracademy.ovejafiel.outbox.domain.OutboxMessageId;

public record AttemptDeliveryCommand(OutboxMessageId id) {}
