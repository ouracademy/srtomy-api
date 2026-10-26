package org.ouracademy.ovejafiel.attendance;

import java.util.List;
import org.ouracademy.ovejafiel.attendance.domain.EventAttendance;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.springframework.test.util.ReflectionTestUtils;

/** Test helpers for the attendance module's tests. */
public final class AttendanceFixtures {

  private AttendanceFixtures() {}

  /** An attendance as loaded from the database: its id is the event's id (via {@code @MapsId}). */
  public static EventAttendance eventAttendance(Event event, List<Person> people) {
    var attendance = new EventAttendance(event);
    ReflectionTestUtils.setField(attendance, "id", event.getId());
    attendance.setPeople(people);
    return attendance;
  }
}
