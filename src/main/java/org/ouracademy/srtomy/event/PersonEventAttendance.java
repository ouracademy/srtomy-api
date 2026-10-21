package org.ouracademy.srtomy.event;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.ouracademy.srtomy.person.PartyId;
import org.ouracademy.srtomy.person.Person;

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

  public PersonEventAttendance(
      EventAttendance eventAttendance, Person person, LocalDateTime dateTime) {
    this.eventAttendance = eventAttendance;
    this.person = person;
    this.id = new Id(this.eventAttendance.getId(), this.person.getId());
    this.dateTime = dateTime;
  }

  PersonEventAttendance() {
    // jpa only
  }
}
