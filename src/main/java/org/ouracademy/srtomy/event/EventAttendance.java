package org.ouracademy.srtomy.event;

import java.util.ArrayList;
import java.util.List;

import org.ouracademy.srtomy.domain.DomainObject;
import org.ouracademy.srtomy.person.Person;
import org.ouracademy.srtomy.utils.JpaLists;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class EventAttendance extends DomainObject {

    @ManyToOne(optional = false)
    Event event;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    List<Person> people = new ArrayList<>();

    Integer numberOfVisitors;
    String visitorsDescription;

    public EventAttendance() {
        // jpa only
    }

    EventAttendance(Event event) {
        this.event = event;
    }

    public void setPeople(List<Person> people) {
        JpaLists.set(this.people, people);
    }
}
