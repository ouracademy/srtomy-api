package org.ouracademy.srtomy.event.repository;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ouracademy.srtomy.event.domain.Event;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.domain.SingleEvent;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<Event, EventId> {

  Optional<Event> findByShareSlug(String shareSlug);

  // t: -----|----|----
  // e: --|-|--------- X

  // t: -----|----|----
  // e: ----|--|-------

  // t: -----|----|----
  // e: -------|-|-----

  // t: -----|----|----
  // e: -------|----|--

  // t: -----|----|---- X
  // e: -----------|-|-
  @Query(
      """
            select e from SingleEvent e
            where e.start <= :end and e.end>=:start
            and e.organization.id in :organizationIds""")
  List<SingleEvent> searchEventsOfOrgIdsAndBetweenDates(
      List<PartyId> organizationIds, ZonedDateTime start, ZonedDateTime end);

  /**
   * The events {@code userId} signed up for (by account or by verified {@code email}) or whose
   * attendance lists the person linked to them, most recent first. Read-only SQL over the
   * attendance and link tables, which the module rules allow.
   */
  @Query(
      value =
          """
          select e.id as id, e.name as title, e.start_time as start,
                 e.is_a_course as isACourse, p.name as organizationName,
                 bool_or(x.registered) as registered, bool_or(x.attended) as attended,
                 e.share_slug as slug
          from (
              select er.event_id as event_id, true as registered, false as attended
              from event_registration er
              where er.user_id = :userId
                 or (cast(:email as text) is not null
                     and lower(er.email) = lower(cast(:email as text)))
              union all
              select ea.id, false, true
              from event_attendance ea
              join person_event_attendance pea on pea.event_attendance_id = ea.id
              join user_person up on up.person_id = pea.person_id
              where up.user_id = :userId and ea.deleted_time is null
          ) x
          join event e on e.id = x.event_id and e.deleted_time is null
          join party p on p.id = e.organization_id
          group by e.id, e.name, e.start_time, e.is_a_course, p.name, e.share_slug
          order by e.start_time desc
          """,
      nativeQuery = true)
  List<MyEventRow> findOfUser(UUID userId, String email);

  interface MyEventRow {
    String getTitle();

    Instant getStart();

    Boolean getIsACourse();

    String getOrganizationName();

    boolean getRegistered();

    boolean getAttended();

    String getSlug();
  }
}
