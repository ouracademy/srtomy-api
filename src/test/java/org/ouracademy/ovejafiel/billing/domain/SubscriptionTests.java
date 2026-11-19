package org.ouracademy.ovejafiel.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class SubscriptionTests {

  static final LocalDate TODAY = LocalDate.of(2026, 11, 15);
  static final LocalDateTime NOW = TODAY.atTime(10, 0);

  Subscription subscription = new Subscription(PartyId.generate());

  @Test
  void aChurchThatNeverPaidIsFree() {
    assertThat(subscription.status(TODAY, 7)).isEqualTo(SubscriptionStatus.FREE);
    assertThat(subscription.nextPeriodStart(TODAY)).isEqualTo(TODAY);
  }

  @Test
  void itIsActiveUntilTheLastPaidDayThenInGraceThenExpired() {
    subscription.extendTo(TODAY, BillingCountry.PE, Plan.CHURCH, false);

    assertThat(subscription.status(TODAY, 7)).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(subscription.status(TODAY.plusDays(1), 7)).isEqualTo(SubscriptionStatus.GRACE);
    assertThat(subscription.status(TODAY.plusDays(7), 7)).isEqualTo(SubscriptionStatus.GRACE);
    assertThat(subscription.status(TODAY.plusDays(8), 7)).isEqualTo(SubscriptionStatus.EXPIRED);
    assertThat(SubscriptionStatus.GRACE.isPaid()).isTrue();
    assertThat(SubscriptionStatus.EXPIRED.isPaid()).isFalse();
  }

  @Test
  void renewingEarlyStartsWhereThePaidTimeEnds() {
    subscription.extendTo(TODAY.plusDays(10), BillingCountry.PE, Plan.CHURCH, false);

    assertThat(subscription.nextPeriodStart(TODAY)).isEqualTo(TODAY.plusDays(11));
  }

  @Test
  void renewingLateStartsToday() {
    subscription.extendTo(TODAY.minusDays(30), BillingCountry.PE, Plan.CHURCH, false);

    assertThat(subscription.nextPeriodStart(TODAY)).isEqualTo(TODAY);
  }

  @Test
  void aPaymentNeverShortensThePaidTime() {
    subscription.extendTo(TODAY.plusDays(40), BillingCountry.PE, Plan.CHURCH, false);
    subscription.extendTo(TODAY.plusDays(5), BillingCountry.CO, Plan.BASIC, true);

    assertThat(subscription.getPaidUntil()).isEqualTo(TODAY.plusDays(40));
    assertThat(subscription.getCountry()).isEqualTo(BillingCountry.CO);
    // The last payment decides the plan and the extras, with no proration.
    assertThat(subscription.getPlan()).isEqualTo(Plan.BASIC);
    assertThat(subscription.isOwnNumber()).isTrue();
  }

  @Test
  void aPaidInvoiceCoversWholeMonthsFromItsStart() {
    var invoice =
        new Invoice(
            PartyId.generate(), Plan.CHURCH, false, BillingCountry.PE, new BigDecimal("75"), 20, 1);

    var paid = invoice.settle(InvoiceStatus.PAID, NOW, TODAY);

    assertThat(paid).isTrue();
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    assertThat(invoice.getPeriodStart()).isEqualTo(TODAY);
    assertThat(invoice.getPeriodEnd()).isEqualTo(LocalDate.of(2026, 12, 14));
    assertThat(invoice.getPaidAt()).isEqualTo(NOW);
    assertThat(invoice.getCurrency()).isEqualTo("PEN");
  }

  @Test
  void aPaidInvoiceIsNotPaidTwiceAndAStillPendingOneDoesNotChange() {
    var invoice =
        new Invoice(
            PartyId.generate(),
            Plan.CHURCH,
            false,
            BillingCountry.PE,
            new BigDecimal("75"),
            20,
            12);
    invoice.sentToGateway("DP-1", "https://checkout/1");

    assertThat(invoice.settle(InvoiceStatus.PENDING, NOW, TODAY)).isFalse();
    assertThat(invoice.getCheckoutUrl()).isEqualTo("https://checkout/1");

    assertThat(invoice.settle(InvoiceStatus.PAID, NOW, TODAY)).isTrue();
    assertThat(invoice.settle(InvoiceStatus.PAID, NOW.plusDays(1), TODAY.plusDays(1))).isFalse();
    assertThat(invoice.settle(InvoiceStatus.REJECTED, NOW, TODAY)).isFalse();
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
    assertThat(invoice.getPeriodEnd()).isEqualTo(LocalDate.of(2027, 11, 14));
    assertThat(invoice.getCheckoutUrl()).isNull();
  }

  @Test
  void aRejectedPaymentClosesTheInvoiceWithoutAPeriod() {
    var invoice =
        new Invoice(
            PartyId.generate(),
            Plan.BASIC,
            true,
            BillingCountry.CO,
            new BigDecimal("98000"),
            20,
            1);

    assertThat(invoice.settle(InvoiceStatus.REJECTED, NOW, TODAY)).isFalse();
    assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.REJECTED);
    assertThat(invoice.getPeriodStart()).isNull();
  }
}
