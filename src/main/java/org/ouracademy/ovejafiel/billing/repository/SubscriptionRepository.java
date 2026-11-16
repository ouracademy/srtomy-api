package org.ouracademy.ovejafiel.billing.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.ouracademy.ovejafiel.billing.domain.Subscription;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionRepository extends JpaRepository<Subscription, PartyId> {

  List<Subscription> findByPaidUntil(LocalDate paidUntil);

  /**
   * Marks a reminder as sent; answers 0 when it already was, so it is never sent twice. Runs in the
   * transaction that queues the email, so both happen or neither does.
   */
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          """
          insert into billing_reminder (organization_id, paid_until, kind, sent_at)
          values (:organizationId, :paidUntil, :kind, now())
          on conflict do nothing
          """)
  int markReminded(UUID organizationId, LocalDate paidUntil, String kind);
}
