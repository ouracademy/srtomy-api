package org.ouracademy.ovejafiel.billing.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.billing.query.EntitlementsTests.CLOCK;
import static org.ouracademy.ovejafiel.billing.query.EntitlementsTests.NOW;
import static org.ouracademy.ovejafiel.billing.query.EntitlementsTests.POLICY;
import static org.ouracademy.ovejafiel.billing.query.EntitlementsTests.TODAY;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.billing.command.start_whatsapp_trial.StartWhatsappTrialCommand;
import org.ouracademy.ovejafiel.billing.command.start_whatsapp_trial.StartWhatsappTrialCommandHandler;
import org.ouracademy.ovejafiel.billing.domain.BillingCountry;
import org.ouracademy.ovejafiel.billing.domain.Feature;
import org.ouracademy.ovejafiel.billing.domain.Invoice;
import org.ouracademy.ovejafiel.billing.domain.InvoiceStatus;
import org.ouracademy.ovejafiel.billing.domain.Subscription;
import org.ouracademy.ovejafiel.billing.gateway.PaymentGateway;
import org.ouracademy.ovejafiel.billing.query.Entitlements;
import org.ouracademy.ovejafiel.billing.query.check_capacity.CheckCapacityQuery;
import org.ouracademy.ovejafiel.billing.query.check_capacity.CheckCapacityQueryHandler;
import org.ouracademy.ovejafiel.billing.query.check_feature.CheckFeatureQuery;
import org.ouracademy.ovejafiel.billing.query.check_feature.CheckFeatureQueryHandler;
import org.ouracademy.ovejafiel.billing.repository.InvoiceRepository;
import org.ouracademy.ovejafiel.billing.repository.SubscriptionRepository;
import org.ouracademy.ovejafiel.organization.query.count_locations.CountLocationsQueryHandler;
import org.ouracademy.ovejafiel.organization.query.count_locations.CountLocationsResult;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.query.count_people.CountPeopleQueryHandler;
import org.ouracademy.ovejafiel.person.query.count_people.CountPeopleResult;
import org.ouracademy.ovejafiel.shared.domain.PlanLimitException;

/** The handlers whose rules depend on the date, with the repositories and the gateway mocked. */
class BillingHandlersTests {

  final PartyId church = PartyId.generate();
  final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
  final InvoiceRepository invoices = mock(InvoiceRepository.class);
  final PaymentGateway gateway = mock(PaymentGateway.class);
  final FindChurchOfOrganizationQueryHandler findChurch =
      mock(FindChurchOfOrganizationQueryHandler.class);
  final CountPeopleQueryHandler countPeople = mock(CountPeopleQueryHandler.class);
  final CountLocationsQueryHandler countLocations = mock(CountLocationsQueryHandler.class);
  final Entitlements entitlements = new Entitlements(subscriptions, findChurch, POLICY, CLOCK);
  final InvoiceSettler settler = new InvoiceSettler(invoices, subscriptions, gateway, CLOCK);

