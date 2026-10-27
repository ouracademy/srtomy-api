package org.ouracademy.ovejafiel.outbox.command.cancel_message;

import org.ouracademy.ovejafiel.outbox.domain.OutboxMessageId;

public record CancelMessageCommand(OutboxMessageId id) {}
