package org.ouracademy.ovejafiel.billing.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;

/**
 * What a church has paid for. A church with no row is on the free plan. Paying is prepaid: each
 * paid invoice moves {@code paidUntil} forward and says which plan (and extras) the church has from
 * then on.
 */
@Entity
@Table(name = "billing_subscription")
@Getter
public class Subscription extends DomainObject {

  @Id PartyId organizationId;

  /** Where the church pays from; null until its first checkout. */
  @Enumerated(EnumType.STRING)
  BillingCountry country;

  /** Last day of the paid period; null while the church never paid. */
  LocalDate paidUntil;

  /** The plan of the last payment; null while the church never paid. */
  @Enumerated(EnumType.STRING)
  Plan plan;

  /** The own WhatsApp number was bought as an extra with the last payment. */
  boolean ownNumber;

  protected Subscription() {
    // jpa only
  }

  public Subscription(PartyId organizationId) {
    this.organizationId = organizationId;
  }

  public SubscriptionStatus status(LocalDate today, int graceDays) {
    if (paidUntil == null) {
      return SubscriptionStatus.FREE;
    }
    if (!today.isAfter(paidUntil)) {
      return SubscriptionStatus.ACTIVE;
    }
    return today.isAfter(paidUntil.plusDays(graceDays))
        ? SubscriptionStatus.EXPIRED
        : SubscriptionStatus.GRACE;
  }

  /**
   * The first day a new payment covers: the day after what is already paid, or {@code today} when
   * nothing is, so that renewing early never loses days.
   */
  public LocalDate nextPeriodStart(LocalDate today) {
    return paidUntil == null || paidUntil.isBefore(today) ? today : paidUntil.plusDays(1);
  }

  /**
   * The paid period now ends at {@code periodEnd}, and the church has the plan of the payment. A
   * payment of another plan changes the plan at once: there is no proration.
   */
  public void extendTo(LocalDate periodEnd, BillingCountry country, Plan plan, boolean ownNumber) {
    if (paidUntil == null || periodEnd.isAfter(paidUntil)) {
      this.paidUntil = periodEnd;
    }
    this.country = country;
    this.plan = plan;
    this.ownNumber = ownNumber;
  }
}
