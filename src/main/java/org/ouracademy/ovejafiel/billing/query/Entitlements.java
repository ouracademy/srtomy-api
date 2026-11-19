package org.ouracademy.ovejafiel.billing.query;

import java.time.Clock;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.billing.domain.BillingPolicy;
import org.ouracademy.ovejafiel.billing.domain.Feature;
import org.ouracademy.ovejafiel.billing.domain.Plan;
import org.ouracademy.ovejafiel.billing.domain.Subscription;
import org.ouracademy.ovejafiel.billing.domain.SubscriptionStatus;
import org.ouracademy.ovejafiel.billing.repository.SubscriptionRepository;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.stereotype.Component;

/**
 * Reads what the plan of a church allows right now. The plan belongs to the church, so any node of
 * its tree gives the same answer. A church whose paid time ran out (and its grace days) is back on
 * the free plan, with nothing deleted.
 */
@Component
@RequiredArgsConstructor
public class Entitlements {

  private final SubscriptionRepository subscriptions;
  private final FindChurchOfOrganizationQueryHandler findChurch;
  private final BillingPolicy policy;
  private final Clock clock;

  /**
   * @param subscription null when the church never paid
   * @param plan the plan in force today: the paid one while it lasts, {@code FREE} otherwise
   * @param ownNumber the own WhatsApp number was bought as an extra and is still paid
   * @param limits what that plan includes
   */
  public record State(
      PartyId churchId,
      Subscription subscription,
      SubscriptionStatus status,
      Plan plan,
      boolean ownNumber,
      BillingPolicy.Limits limits) {

    public boolean isPaid() {
      return plan.isPaid();
    }

    /** The plan includes the feature; the own number can also be bought as an extra. */
    public boolean allows(Feature feature) {
      return limits.includes(feature) || (feature == Feature.OWN_NUMBER && ownNumber);
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
      return state(churchId, null, SubscriptionStatus.FREE, Plan.FREE, false);
    }
    var status = subscription.status(LocalDate.now(clock), policy.graceDays());
    var plan =
        status.isPaid() && subscription.getPlan() != null ? subscription.getPlan() : Plan.FREE;
    return state(churchId, subscription, status, plan, plan.isPaid() && subscription.isOwnNumber());
  }

  private State state(
      PartyId churchId,
      Subscription subscription,
      SubscriptionStatus status,
      Plan plan,
      boolean ownNumber) {
    return new State(churchId, subscription, status, plan, ownNumber, policy.limits(plan));
  }
}
