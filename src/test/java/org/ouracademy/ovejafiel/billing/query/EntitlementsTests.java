package org.ouracademy.ovejafiel.billing.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.billing.domain.BillingCountry;
import org.ouracademy.ovejafiel.billing.domain.BillingPolicy;
import org.ouracademy.ovejafiel.billing.domain.Feature;
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
  public static final BillingPolicy POLICY =
      new BillingPolicy(
          50,
          1,
          15,
          150,
          100,
          10,
          7,
          Map.of(
              BillingCountry.PE,
              new BillingPolicy.Price(new BigDecimal("75"), new BigDecimal("35"))));

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
  void aChurchWithNoRowIsFreeAndMayStillTryWhatsapp() {
    when(findChurch.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    when(subscriptions.findById(church)).thenReturn(Optional.empty());

    var state = entitlements.of(group);

    assertThat(state.churchId()).isEqualTo(church);
    assertThat(state.status()).isEqualTo(SubscriptionStatus.FREE);
    assertThat(state.isPaid()).isFalse();
    assertThat(state.allows(Feature.WHATSAPP)).isTrue();
    assertThat(state.allows(Feature.FINANCES)).isFalse();
    assertThat(state.allows(Feature.MASS_EMAIL)).isFalse();
    assertThat(state.allows(Feature.VOLUNTEERS)).isFalse();
    assertThat(state.allows(Feature.EXCEL_EXPORT)).isFalse();
  }

  @Test
  void duringTheTrialWhatsappWorksAndAfterItDoesNot() {
    var subscription = subscription();
    subscription.startWhatsappTrial(NOW.minusDays(14));

    var running = entitlements.of(church);
    assertThat(running.whatsappTrialRunning()).isTrue();
    assertThat(running.allows(Feature.WHATSAPP)).isTrue();
    assertThat(running.whatsappTrialEndsAt()).isEqualTo(NOW.plusDays(1));

    var ended = new Subscription(church);
    ended.startWhatsappTrial(NOW.minusDays(15));
    when(subscriptions.findById(church)).thenReturn(Optional.of(ended));
    var over = entitlements.of(church);
    assertThat(over.whatsappTrialRunning()).isFalse();
    assertThat(over.allows(Feature.WHATSAPP)).isFalse();
  }

  @Test
  void thePaidPlanAllowsEverythingAlsoDuringTheGraceDays() {
    var subscription = subscription();
    subscription.startWhatsappTrial(NOW.minusDays(60));
    subscription.extendTo(TODAY.minusDays(7), BillingCountry.PE);

    var state = entitlements.of(church);

    assertThat(state.status()).isEqualTo(SubscriptionStatus.GRACE);
    for (var feature : Feature.values()) {
      assertThat(state.allows(feature)).isTrue();
    }
  }

  @Test
  void anExpiredPlanGoesBackToTheFreeLimits() {
    var subscription = subscription();
    subscription.startWhatsappTrial(NOW.minusDays(60));
    subscription.extendTo(TODAY.minusDays(8), BillingCountry.PE);

    var state = entitlements.of(church);

    assertThat(state.status()).isEqualTo(SubscriptionStatus.EXPIRED);
    assertThat(state.allows(Feature.FINANCES)).isFalse();
    assertThat(state.allows(Feature.WHATSAPP)).isFalse();
  }

  @Test
  void aNodeThatDoesNotExistIsNotFound() {
    when(findChurch.handle(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> entitlements.of(group)).isInstanceOf(NoSuchElementException.class);
  }
}
