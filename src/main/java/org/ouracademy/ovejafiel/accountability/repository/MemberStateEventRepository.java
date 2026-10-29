package org.ouracademy.ovejafiel.accountability.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.ouracademy.ovejafiel.accountability.domain.MemberStateEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MemberStateEventRepository extends JpaRepository<MemberStateEvent, UUID> {

  /** Every event in those organizations, in the order they happened. */
  @Query(
      value =
          """
          select e.person_id as personId, e.organization_id as organizationId,
                 e.from_state as fromState, e.to_state as toState, e.occurred_at as occurredAt
          from member_state_event e
          where e.organization_id in (:organizationIds)
          order by e.occurred_at, e.id
          """,
      nativeQuery = true)
  List<EventRow> findByOrganizations(Collection<UUID> organizationIds);

  interface EventRow {
    UUID getPersonId();

    UUID getOrganizationId();

    String getFromState();

    String getToState();

    LocalDateTime getOccurredAt();
  }
}
