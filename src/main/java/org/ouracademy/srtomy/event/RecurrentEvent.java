package org.ouracademy.srtomy.event;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;

@Getter
@Entity
@DiscriminatorValue("2")
public class RecurrentEvent extends Event {

    @Column(columnDefinition = "smallint[]")
    List<Integer> daysOfWeek;

    public RecurrentEvent() {
        // jpa only
    }

    public RecurrentEvent(String title, ZonedDateTime start, ZonedDateTime end, List<Integer> daysOfWeek) {
        this.title = title;
        this.start = start;
        this.end = end;
        this.daysOfWeek = daysOfWeek;
    }

    public LocalTime getStartTime() {
        return start.toLocalTime();
    }

    public LocalTime getEndTime() {
        return end.toLocalTime();
    }

}
