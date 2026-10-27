package org.ouracademy.ovejafiel.event.query.find_event_organization;

import org.ouracademy.ovejafiel.event.domain.EventId;

/** The node an event (single or recurrent) belongs to, used to authorize access to it. */
public record FindEventOrganizationQuery(EventId eventId) {}
