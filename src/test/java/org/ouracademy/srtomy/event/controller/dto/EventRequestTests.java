package org.ouracademy.srtomy.event.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EventRequestTests {

  private static ValidatorFactory validatorFactory;
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterAll
  static void closeValidatorFactory() {
    validatorFactory.close();
  }

  private EventRequest validRequest() {
    var request = new EventRequest();
    request.setName("Culto");
    request.setStartTime("2026-01-01T10:00:00Z");
    request.setEndTime("2026-01-01T12:00:00Z");
    request.setOrganizationId(UUID.randomUUID());
    return request;
  }

  @Test
  void aOneOffEventIsValid() {
    Set<ConstraintViolation<EventRequest>> violations = validator.validate(validRequest());

    assertThat(violations).isEmpty();
  }

  @Test
  void aKnownTimeZoneIsValid() {
    var request = validRequest();
    request.setTimeZone("America/Lima");

    assertThat(validator.validate(request)).isEmpty();
    assertThat(request.zone()).isEqualTo(ZoneId.of("America/Lima"));
  }

  @Test
  void anUnknownTimeZoneIsInvalid() {
    var request = validRequest();
    request.setTimeZone("Mars/Olympus");

    Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);

    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getPropertyPath()).hasToString("validTimeZone");
  }

  @Test
  void theTimeZoneDefaultsToUtc() {
    assertThat(validRequest().zone()).isEqualTo(ZoneId.of("UTC"));
  }
}
