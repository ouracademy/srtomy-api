package org.ouracademy.ovejafiel.event.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

/** How an event takes sign-ups. An event without one has no registration. */
@Entity
@Getter
public class EventRegistrationSetup extends DomainObject {

  @Id EventRegistrationSetupId id = EventRegistrationSetupId.generate();

  @OneToOne(optional = false)
  Event event;

  boolean open;

  boolean requiresAccount;

  /** Not offered any more: kept for the sign-ups that already wait. */
  boolean waitlistEnabled;

  /** Not offered any more: the seats are the quantity of each ticket. Kept for old setups. */
  Integer capacity;

  protected EventRegistrationSetup() {
    // jpa only
  }

  /** A new registration is open. */
  public EventRegistrationSetup(Event event) {
    this.event = event;
    this.open = true;
  }

  public void configure(boolean requiresAccount) {
    this.requiresAccount = requiresAccount;
  }

  /**
   * All of it, including the waitlist and the total of seats, which are no longer offered but still
   * apply to the setups that have them.
   */
  public void configure(
      boolean open, boolean requiresAccount, boolean waitlistEnabled, Integer capacity) {
    if (capacity != null && capacity < 1) {
      throw new DomainValidationException("El cupo total debe ser mayor que cero");
    }
    this.open = open;
    this.requiresAccount = requiresAccount;
    this.waitlistEnabled = waitlistEnabled;
    this.capacity = capacity;
  }

  /** Closed, nobody can sign up; the setup and the sign-ups are kept. */
  public void setOpen(boolean open) {
    this.open = open;
  }
}
