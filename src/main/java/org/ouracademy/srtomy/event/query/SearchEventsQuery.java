package org.ouracademy.srtomy.event.query;

import java.time.ZonedDateTime;
import org.ouracademy.srtomy.party.domain.PartyId;

/** Single events of an organization that overlap the {@code [start, end]} range. */
public record SearchEventsQuery(PartyId organizationId, ZonedDateTime start, ZonedDateTime end) {}
