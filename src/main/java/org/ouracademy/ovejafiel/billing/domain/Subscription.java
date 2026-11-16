package org.ouracademy.ovejafiel.billing.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainObject;

/**
 * What a church has paid for. A church with no row is on the free plan and has not tried WhatsApp.
 * The paid plan is prepaid: each paid invoice moves {@code paidUntil} forward.
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

  /** When the free trial of WhatsApp started; null while it was never connected. */
  LocalDateTime whatsappTrialStartedAt;

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

  public void extendTo(LocalDate periodEnd, BillingCountry country) {
    if (paidUntil == null || periodEnd.isAfter(paidUntil)) {
      this.paidUntil = periodEnd;
    }
    this.country = country;
  }

  /** The trial starts once and never restarts. */
  public void startWhatsappTrial(LocalDateTime now) {
    if (whatsappTrialStartedAt == null) {
      this.whatsappTrialStartedAt = now;
    }
  }

  /** When the trial ends; null while it has not started. */
  public LocalDateTime whatsappTrialEndsAt(int trialDays) {
    return whatsappTrialStartedAt == null ? null : whatsappTrialStartedAt.plusDays(trialDays);
  }
}
