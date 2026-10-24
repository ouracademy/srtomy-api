package org.ouracademy.srtomy.event.query.find_event_organization;

import org.ouracademy.srtomy.event.domain.EventId;

/** The node an event (single or recurrent) belongs to, used to authorize access to it. */
public record FindEventOrganizationQuery(EventId eventId) {}
