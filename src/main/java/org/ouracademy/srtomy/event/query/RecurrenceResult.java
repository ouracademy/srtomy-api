package org.ouracademy.srtomy.event.query;

import java.time.LocalDate;
import java.util.List;
import org.ouracademy.srtomy.event.domain.EventSeries;
import org.ouracademy.srtomy.event.domain.EventSeriesId;
import org.ouracademy.srtomy.event.domain.Recurrence;

/** How the series an event belongs to repeats, as exposed outside the module. */
public record RecurrenceResult(
    EventSeriesId seriesId,
    Recurrence.Frequency frequency,
    int interval,
    List<Integer> daysOfWeek,
    Recurrence.MonthlyMode monthlyMode,
    Recurrence.EndType endType,
    LocalDate until,
    Integer count,
    String timeZone) {

  /** Null for a one-off event. */
  public static RecurrenceResult from(EventSeries series) {
    if (series == null) {
      return null;
    }
    var recurrence = series.getRecurrence();
    return new RecurrenceResult(
        series.getId(),
        recurrence.getFrequency(),
        recurrence.getInterval(),
        recurrence.getDaysOfWeek() == null ? List.of() : List.copyOf(recurrence.getDaysOfWeek()),
        recurrence.getMonthlyMode(),
        recurrence.getEndType(),
        recurrence.getUntil(),
        recurrence.getCount(),
        series.getTimeZone());
  }
}
