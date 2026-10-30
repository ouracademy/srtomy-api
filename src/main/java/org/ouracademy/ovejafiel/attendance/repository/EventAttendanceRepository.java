package org.ouracademy.ovejafiel.attendance.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ouracademy.ovejafiel.attendance.domain.EventAttendance;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EventAttendanceRepository extends JpaRepository<EventAttendance, EventId> {

  Optional<EventAttendance> findByEventId(EventId id);

  /** The attendance sheets of several events, e.g. to aggregate stats over a date range. */
  @Query("select a from EventAttendance a where a.event.id in :eventIds")
  List<EventAttendance> findByEventIds(Collection<EventId> eventIds);

  /** Makes sure the event has its attendance sheet, even when two people mark at the same time. */
  @Modifying
  @Query(
      value =
          "insert into event_attendance (id, date_time) values (:eventId, now()) "
              + "on conflict (id) do nothing",
      nativeQuery = true)
  void createIfMissing(UUID eventId);

  /** Adds visitors without a name in one step, so two ushers counting at once do not overwrite. */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update EventAttendance a set a.guestCount = a.guestCount + :count where a.id = :eventId")
  void addGuests(EventId eventId, int count);
}
