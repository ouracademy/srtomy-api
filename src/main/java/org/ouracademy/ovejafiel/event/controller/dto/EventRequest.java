package org.ouracademy.ovejafiel.event.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import org.ouracademy.ovejafiel.event.domain.RegistrationMode;

@Data
public class EventRequest {
  /** Name of the event or course. */
  @NotBlank String name;

  /** When it starts: ISO date and time with offset (2026-03-08T10:00:00-05:00). */
  @NotNull String startTime;

  /** When it ends: ISO date and time with offset (2026-03-08T12:00:00-05:00). */
  @NotNull String endTime;

  /** Ids of the people who speak or teach. Optional. */
  List<UUID> speakerIds = new ArrayList<>();

  /** Where it takes place. Optional. */
  String address;

  /** Id of the node (church, location, group or ministry) that holds the event. */
  @NotNull UUID organizationId;

  /** What the event is about. Optional. */
  String description;

  /** true for a course, false (the default) for an event. */
  Boolean isACourse = false;

  /** Present when the event repeats. */
  @Valid RecurrenceRequest recurrence;

  /** The container this event goes inside (a sub-event of a mega event). */
  UUID parentEventId;

  /** How a sub-event is entered; inherits the container's registration when omitted. */
  RegistrationMode registrationMode;

  /** The course, room or track of the program a sub-event belongs to; optional. */
  @Size(max = 100)
  String track;

  /** A sub-event with its own sign-up asks for a place in its mega event first. */
  Boolean requiresContainerRegistration;

  /** IANA zone the event repeats in (e.g. America/Lima); UTC when omitted. */
  String timeZone;

  @Schema(hidden = true)
  @AssertTrue(message = "La zona horaria no es válida")
  public boolean isValidTimeZone() {
    if (timeZone == null) {
      return true;
    }
    try {
      ZoneId.of(timeZone);
      return true;
    } catch (DateTimeException e) {
      return false;
    }
  }

  public ZoneId zone() {
    return timeZone == null ? ZoneId.of("UTC") : ZoneId.of(timeZone);
  }
}
