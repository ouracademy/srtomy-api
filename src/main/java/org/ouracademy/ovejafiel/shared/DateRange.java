package org.ouracademy.ovejafiel.shared;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

/**
 * A range of whole days, both ends included, as the dashboard endpoints receive it. It knows the
 * instants it covers (UTC) and how to slice itself for a chart: a day for up to a month, a week for
 * up to about four months, a month beyond that.
 */
public record DateRange(LocalDate startDate, LocalDate endDate) {

  private static final int MAX_DAYS_PER_DAY_BUCKET = 31;
  private static final int MAX_DAYS_PER_WEEK_BUCKET = 120;

  public DateRange {
    if (startDate.isAfter(endDate)) {
      throw new DomainValidationException("La fecha de inicio no puede ser posterior a la de fin");
    }
  }

  public Instant start() {
    return startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
  }

  /** The last nanosecond of {@code endDate}. */
  public Instant end() {
    return endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).minusNanos(1).toInstant();
  }

  public BucketUnit bucket() {
    var days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
    if (days <= MAX_DAYS_PER_DAY_BUCKET) {
      return BucketUnit.DAY;
    }
    if (days <= MAX_DAYS_PER_WEEK_BUCKET) {
      return BucketUnit.WEEK;
    }
    return BucketUnit.MONTH;
  }
}
