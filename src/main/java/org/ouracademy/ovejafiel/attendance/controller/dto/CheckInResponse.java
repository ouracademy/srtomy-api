package org.ouracademy.ovejafiel.attendance.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import org.ouracademy.ovejafiel.event.command.CheckInResult;

/**
 * Who was checked in. {@code alreadyCheckedIn} when they had been before ({@code checkedInAt} is
 * then the first time); {@code checkedInAt} is null after taking the mark back.
 */
public record CheckInResponse(
    UUID registrationId,
    String name,
    String ticketName,
    boolean alreadyCheckedIn,
    LocalDateTime checkedInAt) {

  public static CheckInResponse from(CheckInResult result) {
    return new CheckInResponse(
        result.registrationId().value(),
        result.name(),
        result.ticketName(),
        result.alreadyCheckedIn(),
        result.checkedInAt());
  }
}
