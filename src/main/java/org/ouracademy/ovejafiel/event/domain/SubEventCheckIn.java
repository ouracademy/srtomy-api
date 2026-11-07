package org.ouracademy.ovejafiel.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.Getter;

/**
 * Someone who signed up to a mega event came into one of its sub-events that inherits the sign-up.
 * The sign-up is left as it is: its own check-in means they came to the mega event.
 */
@Entity
@Getter
@Table(name = "event_registration_check_in")
public class SubEventCheckIn {

  @Id UUID id = UUID.randomUUID();

  @ManyToOne(optional = false)
  EventRegistration registration;

  @ManyToOne(optional = false)
  Event event;

  @Column(nullable = false)
  LocalDateTime checkedInAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  CheckInSource checkInSource;

  protected SubEventCheckIn() {
    // jpa only
  }

  public SubEventCheckIn(EventRegistration registration, Event event, CheckInSource source) {
    this.registration = registration;
    this.event = event;
    this.checkInSource = source;
    // The database keeps microseconds: what we answer now must be what it will give back.
    this.checkedInAt = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
  }
}
