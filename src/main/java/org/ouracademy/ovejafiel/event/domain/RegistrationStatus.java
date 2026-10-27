package org.ouracademy.ovejafiel.event.domain;

/** Where a sign-up stands. */
public enum RegistrationStatus {
  /** Has a seat. */
  CONFIRMED,
  /** Waiting for a seat to free up. */
  WAITLISTED,
  /** Gave up the seat or was removed; kept as history. */
  CANCELLED;

  public boolean isActive() {
    return this != CANCELLED;
  }
}
