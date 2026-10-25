package org.ouracademy.srtomy.event.query.get_shared_event;

import java.time.ZonedDateTime;
import org.ouracademy.srtomy.event.query.RecurrenceResult;

/**
 * What the page of a shared event shows. {@code canRegister} says the link lets this visitor sign
 * up and {@code registered} that they already did. {@code recurrence} is set for a repeating event.
 */
public record GetSharedEventResult(
    String title,
    String description,
    String address,
    ZonedDateTime start,
    ZonedDateTime end,
    boolean isACourse,
    String organizationName,
    RecurrenceResult recurrence,
    boolean canRegister,
    boolean registered) {}
