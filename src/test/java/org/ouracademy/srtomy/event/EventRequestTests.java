package org.ouracademy.srtomy.event;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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
    request.setOrganizationId(1L);
    return request;
  }

  @Test
  void singleEventWithoutDaysOfWeekIsValid() {
    var request = validRequest();

    Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);

    assertThat(violations).isEmpty();
  }

  @Test
  void recurrentEventWithoutDaysOfWeekIsInvalid() {
    var request = validRequest();
    request.setEndRecur(LocalDate.of(2026, 12, 31));

    Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);

    assertThat(violations).hasSize(1);
    assertThat(violations.iterator().next().getPropertyPath()).hasToString("validDaysOfWeek");
  }

  @Test
  void recurrentEventWithDaysOfWeekIsValid() {
    var request = validRequest();
    request.setEndRecur(LocalDate.of(2026, 12, 31));
    request.setDaysOfWeek(List.of(1, 3));

    Set<ConstraintViolation<EventRequest>> violations = validator.validate(request);

    assertThat(violations).isEmpty();
  }
}
