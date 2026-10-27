package org.ouracademy.ovejafiel.attendance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.user.domain.AppUser;

@Entity
@Getter
public class PersonEventAttendance {

  @Embeddable
  @Getter
  @AllArgsConstructor
  @NoArgsConstructor // jpa only
  @EqualsAndHashCode
  public static class Id implements Serializable {
    @Column(name = "event_attendance_id")
    private EventId eventAttendanceId;

    @Column(name = "person_id")
    private PartyId personId;
  }

  @EmbeddedId Id id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "event_attendance_id", insertable = false, updatable = false)
  EventAttendance eventAttendance;

  @ManyToOne(optional = false)
  @JoinColumn(name = "person_id", insertable = false, updatable = false)
  Person person;

  LocalDateTime dateTime;

  /** How they were marked as present. */
  @Enumerated(EnumType.STRING)
  CheckInSource source = CheckInSource.MANUAL;

  /** Who marked them; null when they marked themselves. */
  @ManyToOne
  @JoinColumn(name = "marked_by")
  AppUser markedBy;

  public PersonEventAttendance(
      EventAttendance eventAttendance, Person person, LocalDateTime dateTime) {
    this(eventAttendance, person, dateTime, CheckInSource.MANUAL, null);
  }

  public PersonEventAttendance(
      EventAttendance eventAttendance,
      Person person,
      LocalDateTime dateTime,
      CheckInSource source,
      AppUser markedBy) {
    this.eventAttendance = eventAttendance;
    this.person = person;
    this.id = new Id(this.eventAttendance.getId(), this.person.getId());
    this.dateTime = dateTime;
    this.source = source;
    this.markedBy = markedBy;
  }

  PersonEventAttendance() {
    // jpa only
  }
}
