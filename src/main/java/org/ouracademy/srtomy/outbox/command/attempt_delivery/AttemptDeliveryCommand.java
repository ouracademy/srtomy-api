package org.ouracademy.srtomy.outbox.command.attempt_delivery;

import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;

public record AttemptDeliveryCommand(OutboxMessageId id) {}
