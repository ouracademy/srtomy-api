package org.ouracademy.ovejafiel.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.Getter;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.shared.domain.RandomSlugs;
import org.ouracademy.ovejafiel.user.domain.AppUser;

/** Someone with a ticket of an event, or waiting for one. */
@Entity
@Getter
public class EventRegistration extends DomainObject {

  @Id EventRegistrationId id = EventRegistrationId.generate();

  @ManyToOne(optional = false)
  Event event;

  @ManyToOne(optional = false)
  TicketType ticketType;

  /** The account that signed up, if they were signed in. */
  @ManyToOne AppUser user;

  @Column(nullable = false)
  String firstName;

  @Column(nullable = false)
  String lastName;

  @Column(nullable = false)
  String email;

  String phoneNumber;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  RegistrationStatus status;

  /** Lets whoever signed up cancel without an account. */
  @Column(nullable = false)
  String cancelCode = RandomSlugs.next();

  /** What the ticket QR carries: whoever scans it can check them in, not cancel. */
  @Column(nullable = false)
  String ticketCode = RandomSlugs.next();

  LocalDateTime checkedInAt;

  @Enumerated(EnumType.STRING)
  CheckInSource checkInSource;

  protected EventRegistration() {
    // jpa only
  }

  public EventRegistration(
      Event event,
      TicketType ticketType,
      AppUser user,
      String firstName,
      String lastName,
      String email,
      String phone,
      RegistrationStatus status) {
    this.event = event;
    this.ticketType = ticketType;
    this.user = user;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.phoneNumber = phone;
    this.status = status;
  }

  public String getFullName() {
    return firstName + " " + lastName;
  }

  public boolean isCheckedIn() {
    return checkedInAt != null;
  }

  public void cancel() {
    this.status = RegistrationStatus.CANCELLED;
    this.checkedInAt = null;
    this.checkInSource = null;
  }

  /** Marks them as present. Only a confirmed sign-up checks in; doing it again changes nothing. */
  public void checkIn(CheckInSource source) {
    if (status != RegistrationStatus.CONFIRMED) {
      throw new DomainValidationException("Solo una inscripción confirmada puede ingresar");
    }
    if (checkedInAt == null) {
      // The database keeps microseconds: what we answer now must be what it will give back.
      this.checkedInAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
      this.checkInSource = source;
    }
  }

  public void undoCheckIn() {
    this.checkedInAt = null;
    this.checkInSource = null;
  }
}
