package org.ouracademy.ovejafiel.event.domain;

/** Where a sign-up stands. When the seats run out nobody else signs up: there is no waitlist. */
public enum RegistrationStatus {
  /** Has a seat. */
  CONFIRMED,
  /** Gave up the seat or was removed; kept as history. */
  CANCELLED;

  public boolean isActive() {
    return this == CONFIRMED;
  }
}
