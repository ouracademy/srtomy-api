package org.ouracademy.srtomy.event;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.ouracademy.srtomy.person.Person;
import org.ouracademy.srtomy.utils.JpaLists;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Getter;

@Entity
@Getter
public class EventAttendance {

    @Id
    Long id;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "id")
    Event event;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "eventAttendance")
    List<PersonEventAttendance> personEventAttendances = new ArrayList<>();

    @Column(nullable = false)
    LocalDateTime dateTime;

    public EventAttendance() {
        // jpa only
    }

    EventAttendance(Event event) {
        this.event = event;
        this.dateTime = LocalDateTime.now();
    }

    public List<Long> getPersonIds() {
        return personEventAttendances.stream().map(x -> x.getPerson().getId()).toList();
    }

    public void setPeople(List<Person> people) {
        JpaLists.set(
                this.personEventAttendances,
                people.stream()
                        .map(person -> new PersonEventAttendance(this, person, this.dateTime))
                        .toList());
    }
}
