package org.ouracademy.ovejafiel.event.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;

/** How an event takes sign-ups. An event without one has no registration. */
@Entity
@Getter
public class EventRegistrationSetup extends DomainObject {

  @Id EventRegistrationSetupId id = EventRegistrationSetupId.generate();

  @OneToOne(optional = false)
  Event event;

  boolean open;

  boolean requiresAccount;

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

  /** Closed, nobody can sign up; the setup and the sign-ups are kept. */
  public void setOpen(boolean open) {
    this.open = open;
  }
}
