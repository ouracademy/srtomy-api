package org.ouracademy.ovejafiel.billing.query;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.domain.BillingPolicy;
import org.ouracademy.ovejafiel.billing.domain.Feature;
import org.ouracademy.ovejafiel.billing.domain.Subscription;
import org.ouracademy.ovejafiel.billing.domain.SubscriptionStatus;
import org.ouracademy.ovejafiel.billing.repository.SubscriptionRepository;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.stereotype.Component;

/**
 * Reads what the plan of a church allows right now. The plan belongs to the church, so any node of
 * its tree gives the same answer.
 */
@Component
@RequiredArgsConstructor
public class Entitlements {

  private final SubscriptionRepository subscriptions;
  private final FindChurchOfOrganizationQueryHandler findChurch;
  private final BillingPolicy policy;
  private final Clock clock;

  /**
   * @param subscription null when the church never paid nor tried WhatsApp
   */
  public record State(
      PartyId churchId,
      Subscription subscription,
      SubscriptionStatus status,
      LocalDateTime whatsappTrialEndsAt,
      boolean whatsappTrialRunning) {

    public boolean isPaid() {
      return status.isPaid();
    }

    /** WhatsApp is the one feature the free plan may use, while its trial lasts. */
    public boolean allows(Feature feature) {
      if (isPaid()) {
        return true;
      }
      return feature == Feature.WHATSAPP && (whatsappTrialEndsAt == null || whatsappTrialRunning);
    }
  }

  /**
   * @throws NoSuchElementException when {@code organizationId} is not a node
   */
  public State of(PartyId organizationId) {
    var churchId =
        findChurch
            .handle(new FindChurchOfOrganizationQuery(organizationId))
            .orElseThrow(() -> new NoSuchElementException("Organization not found"))
            .churchId();
    var subscription = subscriptions.findById(churchId).orElse(null);
    if (subscription == null) {
      return new State(churchId, null, SubscriptionStatus.FREE, null, false);
    }
    var trialEndsAt = subscription.whatsappTrialEndsAt(policy.whatsappTrialDays());
    return new State(
        churchId,
        subscription,
        subscription.status(LocalDate.now(clock), policy.graceDays()),
        trialEndsAt,
        trialEndsAt != null && LocalDateTime.now(clock).isBefore(trialEndsAt));
  }
}
