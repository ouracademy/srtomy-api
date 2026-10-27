package org.ouracademy.srtomy.outbox.command.cancel_message;

import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;

public record CancelMessageCommand(OutboxMessageId id) {}
