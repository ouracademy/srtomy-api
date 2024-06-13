package org.ouracademy.srtomy.event;

import java.time.DayOfWeek;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;

@Getter
@Entity
@DiscriminatorValue("2")
public class RecurrentEvent extends Event {

    /**
     * Following https://fullcalendar.io/docs/recurring-events
     * L (1), M (2), .... D(0)
     */
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

    public List<SingleEvent> expand() {
        var daysOfWeek = javaDayOfWeeks();
        return dateTimesBetween(start, end)
                .filter(x -> daysOfWeek.contains(x.getDayOfWeek()))
                .map(date -> toSingleEvent(date, date.with(this.end.toLocalTime())))
                .toList();
    }

    private SingleEvent toSingleEvent(ZonedDateTime start, ZonedDateTime end) {
        var event = new SingleEvent(title, start, end);
        event.organization = this.organization;
        event.address = this.address;
        event.speakers = new ArrayList<>(this.speakers);
        event.description = this.description;
        event.isACourse = this.isACourse;
        event.recurrentEvent = this;
        return event;
    }

    public List<DayOfWeek> javaDayOfWeeks() {
        // from Sun(0), Mon(1)-Sat(6) to Sun(7), Mon(1)-Sat(6)
        return this.daysOfWeek.stream().map(x -> DayOfWeek.of(x == 0 ? 7 : x)).toList();
    }

    public static Stream<ZonedDateTime> dateTimesBetween(ZonedDateTime start, ZonedDateTime end) {
        List<ZonedDateTime> totalDates = new ArrayList<>();
        while (!start.isAfter(end)) {
            totalDates.add(start);
            start = start.plusDays(1);
        }
        return totalDates.stream();
    }

}
