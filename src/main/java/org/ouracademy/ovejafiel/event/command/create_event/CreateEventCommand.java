package org.ouracademy.ovejafiel.event.command.create_event;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.ouracademy.ovejafiel.event.domain.Recurrence;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Creates a one-off event, or a series (when {@code recurrence} is set) with its first occurrences.
 * {@code timeZone} is the zone the series repeats in; it is ignored for a one-off event.
 */
public record CreateEventCommand(
    String name,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    String address,
    String description,
    Boolean isACourse,
    PartyId organizationId,
    List<PartyId> speakerIds,
    Recurrence recurrence,
    ZoneId timeZone) {}
