package org.ouracademy.ovejafiel.outbox.command.enqueue_message;

import org.ouracademy.ovejafiel.outbox.domain.OutboxMessageId;

public record EnqueueMessageResult(OutboxMessageId id) {}
