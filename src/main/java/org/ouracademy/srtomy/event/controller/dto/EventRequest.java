package org.ouracademy.srtomy.event.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Data;

@Data
public class EventRequest {
  @NotBlank String name;

  @NotNull String startTime;

  @NotNull String endTime;

  List<UUID> speakerIds = new ArrayList<>();

  String address;

  @NotNull UUID organizationId;

  String description;

  Boolean isACourse = false;

  /** Present when the event repeats. */
  @Valid RecurrenceRequest recurrence;

  /** IANA zone the event repeats in (e.g. America/Lima); UTC when omitted. */
  String timeZone;

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
