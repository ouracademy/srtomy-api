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

  /** The sheets, among those of {@code eventIds}, with someone present or visitors counted. */
  @Query(
      "select a from EventAttendance a where a.id in :eventIds "
          + "and (a.guestCount > 0 or size(a.personEventAttendances) > 0)")
  List<EventAttendance> findWithAttendance(Collection<EventId> eventIds);

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

  /**
   * How many came to {@code eventId}, without counting anyone twice: the people marked in the
   * attendance, plus the sign-ups that came in with their ticket and whose account is not already
   * one of those people. A sub-event that inherits the sign-up of its mega event counts the entries
   * recorded in it; any other event counts the check-in of its own sign-ups. Visitors without a
   * name are counted apart. Read-only SQL over the sign-up and link tables.
   */
  @Query(
      value =
          """
          with entries as (
              select er.user_id as user_id
              from event_registration er
              where er.event_id = :eventId and er.status <> 'CANCELLED'
                and er.checked_in_at is not null
              union all
              select er.user_id
              from event_registration_check_in ci
              join event_registration er on er.id = ci.registration_id
              where ci.event_id = :eventId and er.status <> 'CANCELLED'
          ),
          marked as (
              select pea.person_id as person_id
              from person_event_attendance pea
              join event_attendance ea on ea.id = pea.event_attendance_id
              where ea.id = :eventId and ea.deleted_time is null
          )
          select
              (select count(*) from marked) as people,
              (select count(*) from entries e
                where e.user_id is null
                   or not exists (select 1 from user_person up
                                  join marked m on m.person_id = up.person_id
                                  where up.user_id = e.user_id)) as ticketOnly,
              coalesce((select ea.guest_count from event_attendance ea
                        where ea.id = :eventId and ea.deleted_time is null), 0) as guests
          """,
      nativeQuery = true)
  AttendanceTotals totalsOf(UUID eventId);

  interface AttendanceTotals {
    long getPeople();

    long getTicketOnly();

    long getGuests();
  }
}
