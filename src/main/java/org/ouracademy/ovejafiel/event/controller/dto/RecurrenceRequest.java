package org.ouracademy.ovejafiel.event.controller.dto;

import java.time.LocalDate;
import java.util.List;
import org.ouracademy.ovejafiel.event.domain.Recurrence;

/**
 * How an event repeats. {@code daysOfWeek} (0 = Sunday ... 6 = Saturday) is for weekly recurrences,
 * {@code monthlyMode} for monthly ones; {@code until} goes with the end type UNTIL and {@code
 * count} with COUNT. Not said: every 1 {@code frequency}, no days, never ends.
 *
 * @param frequency How often it repeats.
 * @param interval Every how many days, weeks, months or years; 1 when omitted.
 * @param daysOfWeek Weekly only: the days it happens, 0 for Sunday to 6 for Saturday.
 * @param monthlyMode Monthly only: on the same day of the month, or on the same weekday (the second
 *     Tuesday).
 * @param endType How the repetition ends; never when omitted.
 * @param until With the end type UNTIL: the last day, as an ISO date.
 * @param count With the end type COUNT: how many times it happens.
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
