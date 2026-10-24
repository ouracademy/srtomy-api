package org.ouracademy.srtomy.directory.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AgeRulesTests {

  LocalDate today = LocalDate.of(2026, 10, 24);

  @Test
  void aBirthdateUnder18IsAKnownMinor() {
    assertThat(AgeRules.isKnownMinor(null, LocalDate.of(2008, 10, 25), today)).isTrue();
    assertThat(AgeRules.isKnownAdult(null, LocalDate.of(2008, 10, 25), today)).isFalse();
  }

  @Test
  void turning18TodayMakesThemAnAdult() {
    var birthdate = LocalDate.of(2008, 10, 24);

    assertThat(AgeRules.isKnownAdult(null, birthdate, today)).isTrue();
    assertThat(AgeRules.isKnownMinor(null, birthdate, today)).isFalse();
  }

  @Test
  void theBirthdateWinsOverAStaleAge() {
    assertThat(AgeRules.isKnownMinor(30, LocalDate.of(2015, 1, 1), today)).isTrue();
    assertThat(AgeRules.isKnownAdult(10, LocalDate.of(1990, 1, 1), today)).isTrue();
  }

  @Test
  void withoutABirthdateTheStoredAgeCounts() {
    assertThat(AgeRules.isKnownMinor(17, null, today)).isTrue();
    assertThat(AgeRules.isKnownAdult(18, null, today)).isTrue();
  }

  @Test
  void anUnknownAgeIsNeitherMinorNorKnownAdult() {
    assertThat(AgeRules.isKnownMinor(null, null, today)).isFalse();
    assertThat(AgeRules.isKnownAdult(null, null, today)).isFalse();
  }
}
