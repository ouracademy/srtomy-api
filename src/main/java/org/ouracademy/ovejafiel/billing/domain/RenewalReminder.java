package org.ouracademy.ovejafiel.billing.domain;

import java.time.LocalDate;

/**
 * When the church is reminded to renew: a week and a day before the paid period ends, and the day
 * the grace days start. The plan is prepaid, so nothing is charged on its own.
 */
public enum RenewalReminder {
  SEVEN_DAYS(7),
  ONE_DAY(1),
  GRACE(-1);

  /** Days from today to the last paid day. */
  private final int daysBefore;

  RenewalReminder(int daysBefore) {
    this.daysBefore = daysBefore;
  }

  /** The last paid day of the churches that get this reminder {@code today}. */
  public LocalDate paidUntil(LocalDate today) {
    return today.plusDays(daysBefore);
  }
}
