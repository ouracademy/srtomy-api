package org.ouracademy.srtomy.event.query.get_shared_event;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * What the page of a shared event shows. {@code canRegister} says the link lets this visitor sign
 * up and {@code registered} that they already did. {@code daysOfWeek} and {@code endRecur} are set
 * for a recurring event.
 */
public record GetSharedEventResult(
    String title,
    String description,
    String address,
    ZonedDateTime start,
    ZonedDateTime end,
    boolean isACourse,
    String organizationName,
    List<Integer> daysOfWeek,
    LocalDate endRecur,
    boolean canRegister,
    boolean registered) {}
