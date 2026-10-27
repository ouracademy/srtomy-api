package org.ouracademy.ovejafiel.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;
import org.ouracademy.ovejafiel.user.domain.AppUser;

/** Someone who signed up for an event or course through its shared link. */
@Entity
@Getter
public class EventRegistration extends DomainObject {

  @Id EventRegistrationId id = EventRegistrationId.generate();

  @ManyToOne(optional = false)
  Event event;

  /** The account that signed up, if they were signed in. */
  @ManyToOne AppUser user;

  @Column(nullable = false)
  String firstName;

  @Column(nullable = false)
  String lastName;

  @Column(nullable = false)
  String email;

  String phoneNumber;

  protected EventRegistration() {
    // jpa only
  }

  public EventRegistration(
      Event event, AppUser user, String firstName, String lastName, String email, String phone) {
    this.event = event;
    this.user = user;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.phoneNumber = phone;
  }
}
