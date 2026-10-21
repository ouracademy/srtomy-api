package org.ouracademy.srtomy.event.command.create_event;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.ouracademy.srtomy.party.domain.PartyId;

/**
 * Creates a single event, or a recurrent one (when {@code recurrent}) together with its expanded
 * single events between {@code startTime} and {@code endRecur}.
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
    List<Integer> daysOfWeek,
    LocalDate endRecur,
    boolean recurrent) {}
