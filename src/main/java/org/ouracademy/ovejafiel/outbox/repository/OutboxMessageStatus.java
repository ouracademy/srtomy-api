package org.ouracademy.ovejafiel.outbox.repository;

import org.ouracademy.ovejafiel.outbox.domain.OutboxMessageId;
import org.ouracademy.ovejafiel.outbox.domain.OutboxStatus;

/** Read straight from the database, never from an entity the request may have cached. */
public record OutboxMessageStatus(OutboxMessageId id, OutboxStatus status) {}
