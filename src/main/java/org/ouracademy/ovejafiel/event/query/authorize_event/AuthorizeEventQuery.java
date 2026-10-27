package org.ouracademy.ovejafiel.event.query.authorize_event;

import org.ouracademy.ovejafiel.event.domain.AccessLevel;
import org.ouracademy.ovejafiel.event.domain.EventId;

/** Asks whether {@code subject} can do at least {@code required} with the event. */
public record AuthorizeEventQuery(String subject, EventId eventId, AccessLevel required) {}
