package org.ouracademy.ovejafiel.event.command.share_event;

import org.ouracademy.ovejafiel.event.query.EventSharingResult;
import org.ouracademy.ovejafiel.outbox.domain.OutboxMessageId;

/** The email is queued with the share; ask the outbox with {@code emailMessageId} how it went. */
public record ShareEventResult(EventSharingResult.Share share, OutboxMessageId emailMessageId) {}