  @BeforeEach
  void aChurch() {
    when(findChurch.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    when(subscriptions.findById(church)).thenReturn(Optional.empty());
  }

  Subscription subscription() {
    var subscription = new Subscription(church);
    when(subscriptions.findById(church)).thenReturn(Optional.of(subscription));
    return subscription;
  }

  <T> T handler(String className, Object... args) {
    try {
      var constructor = Class.forName(className).getDeclaredConstructors()[0];
      constructor.setAccessible(true);
      @SuppressWarnings("unchecked")
      var instance = (T) constructor.newInstance(args);
      return instance;
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  // --- WhatsApp trial

  StartWhatsappTrialCommandHandler trialHandler() {
    return handler(
        "org.ouracademy.ovejafiel.billing.command.start_whatsapp_trial."
            + "StartWhatsappTrialCommandHandlerImpl",
        entitlements,
        subscriptions,
        POLICY,
        CLOCK);
  }

  @Test
  void theFirstConnectionOfAFreeChurchStartsTheTrial() {
    var result = trialHandler().handle(new StartWhatsappTrialCommand(church));

    assertThat(result.trialEndsAt()).isEqualTo(NOW.plusDays(15));
    var saved = org.mockito.ArgumentCaptor.forClass(Subscription.class);
    verify(subscriptions).save(saved.capture());
    assertThat(saved.getValue().getWhatsappTrialStartedAt()).isEqualTo(NOW);
  }

  @Test
  void connectingAgainDuringTheTrialKeepsItsEnd() {
    subscription().startWhatsappTrial(NOW.minusDays(10));

    var result = trialHandler().handle(new StartWhatsappTrialCommand(church));

    assertThat(result.trialEndsAt()).isEqualTo(NOW.plusDays(5));
  }

  @Test
  void onceTheTrialEndedAFreeChurchCannotConnect() {
    subscription().startWhatsappTrial(NOW.minusDays(16));

    assertThatThrownBy(() -> trialHandler().handle(new StartWhatsappTrialCommand(church)))
        .isInstanceOf(PlanLimitException.class)
        .extracting("code")
        .isEqualTo("feature.whatsapp");
    verify(subscriptions, never()).save(any());
  }

  @Test
  void aPaidChurchConnectsWithoutAnyTrial() {
    subscription().extendTo(TODAY.plusDays(20), BillingCountry.PE);

    var result = trialHandler().handle(new StartWhatsappTrialCommand(church));

    assertThat(result.trialEndsAt()).isNull();
    verify(subscriptions, never()).save(any());
  }

  // --- features and capacity

  @Test
  void aFeatureOfThePaidPlanIsRefusedToAFreeChurchAndAllowedToAPaidOne() {
    CheckFeatureQueryHandler check =
        handler(
            "org.ouracademy.ovejafiel.billing.query.check_feature.CheckFeatureQueryHandlerImpl",
            entitlements);

    assertThatThrownBy(() -> check.handle(new CheckFeatureQuery(church, Feature.FINANCES)))
        .isInstanceOf(PlanLimitException.class)
        .hasMessageContaining("plan Iglesia")
        .extracting("code")
        .isEqualTo("feature.finances");

    subscription().extendTo(TODAY, BillingCountry.PE);
    assertThat(check.handle(new CheckFeatureQuery(church, Feature.FINANCES)).feature())
        .isEqualTo(Feature.FINANCES);
  }

  @Test
  void anExpiredTrialRefusesWhatsappWithItsOwnMessage() {
    CheckFeatureQueryHandler check =
        handler(
            "org.ouracademy.ovejafiel.billing.query.check_feature.CheckFeatureQueryHandlerImpl",
            entitlements);
    subscription().startWhatsappTrial(NOW.minusDays(30));

    assertThatThrownBy(() -> check.handle(new CheckFeatureQuery(church, Feature.WHATSAPP)))
        .isInstanceOf(PlanLimitException.class)
        .hasMessageContaining("prueba gratuita");
  }

  @Test
  void aPaidChurchHasNoMaximumEvenAboveTheFreeOne() {
    CheckCapacityQueryHandler check =
        handler(
            "org.ouracademy.ovejafiel.billing.query.check_capacity.CheckCapacityQueryHandlerImpl",
            entitlements,
            POLICY,
            countPeople,
            countLocations);
    when(countPeople.handle(any())).thenReturn(new CountPeopleResult(900));
    when(countLocations.handle(any())).thenReturn(new CountLocationsResult(1));

    // Free: 49 fit, 50 do not; one location is the maximum.
    when(countPeople.handle(any())).thenReturn(new CountPeopleResult(49));
    assertThat(
            check.handle(new CheckCapacityQuery(church, CheckCapacityQuery.Resource.PEOPLE)).max())
        .isEqualTo(50);
    when(countPeople.handle(any())).thenReturn(new CountPeopleResult(50));
    assertThatThrownBy(
            () -> check.handle(new CheckCapacityQuery(church, CheckCapacityQuery.Resource.PEOPLE)))
        .isInstanceOf(PlanLimitException.class)
        .hasMessageContaining("50 personas");
    assertThatThrownBy(
            () ->
                check.handle(new CheckCapacityQuery(church, CheckCapacityQuery.Resource.LOCATIONS)))
        .isInstanceOf(PlanLimitException.class)
        .hasMessageContaining("1 sede.");

    subscription().extendTo(TODAY, BillingCountry.PE);
    when(countPeople.handle(any())).thenReturn(new CountPeopleResult(900));
    assertThat(
            check.handle(new CheckCapacityQuery(church, CheckCapacityQuery.Resource.PEOPLE)).max())
        .isNull();
  }

  // --- settling a payment

  Invoice pendingInvoice() {
    var invoice = new Invoice(church, BillingCountry.PE, new BigDecimal("75"), 20, 1);
    invoice.sentToGateway("DP-1", "https://checkout/1");
    when(invoices.findForUpdate(invoice.getId())).thenReturn(Optional.of(invoice));
    return invoice;
  }

  @Test
  void aPaymentOfAnotherOrderIsNeverCreditedToTheChurch() {
    var invoice = pendingInvoice();
    when(gateway.findPayment("DP-1"))
        .thenReturn(new PaymentGateway.PaymentState("DP-1", "another-order", InvoiceStatus.PAID));

    assertThat(settler.settle(invoice)).isFalse();

    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PENDING);
    verify(subscriptions, never()).save(any());
  }

  @Test
  void aRejectedPaymentClosesTheInvoiceAndGivesNoPaidTime() {
    var invoice = pendingInvoice();
    when(gateway.findPayment("DP-1"))
        .thenReturn(
            new PaymentGateway.PaymentState(
                "DP-1", invoice.getId().value().toString(), InvoiceStatus.REJECTED));

    assertThat(settler.settle(invoice)).isFalse();

    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.REJECTED);
    verify(subscriptions, never()).save(any());
    verify(invoices).save(invoice);
  }

  @Test
  void aPaidPaymentExtendsThePlanFromToday() {
    var invoice = pendingInvoice();
    when(gateway.findPayment("DP-1"))
        .thenReturn(
            new PaymentGateway.PaymentState(
                "DP-1", invoice.getId().value().toString(), InvoiceStatus.PAID));

    assertThat(settler.settle(invoice)).isTrue();

    var saved = org.mockito.ArgumentCaptor.forClass(Subscription.class);
    verify(subscriptions).save(saved.capture());
    assertThat(saved.getValue().getPaidUntil()).isEqualTo(TODAY.plusMonths(1).minusDays(1));
  }

  @Test
  void anInvoiceAlreadySettledOrNeverSentIsNotAskedAbout() {
    var invoice = new Invoice(church, BillingCountry.PE, new BigDecimal("75"), 20, 1);
    when(invoices.findForUpdate(invoice.getId())).thenReturn(Optional.of(invoice));

    assertThat(settler.settle(invoice)).isFalse();
    when(invoices.findForUpdate(invoice.getId())).thenReturn(Optional.empty());
    assertThat(settler.settle(invoice)).isFalse();

    org.mockito.Mockito.verifyNoInteractions(gateway);
  }
}
