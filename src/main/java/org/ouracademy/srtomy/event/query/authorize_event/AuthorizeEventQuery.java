package org.ouracademy.srtomy.event.query.authorize_event;

import org.ouracademy.srtomy.event.domain.AccessLevel;
import org.ouracademy.srtomy.event.domain.EventId;

/** Asks whether {@code subject} can do at least {@code required} with the event. */
public record AuthorizeEventQuery(String subject, EventId eventId, AccessLevel required) {}
