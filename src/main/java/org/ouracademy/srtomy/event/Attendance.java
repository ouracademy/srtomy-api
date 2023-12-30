package org.ouracademy.srtomy.event;

import org.ouracademy.srtomy.person.Person;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
public class Attendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(optional = false)
    Person person;

    @ManyToOne(optional = false)
    Event event;

    public Attendance(Event event, Person person) {
        this.event = event;
        this.person = person;
    }

    Attendance() {
        // jpa only
    }

}
