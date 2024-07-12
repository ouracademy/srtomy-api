package org.ouracademy.srtomy.event;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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

    LocalDate endRecur;

    /**
     * Following https://fullcalendar.io/docs/recurring-events
     * L (1), M (2), .... D(0)
     */
    @Column(columnDefinition = "smallint[]")
    List<Integer> daysOfWeek;

    public RecurrentEvent() {
        // jpa only
    }

    public RecurrentEvent(
            String title,
            ZonedDateTime start, ZonedDateTime end,
            List<Integer> daysOfWeek, LocalDate endRecur) {
        this.title = title;
        this.start = start;
        this.end = end;
        this.daysOfWeek = daysOfWeek;
        this.endRecur = endRecur;
    }

    public List<SingleEvent> expand(OffsetDateTime start, OffsetDateTime end) {
        var daysOfWeek = javaDayOfWeeks();
        var endRecurrent = endRecur.atTime(start.getHour(), start.getMinute(), start.getSecond()).plus(duration()).atOffset(start.getOffset());
        return dateTimesBetween(start, endRecurrent)
            .filter(x -> daysOfWeek.contains(x.getDayOfWeek()))
            .map(offsetDate -> offsetDate.atZoneSameInstant(ZoneId.of("UTC")))
            .map(date -> toSingleEvent(date, date.plus(duration())))
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

    public static Stream<OffsetDateTime> dateTimesBetween(OffsetDateTime start, OffsetDateTime end) {
        List<OffsetDateTime> totalDates = new ArrayList<>();
        while (!start.isAfter(end)) {
            totalDates.add(start);
            start = start.plusDays(1);
        }
        return totalDates.stream();
    }

}
