package org.ouracademy.ovejafiel.billing.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.billing.query.EntitlementsTests.CLOCK;
import static org.ouracademy.ovejafiel.billing.query.EntitlementsTests.POLICY;
import static org.ouracademy.ovejafiel.billing.query.EntitlementsTests.TODAY;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.billing.domain.BillingCountry;
import org.ouracademy.ovejafiel.billing.domain.Feature;
import org.ouracademy.ovejafiel.billing.domain.Invoice;
import org.ouracademy.ovejafiel.billing.domain.InvoiceStatus;
import org.ouracademy.ovejafiel.billing.domain.Plan;
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

  // --- features and capacity

  CheckFeatureQueryHandler featureCheck() {
    return handler(
        "org.ouracademy.ovejafiel.billing.query.check_feature.CheckFeatureQueryHandlerImpl",
        entitlements);
  }

  CheckCapacityQueryHandler capacityCheck() {
    return handler(
        "org.ouracademy.ovejafiel.billing.query.check_capacity.CheckCapacityQueryHandlerImpl",
        entitlements,
        countPeople,
        countLocations);
  }

  @Test
  void aFeatureIsRefusedToAPlanThatDoesNotHaveItAndSaysWhichPlanHasIt() {
    var check = featureCheck();

    assertThatThrownBy(() -> check.handle(new CheckFeatureQuery(church, Feature.FINANCES)))
        .isInstanceOf(PlanLimitException.class)
        .hasMessageContaining("plan Iglesia")
        .extracting("code")
        .isEqualTo("feature.finances");
    assertThatThrownBy(() -> check.handle(new CheckFeatureQuery(church, Feature.EXCEL_EXPORT)))
        .hasMessageContaining("plan Básico")
        .extracting("code")
        .isEqualTo("feature.excel_export");

    subscription().extendTo(TODAY, BillingCountry.PE, Plan.BASIC, false);
    assertThat(check.handle(new CheckFeatureQuery(church, Feature.EXCEL_EXPORT)).feature())
        .isEqualTo(Feature.EXCEL_EXPORT);
    assertThat(check.handle(new CheckFeatureQuery(church, Feature.CUSTOM_ROLES)).feature())
        .isEqualTo(Feature.CUSTOM_ROLES);
    // Basic still has no finances.
    assertThatThrownBy(() -> check.handle(new CheckFeatureQuery(church, Feature.FINANCES)))
        .isInstanceOf(PlanLimitException.class);

    subscription().extendTo(TODAY, BillingCountry.PE, Plan.CHURCH, false);
    assertThat(check.handle(new CheckFeatureQuery(church, Feature.FINANCES)).feature())
        .isEqualTo(Feature.FINANCES);
  }

  @Test
  void theOwnNumberNeedsNetworkOrTheExtra() {
    var check = featureCheck();

    // Free, Basic and Church without the extra are refused with their own code and message.
    for (var plan : new Plan[] {Plan.BASIC, Plan.CHURCH}) {
      subscription().extendTo(TODAY, BillingCountry.PE, plan, false);
      assertThatThrownBy(() -> check.handle(new CheckFeatureQuery(church, Feature.OWN_NUMBER)))
          .isInstanceOf(PlanLimitException.class)
          .hasMessageContaining("número de Sr. Tommy")
          .extracting("code")
          .isEqualTo("feature.own_number");
    }
    assertThatThrownBy(() -> check.handle(new CheckFeatureQuery(church, Feature.OWN_NUMBER)))
        .isInstanceOf(PlanLimitException.class);

    subscription().extendTo(TODAY, BillingCountry.PE, Plan.CHURCH, true);
    assertThat(check.handle(new CheckFeatureQuery(church, Feature.OWN_NUMBER)).feature())
        .isEqualTo(Feature.OWN_NUMBER);

    subscription().extendTo(TODAY, BillingCountry.PE, Plan.NETWORK, false);
    assertThat(check.handle(new CheckFeatureQuery(church, Feature.OWN_NUMBER)).feature())
        .isEqualTo(Feature.OWN_NUMBER);
  }

  @Test
  void aChurchCannotAddMoreThanItsPlanIncludesAndIsToldWhichPlanComesNext() {
    var check = capacityCheck();
    when(countLocations.handle(any())).thenReturn(new CountLocationsResult(1));

    // Free: 49 fit, 50 do not.
    when(countPeople.handle(any())).thenReturn(new CountPeopleResult(49));
    assertThat(
            check.handle(new CheckCapacityQuery(church, CheckCapacityQuery.Resource.PEOPLE)).max())
        .isEqualTo(50);
    when(countPeople.handle(any())).thenReturn(new CountPeopleResult(50));
    assertThatThrownBy(
            () -> check.handle(new CheckCapacityQuery(church, CheckCapacityQuery.Resource.PEOPLE)))
        .isInstanceOf(PlanLimitException.class)
        .hasMessageContaining("plan Gratis incluye hasta 50 personas")
        .hasMessageContaining("plan Básico")
        .extracting("code")
        .isEqualTo("limit.people");
    assertThatThrownBy(
            () ->
                check.handle(new CheckCapacityQuery(church, CheckCapacityQuery.Resource.LOCATIONS)))
        .hasMessageContaining("1 sede.")
        .extracting("code")
        .isEqualTo("limit.locations");
  }

  @Test
  void eachPlanHasItsOwnMaximumOfPeopleAndLocations() {
    var check = capacityCheck();
    var people = CheckCapacityQuery.Resource.PEOPLE;
    var locations = CheckCapacityQuery.Resource.LOCATIONS;

    subscription().extendTo(TODAY, BillingCountry.PE, Plan.BASIC, false);
    when(countPeople.handle(any())).thenReturn(new CountPeopleResult(149));
    assertThat(check.handle(new CheckCapacityQuery(church, people)).max()).isEqualTo(150);
    when(countPeople.handle(any())).thenReturn(new CountPeopleResult(150));
    assertThatThrownBy(() -> check.handle(new CheckCapacityQuery(church, people)))
        .hasMessageContaining("plan Básico incluye hasta 150 personas")
        .hasMessageContaining("plan Iglesia");
    when(countLocations.handle(any())).thenReturn(new CountLocationsResult(1));
    assertThatThrownBy(() -> check.handle(new CheckCapacityQuery(church, locations)))
        .hasMessageContaining("plan Básico incluye 1 sede");

    subscription().extendTo(TODAY, BillingCountry.PE, Plan.CHURCH, false);
    when(countPeople.handle(any())).thenReturn(new CountPeopleResult(399));
    assertThat(check.handle(new CheckCapacityQuery(church, people)).max()).isEqualTo(400);
    when(countPeople.handle(any())).thenReturn(new CountPeopleResult(400));
    assertThatThrownBy(() -> check.handle(new CheckCapacityQuery(church, people)))
        .hasMessageContaining("plan Iglesia incluye hasta 400 personas")
        .hasMessageContaining("plan Red");
    when(countLocations.handle(any())).thenReturn(new CountLocationsResult(4));
    assertThat(check.handle(new CheckCapacityQuery(church, locations)).max()).isEqualTo(5);
    when(countLocations.handle(any())).thenReturn(new CountLocationsResult(5));
    assertThatThrownBy(() -> check.handle(new CheckCapacityQuery(church, locations)))
        .hasMessageContaining("plan Iglesia incluye 5 sedes");

    subscription().extendTo(TODAY, BillingCountry.PE, Plan.NETWORK, false);
    when(countPeople.handle(any())).thenReturn(new CountPeopleResult(100_000));
    when(countLocations.handle(any())).thenReturn(new CountLocationsResult(300));
    assertThat(check.handle(new CheckCapacityQuery(church, people)).max()).isNull();
    assertThat(check.handle(new CheckCapacityQuery(church, locations)).max()).isNull();
  }

  // --- settling a payment

  Invoice pendingInvoice() {
    var invoice =
        new Invoice(church, Plan.CHURCH, false, BillingCountry.PE, new BigDecimal("75"), 20, 1);
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
    assertThat(saved.getValue().getPlan()).isEqualTo(Plan.CHURCH);
    assertThat(saved.getValue().isOwnNumber()).isFalse();
  }

  @Test
  void aPaidInvoiceGivesThePlanAndTheExtraItWasBoughtWith() {
    var invoice =
        new Invoice(church, Plan.BASIC, true, BillingCountry.CO, new BigDecimal("98000"), 20, 1);
    invoice.sentToGateway("DP-2", "https://checkout/2");
    when(invoices.findForUpdate(invoice.getId())).thenReturn(Optional.of(invoice));
    when(gateway.findPayment("DP-2"))
        .thenReturn(
            new PaymentGateway.PaymentState(
                "DP-2", invoice.getId().value().toString(), InvoiceStatus.PAID));

    assertThat(settler.settle(invoice)).isTrue();

    var saved = org.mockito.ArgumentCaptor.forClass(Subscription.class);
    verify(subscriptions).save(saved.capture());
    assertThat(saved.getValue().getPlan()).isEqualTo(Plan.BASIC);
    assertThat(saved.getValue().isOwnNumber()).isTrue();
    assertThat(saved.getValue().getCountry()).isEqualTo(BillingCountry.CO);
  }

  @Test
  void anInvoiceAlreadySettledOrNeverSentIsNotAskedAbout() {
    var invoice =
        new Invoice(church, Plan.CHURCH, false, BillingCountry.PE, new BigDecimal("75"), 20, 1);
    when(invoices.findForUpdate(invoice.getId())).thenReturn(Optional.of(invoice));

    assertThat(settler.settle(invoice)).isFalse();
    when(invoices.findForUpdate(invoice.getId())).thenReturn(Optional.empty());
    assertThat(settler.settle(invoice)).isFalse();

    org.mockito.Mockito.verifyNoInteractions(gateway);
  }
}
