package org.ouracademy.ovejafiel.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.Recurrence.EndType;
import org.ouracademy.ovejafiel.event.domain.Recurrence.Frequency;
import org.ouracademy.ovejafiel.event.domain.Recurrence.MonthlyMode;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

class RecurrenceTests {

  static final ZoneId UTC = ZoneId.of("UTC");
  // A Monday.
  static final LocalDateTime FIRST = LocalDateTime.of(2026, 1, 5, 10, 0);

  private static Recurrence never(Frequency frequency, int interval) {
    return Recurrence.of(frequency, interval, null, null, EndType.NEVER, null, null);
  }

  private static List<LocalDate> dates(Recurrence.Expansion expansion) {
    return expansion.starts().stream().map(ZonedDateTime::toLocalDate).toList();
  }

  @Test
  void dailyEveryTwoDaysUpToTheHorizon() {
    var expansion = never(Frequency.DAILY, 2).expand(FIRST, UTC, LocalDate.of(2026, 1, 12));

    assertThat(dates(expansion))
        .containsExactly(
            LocalDate.of(2026, 1, 5),
            LocalDate.of(2026, 1, 7),
            LocalDate.of(2026, 1, 9),
            LocalDate.of(2026, 1, 11));
    assertThat(expansion.exhausted()).isFalse();
  }

