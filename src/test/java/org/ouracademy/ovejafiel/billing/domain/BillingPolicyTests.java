package org.ouracademy.ovejafiel.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

class BillingPolicyTests {

  static final BillingPolicy POLICY =
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

  @Test
  void theBasePriceCoversThePeopleItIncludes() {
    assertThat(POLICY.monthlyPrice(BillingCountry.PE, 0)).isEqualByComparingTo("75");
    assertThat(POLICY.monthlyPrice(BillingCountry.PE, 150)).isEqualByComparingTo("75");
  }

  @Test
  void everyStartedBlockOfPeopleAboveThatAddsItsPrice() {
    assertThat(POLICY.extraBlocks(151)).isEqualTo(1);
    assertThat(POLICY.extraBlocks(250)).isEqualTo(1);
    assertThat(POLICY.extraBlocks(251)).isEqualTo(2);
    assertThat(POLICY.monthlyPrice(BillingCountry.PE, 151)).isEqualByComparingTo("110");
    assertThat(POLICY.monthlyPrice(BillingCountry.PE, 600)).isEqualByComparingTo("250");
  }

  @Test
  void aYearCostsTenMonths() {
    var quote = POLICY.quote(BillingCountry.PE, 100, 12);

    assertThat(quote.total()).isEqualByComparingTo("750");
    assertThat(quote.monthly()).isEqualByComparingTo("75");
    assertThat(quote.currency()).isEqualTo("PEN");
    assertThat(POLICY.quote(BillingCountry.PE, 100, 1).total()).isEqualByComparingTo("75");
  }

  @Test
  void onlyAMonthOrAYearCanBePaid() {
    assertThatThrownBy(() -> POLICY.quote(BillingCountry.PE, 10, 3))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> POLICY.quote(BillingCountry.PE, 10, 0))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void aCountryWithoutPricesCannotBeQuoted() {
    assertThatThrownBy(() -> POLICY.quote(BillingCountry.CO, 10, 1))
        .isInstanceOf(DomainValidationException.class);
  }
}
