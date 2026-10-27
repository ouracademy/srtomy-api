package org.ouracademy.srtomy.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;

/**
 * How an event repeats, like the "custom recurrence" of Google Calendar: every {@code interval}
 * days/weeks/months/years, on some weekdays (weekly) or on the same day of the month or the same
 * weekday of the month (monthly), ending never, on a date or after a number of occurrences.
 */
@Getter
@EqualsAndHashCode
@Embeddable
public class Recurrence {

  public enum Frequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
  }

  /** Monthly only: "on day 15" or "on the second Tuesday" (the fifth one means the last). */
  public enum MonthlyMode {
    DAY_OF_MONTH,
    NTH_WEEKDAY
  }

  public enum EndType {
    NEVER,
    UNTIL,
    COUNT
  }

  /** Occurrences found up to a horizon; {@code exhausted} when the recurrence has no more after. */
  public record Expansion(List<ZonedDateTime> starts, boolean exhausted) {}

  private static final int MAX_INTERVAL = 99;
  private static final int MAX_COUNT = 1000;
  // Guards the loops against a recurrence that never reaches the horizon.
  private static final int MAX_STEPS = 100_000;

  @Enumerated(EnumType.STRING)
  @Column(name = "frequency", nullable = false)
  Frequency frequency;

  @Column(name = "recur_interval", nullable = false)
  int interval;

  /**
   * Weekly only. Following https://fullcalendar.io/docs/recurring-events: D (0), L (1), ... S (6)
   */
  @Column(name = "days_of_week", columnDefinition = "smallint[]")
  List<Integer> daysOfWeek;

  @Enumerated(EnumType.STRING)
  @Column(name = "monthly_mode")
  MonthlyMode monthlyMode;

  @Enumerated(EnumType.STRING)
  @Column(name = "end_type", nullable = false)
  EndType endType;

  @Column(name = "until_date")
  LocalDate until;

  @Column(name = "occurrence_count")
  Integer count;

  protected Recurrence() {
    // jpa only
  }

  private Recurrence(
      Frequency frequency,
      int interval,
      List<Integer> daysOfWeek,
      MonthlyMode monthlyMode,
      EndType endType,
      LocalDate until,
      Integer count) {
    this.frequency = frequency;
    this.interval = interval;
    this.daysOfWeek = daysOfWeek;
    this.monthlyMode = monthlyMode;
    this.endType = endType;
    this.until = until;
    this.count = count;
  }

  /**
   * @throws DomainValidationException when the combination makes no sense (e.g. weekdays on a
   *     monthly recurrence, or an end date without a date)
   */
  public static Recurrence of(
      Frequency frequency,
      Integer interval,
      List<Integer> daysOfWeek,
      MonthlyMode monthlyMode,
      EndType endType,
      LocalDate until,
      Integer count) {
    if (frequency == null) {
      throw new DomainValidationException("Indica cada cuánto se repite el evento");
    }
    var every = interval == null ? 1 : interval;
    if (every < 1 || every > MAX_INTERVAL) {
      throw new DomainValidationException("El intervalo debe estar entre 1 y " + MAX_INTERVAL);
    }
    var days = normalizedDays(frequency, daysOfWeek);
    var mode = frequency == Frequency.MONTHLY ? monthlyMode : null;
    var end = endType == null ? EndType.NEVER : endType;
    return switch (end) {
      case NEVER -> new Recurrence(frequency, every, days, mode, end, null, null);
      case UNTIL -> {
        if (until == null) {
          throw new DomainValidationException("Indica la fecha en la que termina el evento");
        }
        yield new Recurrence(frequency, every, days, mode, end, until, null);
      }
      case COUNT -> {
        if (count == null || count < 1 || count > MAX_COUNT) {
          throw new DomainValidationException(
              "El número de repeticiones debe estar entre 1 y " + MAX_COUNT);
        }
        yield new Recurrence(frequency, every, days, mode, end, null, count);
      }
    };
  }

  private static List<Integer> normalizedDays(Frequency frequency, List<Integer> daysOfWeek) {
    if (frequency != Frequency.WEEKLY) {
      return null;
    }
    if (daysOfWeek == null || daysOfWeek.isEmpty()) {
      return null; // the weekday of the first occurrence
    }
    if (daysOfWeek.stream().anyMatch(d -> d == null || d < 0 || d > 6)) {
      throw new DomainValidationException("Los días de la semana van de 0 (domingo) a 6 (sábado)");
    }
    return List.copyOf(new TreeSet<>(daysOfWeek));
  }

  /** The same recurrence, ending on {@code lastDay}. */
  public Recurrence endingOn(LocalDate lastDay) {
    return new Recurrence(
        frequency, interval, daysOfWeek, monthlyMode, EndType.UNTIL, lastDay, null);
  }

  /** The recurrence as an RFC 5545 RRULE value (informational, e.g. for the calendar feed). */
  public String toRRule(ZoneId zone) {
    var rule = new StringBuilder("FREQ=" + frequency + ";INTERVAL=" + interval);
    if (frequency == Frequency.WEEKLY && daysOfWeek != null) {
      rule.append(";BYDAY=")
          .append(String.join(",", daysOfWeek.stream().map(Recurrence::rruleDay).toList()));
    }
    switch (endType) {
      case UNTIL ->
          rule.append(";UNTIL=")
              .append(
                  until
                      .plusDays(1)
                      .atStartOfDay(zone)
                      .toInstant()
                      .minusSeconds(1)
                      .toString()
                      .replaceAll("[-:]", ""));
      case COUNT -> rule.append(";COUNT=").append(count);
      case NEVER -> {}
    }
    return rule.toString();
  }

  private static String rruleDay(int day) {
    return List.of("SU", "MO", "TU", "WE", "TH", "FR", "SA").get(day);
  }

  /**
   * The occurrences from {@code first} up to {@code horizon} (inclusive, local dates in {@code
   * zone}). The wall-clock time is kept across daylight-saving changes.
   */
  public Expansion expand(LocalDateTime first, ZoneId zone, LocalDate horizon) {
    var firstDate = first.toLocalDate();
    var time = first.toLocalTime();
    var starts = new ArrayList<ZonedDateTime>();
    var exhausted = false;
    var emitted = 0;
    var steps = 0;
    var period = 0;
    loop:
    while (steps++ < MAX_STEPS) {
      for (var date : datesOfPeriod(firstDate, period)) {
        if (date.isBefore(firstDate)) {
          continue;
        }
        if (endType == EndType.UNTIL && date.isAfter(until)) {
          exhausted = true;
          break loop;
        }
        if (date.isAfter(horizon)) {
          break loop;
        }
        starts.add(date.atTime(time).atZone(zone));
        emitted++;
        if (endType == EndType.COUNT && emitted >= count) {
          exhausted = true;
          break loop;
        }
      }
      period++;
      if (periodStart(firstDate, period).isAfter(horizon)) {
        break;
      }
    }
    if (endType == EndType.UNTIL && !until.isAfter(horizon)) {
      exhausted = true;
    }
    return new Expansion(starts, exhausted);
  }

  private LocalDate periodStart(LocalDate firstDate, int period) {
    return switch (frequency) {
      case DAILY -> firstDate.plusDays((long) period * interval);
      case WEEKLY ->
          firstDate
              .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
              .plusWeeks((long) period * interval);
      case MONTHLY -> firstDate.withDayOfMonth(1).plusMonths((long) period * interval);
      case YEARLY -> firstDate.withDayOfYear(1).plusYears((long) period * interval);
    };
  }

  /** The dates of one period, sorted; may include dates before the first occurrence. */
  private List<LocalDate> datesOfPeriod(LocalDate firstDate, int period) {
    var start = periodStart(firstDate, period);
    return switch (frequency) {
      case DAILY -> List.of(start);
      case WEEKLY -> weekDates(firstDate, start);
      case MONTHLY -> monthDate(firstDate, start);
      case YEARLY -> yearDate(firstDate, start);
    };
  }

  private List<LocalDate> weekDates(LocalDate firstDate, LocalDate monday) {
    var days = daysOfWeek == null ? List.of(firstDate.getDayOfWeek().getValue() % 7) : daysOfWeek;
    return days.stream()
        .map(d -> monday.plusDays(d == 0 ? 6 : d - 1L)) // Monday-based weeks
        .sorted()
        .toList();
  }

  private List<LocalDate> monthDate(LocalDate firstDate, LocalDate monthStart) {
    var month = YearMonth.from(monthStart);
    if (monthlyMode == MonthlyMode.NTH_WEEKDAY) {
      var weekday = firstDate.getDayOfWeek();
      var nth = (firstDate.getDayOfMonth() - 1) / 7 + 1;
      var date =
          nth >= 5
              ? month.atEndOfMonth().with(TemporalAdjusters.lastInMonth(weekday))
              : month.atDay(1).with(TemporalAdjusters.dayOfWeekInMonth(nth, weekday));
      return List.of(date);
    }
    var day = firstDate.getDayOfMonth();
    return day > month.lengthOfMonth() ? List.of() : List.of(month.atDay(day));
  }

  private List<LocalDate> yearDate(LocalDate firstDate, LocalDate yearStart) {
    var month = YearMonth.of(yearStart.getYear(), firstDate.getMonth());
    var day = firstDate.getDayOfMonth();
    return day > month.lengthOfMonth() ? List.of() : List.of(month.atDay(day));
  }
}
