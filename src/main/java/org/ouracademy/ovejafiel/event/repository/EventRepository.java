package org.ouracademy.ovejafiel.event.repository;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventSeries;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<Event, EventId> {

  Optional<Event> findByShareSlug(String shareSlug);

  List<Event> findBySeries(EventSeries series);

  List<Event> findByParentOrderByStart(Event parent);

  boolean existsByParent(Event parent);

  List<Event> findBySeriesAndStartGreaterThanEqual(EventSeries series, ZonedDateTime start);

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
            select e from Event e
            where e.start <= :end and e.end>=:start
            and e.organization.id in :organizationIds""")
  List<Event> searchEventsOfOrgIdsAndBetweenDates(
      List<PartyId> organizationIds, ZonedDateTime start, ZonedDateTime end);

  /**
   * The events of the nodes where {@code userId} holds {@code events.read}, and of everything below
   * them, that overlap the range. Read-only SQL over the access view, which the module rules allow.
   */
  @Query(
      value =
          """
          with recursive bound as (
              select ua.organization_id as id
              from user_access ua
              where ua.user_id = :userId and 'events.read' = any(ua.permissions)
          ),
          subtree as (
              select o.id from organization o where o.id in (select id from bound)
              union
              select o.id from organization o join subtree s on o.parent_id = s.id
          )
          select e.* from event e
          where e.organization_id in (select id from subtree)
            and e.deleted_time is null
            and e.end_time >= :from and e.start_time <= :to
          order by e.start_time
          limit :max
          """,
      nativeQuery = true)
  List<Event> findReadableByUser(UUID userId, Instant from, Instant to, int max);

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
                 e.share_slug as slug,
                 max(cast(x.registration_id as text)) as registrationId,
                 max(x.ticket_name) as ticketName,
                 max(x.registration_status) as registrationStatus,
                 max(x.cancel_code) as cancelCode,
                 max(x.ticket_code) as ticketCode
          from (
              select er.event_id as event_id, true as registered,
                     (er.checked_in_at is not null) as attended,
                     er.id as registration_id, tt.name as ticket_name,
                     er.status as registration_status, er.cancel_code as cancel_code,
                     er.ticket_code as ticket_code
              from event_registration er
              join ticket_type tt on tt.id = er.ticket_type_id
              where er.status <> 'CANCELLED'
                and (er.user_id = :userId
                     or (cast(:email as text) is not null
                         and lower(er.email) = lower(cast(:email as text))))
              union all
              select ea.id, false, true, cast(null as uuid), cast(null as text),
                     cast(null as text), cast(null as text), cast(null as text)
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

    String getRegistrationId();

    String getTicketName();

    String getRegistrationStatus();

    String getCancelCode();

    String getTicketCode();
  }
}
