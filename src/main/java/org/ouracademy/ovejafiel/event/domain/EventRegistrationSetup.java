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

  boolean waitlistEnabled;

  /** Total seats; null is unlimited. */
  Integer capacity;

  protected EventRegistrationSetup() {
    // jpa only
  }

  public EventRegistrationSetup(Event event) {
    this.event = event;
  }

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
}
