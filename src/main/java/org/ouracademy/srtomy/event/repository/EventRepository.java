package org.ouracademy.srtomy.event.repository;

import java.time.ZonedDateTime;
import java.util.List;
import org.ouracademy.srtomy.event.domain.Event;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.domain.SingleEvent;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EventRepository extends JpaRepository<Event, EventId> {

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
            and e.organization.id=:organizationId""")
  List<SingleEvent> searchEventsOfOrgIdAndBetweenDates(
      PartyId organizationId, ZonedDateTime start, ZonedDateTime end);
}
