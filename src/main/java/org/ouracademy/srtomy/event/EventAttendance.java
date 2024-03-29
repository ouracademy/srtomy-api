package org.ouracademy.srtomy.event;

import java.time.LocalDateTime;
import java.util.List;

import org.ouracademy.srtomy.person.Person;
import org.ouracademy.srtomy.utils.JpaLists;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
public class EventAttendance {

    @Id
    Long id;

    @MapsId
    @OneToOne(optional = false)
    Event event;

    @Setter
    Integer numberOfVisitors;

    @Setter
    String visitorsDescription;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "eventAttendance")
    List<PersonEventAttendance> personEventAttendances;

    public EventAttendance() {
        // jpa only
    }

    EventAttendance(Event event) {
        this.event = event;
    }

    public List<Long> getPersonIds() {
        return personEventAttendances.stream().map(x -> x.getPerson().getId()).toList();
    }

    public void setPeople(List<Person> people) {
        JpaLists.set(
                this.personEventAttendances,
                people.stream()
                        .map(person -> new PersonEventAttendance(this, person, LocalDateTime.now()))
                        .toList());
    }
}
