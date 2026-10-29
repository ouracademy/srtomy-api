package org.ouracademy.ovejafiel.event.command.edit_event;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.ouracademy.ovejafiel.event.domain.EventChangeOption;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.Recurrence;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/**
 * Edits an event and, depending on {@code option}, the rest of its series (see {@code
 * specs/013-edit-events}). {@code organizationId} and {@code isACourse} cannot change: they are
 * here only to reject a request that tries. A null {@code recurrence} leaves the rule as it is, a
 * null {@code timeZone} keeps the zone of the series. {@code now} splits past from future
 * occurrences.
 */
public record EditEventCommand(
    EventId id,
    EventChangeOption option,
    String name,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    String address,
    String description,
    Boolean isACourse,
    PartyId organizationId,
    List<PartyId> speakerIds,
    Recurrence recurrence,
    ZoneId timeZone,
    Instant now) {}
