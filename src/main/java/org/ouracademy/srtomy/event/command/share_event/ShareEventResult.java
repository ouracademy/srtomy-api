package org.ouracademy.srtomy.event.command.share_event;

import org.ouracademy.srtomy.event.query.EventSharingResult;
import org.ouracademy.srtomy.outbox.domain.OutboxMessageId;

/** The email is queued with the share; ask the outbox with {@code emailMessageId} how it went. */
public record ShareEventResult(EventSharingResult.Share share, OutboxMessageId emailMessageId) {}
