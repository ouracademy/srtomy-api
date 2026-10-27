package org.ouracademy.ovejafiel.event.query.search_events;

import java.time.ZonedDateTime;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/** Single events of an organization that overlap the {@code [start, end]} range. */
public record SearchEventsQuery(PartyId organizationId, ZonedDateTime start, ZonedDateTime end) {}
