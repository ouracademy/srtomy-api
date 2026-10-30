package org.ouracademy.ovejafiel.shared;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * The size of the time slices a date range is cut into for a chart: a day, a calendar week (7 days
 * starting on the range's first day) or a calendar month.
 */
public enum BucketUnit {
  DAY,
  WEEK,
  MONTH;

  /**
   * The consecutive periods that cover {@code [start, end]} (both inclusive), oldest first. Every
   * period is {@code [period.start(), period.endExclusive())}, so they never overlap.
   */
  public List<Period> bucketsOver(LocalDateTime start, LocalDateTime end) {
    var periods = new ArrayList<Period>();
    switch (this) {
      case MONTH -> {
        var first = YearMonth.from(start);
        var last = YearMonth.from(end);
        var count = (int) ChronoUnit.MONTHS.between(first, last) + 1;
        for (var i = 0; i < count; i++) {
          var month = first.plusMonths(i);
          periods.add(
              new Period(
                  month.atDay(1).atStartOfDay(),
                  month.plusMonths(1).atDay(1).atStartOfDay(),
                  month.toString()));
        }
      }
      case WEEK -> {
        var firstDate = start.toLocalDate();
        var lastDate = end.toLocalDate();
        var totalDays = (int) ChronoUnit.DAYS.between(firstDate, lastDate) + 1;
        var count = (totalDays + 6) / 7;
        for (var i = 0; i < count; i++) {
          var weekStart = firstDate.plusDays(i * 7L);
          periods.add(
              new Period(
                  weekStart.atStartOfDay(),
                  weekStart.plusDays(7).atStartOfDay(),
                  weekStart.toString()));
        }
      }
      case DAY -> {
        var firstDate = start.toLocalDate();
        var lastDate = end.toLocalDate();
        var count = (int) ChronoUnit.DAYS.between(firstDate, lastDate) + 1;
        for (var i = 0; i < count; i++) {
          var day = firstDate.plusDays(i);
          periods.add(
              new Period(day.atStartOfDay(), day.plusDays(1).atStartOfDay(), day.toString()));
        }
      }
    }
    return periods;
  }

  /** One slice: {@code label} is the human string shown for it ("2026-10", an ISO date, ...). */
  public record Period(LocalDateTime start, LocalDateTime endExclusive, String label) {}
}