  @Test
  void weeklyWithoutDaysRepeatsOnTheWeekdayOfTheFirstOccurrence() {
    var expansion = never(Frequency.WEEKLY, 1).expand(FIRST, UTC, LocalDate.of(2026, 1, 20));

    assertThat(dates(expansion))
        .containsExactly(
            LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 12), LocalDate.of(2026, 1, 19));
  }

  @Test
  void weeklyOnSeveralDaysEveryTwoWeeksSkipsDaysBeforeTheFirstOccurrence() {
    // Sunday (0), Monday (1) and Friday (5), every other week, starting on a Wednesday.
    var recurrence =
        Recurrence.of(Frequency.WEEKLY, 2, List.of(5, 0, 1), null, EndType.NEVER, null, null);

    var expansion =
        recurrence.expand(LocalDateTime.of(2026, 1, 7, 9, 0), UTC, LocalDate.of(2026, 1, 31));

    // Weeks start on Monday: the week of Jan 5 gives Fri 9 and Sun 11 (Mon 5 is before the first
    // occurrence), then the week of Jan 19 gives Mon 19, Fri 23 and Sun 25.
    assertThat(dates(expansion))
        .containsExactly(
            LocalDate.of(2026, 1, 9),
            LocalDate.of(2026, 1, 11),
            LocalDate.of(2026, 1, 19),
            LocalDate.of(2026, 1, 23),
            LocalDate.of(2026, 1, 25));
  }

  @Test
  void monthlyOnTheDayOfTheMonthSkipsMonthsWithoutThatDay() {
    var recurrence =
        Recurrence.of(
            Frequency.MONTHLY, 1, null, MonthlyMode.DAY_OF_MONTH, EndType.NEVER, null, null);

    var expansion =
        recurrence.expand(LocalDateTime.of(2026, 1, 31, 8, 0), UTC, LocalDate.of(2026, 5, 31));

    assertThat(dates(expansion))
        .containsExactly(
            LocalDate.of(2026, 1, 31), LocalDate.of(2026, 3, 31), LocalDate.of(2026, 5, 31));
  }

  @Test
  void monthlyOnTheNthWeekday() {
    // Jan 11, 2026 is the second Sunday.
    var recurrence =
        Recurrence.of(
            Frequency.MONTHLY, 1, null, MonthlyMode.NTH_WEEKDAY, EndType.NEVER, null, null);

    var expansion =
        recurrence.expand(LocalDateTime.of(2026, 1, 11, 8, 0), UTC, LocalDate.of(2026, 4, 30));

    assertThat(dates(expansion))
        .containsExactly(
            LocalDate.of(2026, 1, 11),
            LocalDate.of(2026, 2, 8),
            LocalDate.of(2026, 3, 8),
            LocalDate.of(2026, 4, 12));
  }

  @Test
  void aFifthWeekdayMeansTheLastOneOfEachMonth() {
    // Jan 29, 2026 is the fifth Thursday.
    var recurrence =
        Recurrence.of(
            Frequency.MONTHLY, 1, null, MonthlyMode.NTH_WEEKDAY, EndType.NEVER, null, null);

    var expansion =
        recurrence.expand(LocalDateTime.of(2026, 1, 29, 8, 0), UTC, LocalDate.of(2026, 3, 31));

    assertThat(dates(expansion))
        .containsExactly(
            LocalDate.of(2026, 1, 29), LocalDate.of(2026, 2, 26), LocalDate.of(2026, 3, 26));
  }

  @Test
  void yearlySkipsFebruary29OnCommonYears() {
    var expansion =
        never(Frequency.YEARLY, 1)
            .expand(LocalDateTime.of(2024, 2, 29, 8, 0), UTC, LocalDate.of(2032, 12, 31));

    assertThat(dates(expansion))
        .containsExactly(
            LocalDate.of(2024, 2, 29), LocalDate.of(2028, 2, 29), LocalDate.of(2032, 2, 29));
  }

  @Test
  void endingAfterACountStopsThereAndIsExhausted() {
    var recurrence = Recurrence.of(Frequency.DAILY, 1, null, null, EndType.COUNT, null, 3);

    var expansion = recurrence.expand(FIRST, UTC, LocalDate.of(2027, 1, 1));

    assertThat(dates(expansion))
        .containsExactly(
            LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 7));
    assertThat(expansion.exhausted()).isTrue();
  }

  @Test
  void aCountBeyondTheHorizonIsNotExhaustedYet() {
    var recurrence = Recurrence.of(Frequency.DAILY, 1, null, null, EndType.COUNT, null, 10);

    var expansion = recurrence.expand(FIRST, UTC, LocalDate.of(2026, 1, 7));

    assertThat(expansion.starts()).hasSize(3);
    assertThat(expansion.exhausted()).isFalse();
  }

  @Test
  void endingOnADateIncludesThatDay() {
    var recurrence =
        Recurrence.of(
            Frequency.DAILY, 1, null, null, EndType.UNTIL, LocalDate.of(2026, 1, 7), null);

    var expansion = recurrence.expand(FIRST, UTC, LocalDate.of(2027, 1, 1));

    assertThat(dates(expansion))
        .containsExactly(
            LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 7));
    assertThat(expansion.exhausted()).isTrue();
  }

  @Test
  void theHourOfTheDayIsKeptAcrossSummerTime() {
    var newYork = ZoneId.of("America/New_York");

    var expansion =
        never(Frequency.WEEKLY, 1)
            .expand(LocalDateTime.of(2026, 3, 1, 10, 0), newYork, LocalDate.of(2026, 3, 16));

    // Summer time starts on Mar 8: the local hour stays 10:00, the instant moves.
    assertThat(expansion.starts())
        .extracting(start -> start.toLocalTime().getHour())
        .containsOnly(10);
    assertThat(expansion.starts().get(0).toInstant().toString()).isEqualTo("2026-03-01T15:00:00Z");
    assertThat(expansion.starts().get(1).toInstant().toString()).isEqualTo("2026-03-08T14:00:00Z");
  }

  @Test
  void endingOnProducesARecurrenceThatEndsOnThatDay() {
    var recurrence = never(Frequency.DAILY, 1).endingOn(LocalDate.of(2026, 1, 6));

    assertThat(recurrence.getEndType()).isEqualTo(EndType.UNTIL);
    assertThat(recurrence.getUntil()).isEqualTo(LocalDate.of(2026, 1, 6));
    assertThat(recurrence.expand(FIRST, UTC, LocalDate.of(2027, 1, 1)).starts()).hasSize(2);
  }

  @Test
  void rruleDescribesTheRecurrence() {
    var weekly = Recurrence.of(Frequency.WEEKLY, 2, List.of(3, 1), null, EndType.COUNT, null, 10);

    assertThat(weekly.toRRule(UTC)).isEqualTo("FREQ=WEEKLY;INTERVAL=2;BYDAY=MO,WE;COUNT=10");
    assertThat(never(Frequency.DAILY, 1).toRRule(UTC)).isEqualTo("FREQ=DAILY;INTERVAL=1");
    assertThat(never(Frequency.DAILY, 1).endingOn(LocalDate.of(2026, 1, 6)).toRRule(UTC))
        .isEqualTo("FREQ=DAILY;INTERVAL=1;UNTIL=20260106T235959Z");
  }

  @Test
  void aFrequencyIsRequired() {
    assertThatThrownBy(() -> Recurrence.of(null, 1, null, null, EndType.NEVER, null, null))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void theIntervalMustBeBetweenOneAndNinetyNine() {
    assertThatThrownBy(() -> never(Frequency.DAILY, 0))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> never(Frequency.DAILY, 100))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void weekdaysMustBeBetweenZeroAndSix() {
    assertThatThrownBy(
            () -> Recurrence.of(Frequency.WEEKLY, 1, List.of(7), null, EndType.NEVER, null, null))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void anEndDateIsRequiredToEndOnADate() {
    assertThatThrownBy(
            () -> Recurrence.of(Frequency.DAILY, 1, null, null, EndType.UNTIL, null, null))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void aCountBetweenOneAndAThousandIsRequiredToEndAfterAnAmount() {
    assertThatThrownBy(() -> Recurrence.of(Frequency.DAILY, 1, null, null, EndType.COUNT, null, 0))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(
            () -> Recurrence.of(Frequency.DAILY, 1, null, null, EndType.COUNT, null, 1001))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void weekdaysAreIgnoredOutsideAWeeklyRecurrence() {
    var recurrence =
        Recurrence.of(Frequency.DAILY, 1, List.of(1), MonthlyMode.NTH_WEEKDAY, null, null, null);

    assertThat(recurrence.getDaysOfWeek()).isNull();
    assertThat(recurrence.getMonthlyMode()).isNull();
    assertThat(recurrence.getEndType()).isEqualTo(EndType.NEVER);
  }
}
