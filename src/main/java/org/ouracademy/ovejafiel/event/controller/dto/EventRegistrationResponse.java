package org.ouracademy.ovejafiel.event.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.query.list_event_registrations.EventRegistrationResult;

public record EventRegistrationResponse(
    UUID id,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    boolean hasAccount,
    String ticketName,
    RegistrationStatus status,
    LocalDateTime registeredAt,
    LocalDateTime checkedInAt,
    CheckInSource checkInSource) {

  public static EventRegistrationResponse from(EventRegistrationResult result) {
    return new EventRegistrationResponse(
        result.id().value(),
        result.firstName(),
        result.lastName(),
        result.email(),
        result.phoneNumber(),
        result.hasAccount(),
        result.ticketName(),
        result.status(),
        result.registeredAt(),
        result.checkedInAt(),
        result.checkInSource());
  }
}
