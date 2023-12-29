package org.ouracademy.srtomy;

import java.time.DayOfWeek;
import java.time.LocalDate;

import net.logicsquad.recurring.DayInMonth;
import net.logicsquad.recurring.Schedule;
import net.logicsquad.recurring.ScheduleElement;
import net.logicsquad.recurring.TemporalExpression;
import net.logicsquad.recurring.Union;

public class SampleRecurring {
    public static void main(String[] args) {
        TemporalExpression firstThursday = DayInMonth.of(DayOfWeek.THURSDAY, 1);
        TemporalExpression lastWednesday = DayInMonth.of(DayOfWeek.WEDNESDAY, -1);
        ScheduleElement element = ScheduleElement.of("Meeting", Union.of(firstThursday, lastWednesday));
        Schedule schedule = Schedule.of(element);
        boolean today = schedule.isOccurring("Meeting", LocalDate.now());

        System.out.println(today);
        System.out.println("TemporalExpression:" + lastWednesday);
        System.out.println("ScheduleElement:" + element);
        System.out.println("Schedule:" + schedule);
    }

}
