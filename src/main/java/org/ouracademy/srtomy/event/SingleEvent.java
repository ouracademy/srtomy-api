package org.ouracademy.srtomy.event;

import java.time.ZonedDateTime;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("1")
public class SingleEvent extends Event {

    public SingleEvent() {
        // jpa only
    }

    public SingleEvent(String title, ZonedDateTime start, ZonedDateTime end) {
        this.title = title;
        this.start = start;
        this.end = end;
    }

    public ZonedDateTime getStart() {
        return start;
    }

    public ZonedDateTime getEnd() {
        return end;
    }
}
