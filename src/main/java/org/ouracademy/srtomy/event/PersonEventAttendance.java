package org.ouracademy.srtomy.event;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.ouracademy.srtomy.person.Person;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
public class PersonEventAttendance {

    @Embeddable
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor // jpa only
    @EqualsAndHashCode
    public static class Id implements Serializable {
        private Long eventAttendanceId;
        private Long personId;
    }

    @EmbeddedId
    Id id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "event_attendance_id", insertable = false, updatable = false)
    EventAttendance eventAttendance;

    @ManyToOne(optional = false)
    @JoinColumn(name = "person_id", insertable = false, updatable = false)
    Person person;

    LocalDateTime dateTime;

    public PersonEventAttendance(EventAttendance eventAttendance, Person person, LocalDateTime dateTime) {
        this.eventAttendance = eventAttendance;
        this.person = person;
        this.dateTime = dateTime;
    }

}
