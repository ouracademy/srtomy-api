package org.ouracademy.srtomy.event.controller.dto;

import java.time.ZonedDateTime;
import org.ouracademy.srtomy.event.query.get_shared_event.GetSharedEventResult;

/** The page of a shared event: what it is, where and when, and what the visitor can do. */
public record SharedEventResponse(
    String title,
    String description,
    String address,
    ZonedDateTime start,
    ZonedDateTime end,
    boolean isACourse,
    String organizationName,
    RecurrenceResponse recurrence,
    boolean canRegister,
    boolean registered) {

  public static SharedEventResponse from(GetSharedEventResult result) {
    return new SharedEventResponse(
        result.title(),
        result.description(),
        result.address(),
        result.start(),
        result.end(),
        result.isACourse(),
        result.organizationName(),
        RecurrenceResponse.from(result.recurrence()),
        result.canRegister(),
        result.registered());
  }
}
