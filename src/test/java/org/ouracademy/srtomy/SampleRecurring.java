package org.ouracademy.srtomy;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import net.logicsquad.recurring.DayInMonth;
import net.logicsquad.recurring.Schedule;
import net.logicsquad.recurring.ScheduleElement;
import net.logicsquad.recurring.TemporalExpression;
import net.logicsquad.recurring.Union;

public class SampleRecurring {
    @Test
    public void just_a_test() {
        TemporalExpression firstThursday = DayInMonth.of(DayOfWeek.THURSDAY, 1);
        TemporalExpression lastWednesday = DayInMonth.of(DayOfWeek.WEDNESDAY, -1);
        ScheduleElement element = ScheduleElement.of("Meeting", Union.of(firstThursday, lastWednesday));
        Schedule schedule = Schedule.of(element);
        boolean today = schedule.isOccurring("Meeting", LocalDate.now());

        System.out.println(today);
        System.out.println("TemporalExpression:" + lastWednesday);
        System.out.println("ScheduleElement:" + element);
        System.out.println("Schedule:" + schedule);
        assertNotNull(schedule);
    }

}
