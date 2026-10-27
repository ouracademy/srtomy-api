package org.ouracademy.ovejafiel.event.controller.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.ouracademy.ovejafiel.event.domain.Recurrence;

/**
 * How an event repeats. {@code daysOfWeek} (0 = Sunday ... 6 = Saturday) is for weekly recurrences,
 * {@code monthlyMode} for monthly ones; {@code until} goes with the end type UNTIL and {@code
 * count} with COUNT.
 */
@Data
public class RecurrenceRequest {
  Recurrence.Frequency frequency;

  Integer interval = 1;

  List<Integer> daysOfWeek = new ArrayList<>();

  Recurrence.MonthlyMode monthlyMode;

  Recurrence.EndType endType = Recurrence.EndType.NEVER;

  LocalDate until;

  Integer count;

  /**
   * @throws org.ouracademy.ovejafiel.shared.domain.DomainValidationException when the combination is
   *     not a valid recurrence
   */
  public Recurrence toRecurrence() {
    return Recurrence.of(frequency, interval, daysOfWeek, monthlyMode, endType, until, count);
  }
}
