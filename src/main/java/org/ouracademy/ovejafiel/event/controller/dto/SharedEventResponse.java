package org.ouracademy.ovejafiel.event.controller.dto;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import org.ouracademy.ovejafiel.event.query.get_shared_event.GetSharedEventResult;

/** The page of a shared event: what it is, where and when, and what the visitor can do. */
public record SharedEventResponse(
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
        result.daysOfWeek(),
        result.endRecur(),
        result.canRegister(),
        result.registered());
  }
}
