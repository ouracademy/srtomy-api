package org.ouracademy.ovejafiel.event.controller.dto;

import java.time.LocalDate;
import java.util.List;
import org.ouracademy.ovejafiel.event.domain.Recurrence;

/**
 * How an event repeats. {@code daysOfWeek} (0 = Sunday ... 6 = Saturday) is for weekly recurrences,
 * {@code monthlyMode} for monthly ones; {@code until} goes with the end type UNTIL and {@code
 * count} with COUNT. Not said: every 1 {@code frequency}, no days, never ends.
 */
public record RecurrenceRequest(
    Recurrence.Frequency frequency,
    Integer interval,
    List<Integer> daysOfWeek,
    Recurrence.MonthlyMode monthlyMode,
    Recurrence.EndType endType,
    LocalDate until,
    Integer count) {

  public RecurrenceRequest {
    interval = interval == null ? 1 : interval;
    daysOfWeek = daysOfWeek == null ? List.of() : daysOfWeek;
    endType = endType == null ? Recurrence.EndType.NEVER : endType;
  }

  /**
   * @throws org.ouracademy.ovejafiel.shared.domain.DomainValidationException when the combination
   *     is not a valid recurrence
   */
  public Recurrence toRecurrence() {
    return Recurrence.of(frequency, interval, daysOfWeek, monthlyMode, endType, until, count);
  }
}
