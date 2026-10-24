package org.ouracademy.srtomy.event.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import org.ouracademy.srtomy.event.query.list_event_registrations.EventRegistrationResult;

public record EventRegistrationResponse(
    UUID id,
    String firstName,
    String lastName,
    String email,
    String phoneNumber,
    boolean hasAccount,
    LocalDateTime registeredAt) {

  public static EventRegistrationResponse from(EventRegistrationResult result) {
    return new EventRegistrationResponse(
        result.id().value(),
        result.firstName(),
        result.lastName(),
        result.email(),
        result.phoneNumber(),
        result.hasAccount(),
        result.registeredAt());
  }
}
