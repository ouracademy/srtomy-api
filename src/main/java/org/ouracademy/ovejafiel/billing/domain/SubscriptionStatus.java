package org.ouracademy.ovejafiel.billing.domain;

/**
 * {@code GRACE}: the paid period ended a few days ago and the church keeps the paid plan while it
 * renews. {@code EXPIRED}: it did not renew, so it has the limits of the free plan (nothing is
 * deleted).
 */
public enum SubscriptionStatus {
  FREE,
  ACTIVE,
  GRACE,
  EXPIRED;

  public boolean isPaid() {
    return this == ACTIVE || this == GRACE;
  }
}
