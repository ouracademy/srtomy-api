package org.ouracademy.srtomy.event.controller.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.ouracademy.srtomy.event.domain.Recurrence;
import org.ouracademy.srtomy.event.query.RecurrenceResult;

/** How the series of an event repeats; absent for a one-off event. */
public record RecurrenceResponse(
    UUID seriesId,
    Recurrence.Frequency frequency,
    int interval,
    List<Integer> daysOfWeek,
    Recurrence.MonthlyMode monthlyMode,
    Recurrence.EndType endType,
    LocalDate until,
    Integer count,
    String timeZone) {

  public static RecurrenceResponse from(RecurrenceResult result) {
    if (result == null) {
      return null;
    }
    return new RecurrenceResponse(
        result.seriesId().value(),
        result.frequency(),
        result.interval(),
        result.daysOfWeek(),
        result.monthlyMode(),
        result.endType(),
        result.until(),
        result.count(),
        result.timeZone());
  }
}
