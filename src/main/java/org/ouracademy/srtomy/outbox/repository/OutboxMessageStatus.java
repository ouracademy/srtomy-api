package org.ouracademy.srtomy.outbox.repository;

import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;
import org.ouracademy.srtomy.outbox.domain.OutboxStatus;

/** Read straight from the database, never from an entity the request may have cached. */
public record OutboxMessageStatus(OutboxMessageId id, OutboxStatus status) {}
