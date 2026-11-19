package org.ouracademy.ovejafiel.billing.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.billing.domain.BillingCountry;
import org.ouracademy.ovejafiel.billing.domain.BillingPolicy;
import org.ouracademy.ovejafiel.billing.domain.BillingPolicyTests;
import org.ouracademy.ovejafiel.billing.domain.Feature;
import org.ouracademy.ovejafiel.billing.domain.Plan;
import org.ouracademy.ovejafiel.billing.domain.Subscription;
import org.ouracademy.ovejafiel.billing.domain.SubscriptionStatus;
import org.ouracademy.ovejafiel.billing.repository.SubscriptionRepository;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;

/** What each plan allows, with the date fixed. */
public class EntitlementsTests {

  public static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-11-15T10:00:00Z"), ZoneOffset.UTC);
  public static final LocalDate TODAY = LocalDate.now(CLOCK);
  public static final LocalDateTime NOW = LocalDateTime.now(CLOCK);
  public static final BillingPolicy POLICY = BillingPolicyTests.POLICY;

  final PartyId church = PartyId.generate();
  final PartyId group = PartyId.generate();
  final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
  final FindChurchOfOrganizationQueryHandler findChurch =
      mock(FindChurchOfOrganizationQueryHandler.class);
  final Entitlements entitlements = new Entitlements(subscriptions, findChurch, POLICY, CLOCK);

  Subscription subscription() {
    when(findChurch.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    var subscription = new Subscription(church);
    when(subscriptions.findById(church)).thenReturn(Optional.of(subscription));
    return subscription;
  }

  @Test
  void aChurchWithNoRowIsFreeAndHasNoneOfTheFeatures() {
    when(findChurch.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    when(subscriptions.findById(church)).thenReturn(Optional.empty());

    var state = entitlements.of(group);

    assertThat(state.churchId()).isEqualTo(church);
    assertThat(state.status()).isEqualTo(SubscriptionStatus.FREE);
    assertThat(state.plan()).isEqualTo(Plan.FREE);
    assertThat(state.isPaid()).isFalse();
    assertThat(state.limits().maxPeople()).isEqualTo(50);
    for (var feature : Feature.values()) {
      assertThat(state.allows(feature)).isFalse();
    }
  }

  @Test
  void eachPaidPlanAllowsItsOwnFeatures() {
    var subscription = subscription();
    subscription.extendTo(TODAY.plusDays(10), BillingCountry.PE, Plan.BASIC, false);
    var basic = entitlements.of(church);
    assertThat(basic.plan()).isEqualTo(Plan.BASIC);
    assertThat(basic.limits().maxPeople()).isEqualTo(150);
    assertThat(basic.allows(Feature.CUSTOM_ROLES)).isTrue();
    assertThat(basic.allows(Feature.EXCEL_EXPORT)).isTrue();
    assertThat(basic.allows(Feature.FINANCES)).isFalse();
    assertThat(basic.allows(Feature.OWN_NUMBER)).isFalse();

    subscription.extendTo(TODAY.plusDays(10), BillingCountry.PE, Plan.CHURCH, false);
    var church = entitlements.of(this.church);
    assertThat(church.allows(Feature.FINANCES)).isTrue();
    assertThat(church.allows(Feature.MASS_EMAIL)).isTrue();
    assertThat(church.allows(Feature.OWN_NUMBER)).isFalse();

    subscription.extendTo(TODAY.plusDays(10), BillingCountry.PE, Plan.NETWORK, false);
    var network = entitlements.of(this.church);
    assertThat(network.limits().maxPeople()).isNull();
    for (var feature : Feature.values()) {
      assertThat(network.allows(feature)).isTrue();
    }
  }

  @Test
  void theOwnNumberBoughtAsAnExtraIsAllowedOnlyWhilePaid() {
    var subscription = subscription();
    subscription.extendTo(TODAY.plusDays(10), BillingCountry.PE, Plan.CHURCH, true);

    var paid = entitlements.of(church);
    assertThat(paid.ownNumber()).isTrue();
    assertThat(paid.allows(Feature.OWN_NUMBER)).isTrue();

    subscription.extendTo(TODAY.minusDays(8), BillingCountry.PE, Plan.CHURCH, true);
    // The earlier, longer period is kept: use a new row that ended long ago.
    var ended = new Subscription(church);
    ended.extendTo(TODAY.minusDays(8), BillingCountry.PE, Plan.CHURCH, true);
    when(subscriptions.findById(church)).thenReturn(Optional.of(ended));
    var expired = entitlements.of(church);
    assertThat(expired.allows(Feature.OWN_NUMBER)).isFalse();
    assertThat(expired.ownNumber()).isFalse();
  }

  @Test
  void thePaidPlanKeepsEverythingDuringTheGraceDays() {
    var subscription = subscription();
    subscription.extendTo(TODAY.minusDays(7), BillingCountry.PE, Plan.NETWORK, false);

    var state = entitlements.of(church);

    assertThat(state.status()).isEqualTo(SubscriptionStatus.GRACE);
    assertThat(state.plan()).isEqualTo(Plan.NETWORK);
    for (var feature : Feature.values()) {
      assertThat(state.allows(feature)).isTrue();
    }
  }

  @Test
  void anExpiredPlanGoesBackToTheFreeLimits() {
    var subscription = subscription();
    subscription.extendTo(TODAY.minusDays(8), BillingCountry.PE, Plan.NETWORK, false);

    var state = entitlements.of(church);

    assertThat(state.status()).isEqualTo(SubscriptionStatus.EXPIRED);
    assertThat(state.plan()).isEqualTo(Plan.FREE);
    assertThat(state.limits().maxPeople()).isEqualTo(50);
    assertThat(state.allows(Feature.FINANCES)).isFalse();
  }

  @Test
  void aNodeThatDoesNotExistIsNotFound() {
    when(findChurch.handle(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> entitlements.of(group)).isInstanceOf(NoSuchElementException.class);
  }
}
