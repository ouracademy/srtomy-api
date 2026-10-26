package org.ouracademy.srtomy.outbox.command.enqueue_message;

import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;

public record EnqueueMessageResult(OutboxMessageId id) {}
