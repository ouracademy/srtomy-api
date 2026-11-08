package org.ouracademy.ovejafiel.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

class DateRangeTests {

  @Test
  void coversFromTheFirstInstantOfTheStartDayToTheLastOfTheEndDay() {
    var range = new DateRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2));

    assertThat(range.start()).isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
    assertThat(range.end()).isEqualTo(Instant.parse("2026-10-02T23:59:59.999999999Z"));
  }

  @ParameterizedTest
  @CsvSource({
    "2026-10-01, 2026-10-01, DAY",
    "2026-10-01, 2026-10-31, DAY",
    "2026-10-01, 2026-11-01, WEEK",
    "2026-10-01, 2027-01-28, WEEK",
    "2026-10-01, 2027-01-29, MONTH"
  })
  void choosesTheBucketFromTheLengthOfTheRange(LocalDate start, LocalDate end, BucketUnit bucket) {
    assertThat(new DateRange(start, end).bucket()).isEqualTo(bucket);
  }

  @Test
  void rejectsAStartAfterTheEnd() {
    var start = LocalDate.of(2026, 10, 5);
    var end = LocalDate.of(2026, 10, 1);

    assertThatThrownBy(() -> new DateRange(start, end))
        .isInstanceOf(DomainValidationException.class);
  }
}
