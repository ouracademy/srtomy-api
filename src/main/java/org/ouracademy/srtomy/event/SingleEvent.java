package org.ouracademy.srtomy.event;

import java.time.ZonedDateTime;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

@Entity
@DiscriminatorValue("1")
public class SingleEvent extends Event {
    @ManyToOne
    RecurrentEvent recurrentEvent;

    public SingleEvent() {
        // jpa only
    }

    public SingleEvent(String title, ZonedDateTime start, ZonedDateTime end) {
        this.title = title;
        this.start = start;
        this.end = end;
    }
}
