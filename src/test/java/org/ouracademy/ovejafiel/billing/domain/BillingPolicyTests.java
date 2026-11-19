package org.ouracademy.ovejafiel.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

/** The four plans with the numbers of the specification (taxes included). */
public class BillingPolicyTests {

  public static final BillingPolicy POLICY =
      new BillingPolicyConfig().billingPolicy(new BillingProperties());

  @Test
  void everyPlanHasItsOwnMaximumsAndQuotas() {
    var free = POLICY.limits(Plan.FREE);
    var basic = POLICY.limits(Plan.BASIC);
    var church = POLICY.limits(Plan.CHURCH);
    var network = POLICY.limits(Plan.NETWORK);

    assertThat(new Object[] {free.maxPeople(), basic.maxPeople(), church.maxPeople()})
        .containsExactly(50, 150, 400);
    assertThat(network.maxPeople()).isNull();
    assertThat(new Object[] {free.maxLocations(), basic.maxLocations(), church.maxLocations()})
        .containsExactly(1, 1, 5);
    assertThat(network.maxLocations()).isNull();
    assertThat(new int[] {free.aiMessages(), basic.aiMessages(), church.aiMessages()})
        .containsExactly(20, 150, 400);
    assertThat(network.aiMessages()).isEqualTo(1200);
    assertThat(new int[] {free.whatsappReminders(), basic.whatsappReminders()})
        .containsExactly(0, 50);
    assertThat(new int[] {church.whatsappReminders(), network.whatsappReminders()})
        .containsExactly(200, 600);
    assertThat(free.aiMaxCostUsd()).isEqualByComparingTo("0.50");
    assertThat(basic.aiMaxCostUsd()).isEqualByComparingTo("2");
    assertThat(church.aiMaxCostUsd()).isEqualByComparingTo("5");
    assertThat(network.aiMaxCostUsd()).isEqualByComparingTo("15");
  }

  @Test
  void theMaximumOfPeopleAndLocationsIsInclusive() {
    var basic = POLICY.limits(Plan.BASIC);

    assertThat(basic.allowsPeople(150)).isTrue();
    assertThat(basic.allowsPeople(151)).isFalse();
    assertThat(basic.allowsLocations(1)).isTrue();
    assertThat(basic.allowsLocations(2)).isFalse();
    assertThat(POLICY.limits(Plan.NETWORK).allowsPeople(1_000_000)).isTrue();
  }

  @Test
  void eachPlanIncludesWhatTheSpecificationSays() {
    assertThat(Feature.values())
        .allSatisfy(f -> assertThat(POLICY.limits(Plan.FREE).includes(f)).isFalse());
    var basic = POLICY.limits(Plan.BASIC);
    assertThat(basic.includes(Feature.CUSTOM_ROLES)).isTrue();
    assertThat(basic.includes(Feature.EXCEL_EXPORT)).isTrue();
    assertThat(basic.includes(Feature.FINANCES)).isFalse();
    assertThat(basic.includes(Feature.MASS_EMAIL)).isFalse();
    assertThat(basic.includes(Feature.VOLUNTEERS)).isFalse();
    assertThat(basic.includes(Feature.OWN_NUMBER)).isFalse();
    var church = POLICY.limits(Plan.CHURCH);
    assertThat(church.includes(Feature.FINANCES)).isTrue();
    assertThat(church.includes(Feature.MASS_EMAIL)).isTrue();
    assertThat(church.includes(Feature.VOLUNTEERS)).isTrue();
    assertThat(church.includes(Feature.OWN_NUMBER)).isFalse();
    assertThat(Feature.values())
        .allSatisfy(f -> assertThat(POLICY.limits(Plan.NETWORK).includes(f)).isTrue());
  }

  @Test
  void theOwnNumberIsAnExtraOfBasicAndChurchAndIncludedInNetwork() {
    assertThat(POLICY.limits(Plan.FREE).ownNumberExtra()).isFalse();
    assertThat(POLICY.limits(Plan.BASIC).ownNumberExtra()).isTrue();
    assertThat(POLICY.limits(Plan.CHURCH).ownNumberExtra()).isTrue();
    assertThat(POLICY.limits(Plan.NETWORK).ownNumberExtra()).isFalse();
    assertThat(POLICY.limits(Plan.NETWORK).ownNumberIncluded()).isTrue();
  }

  @Test
  void aMonthCostsWhatThePlanCostsInTheCountry() {
    assertThat(POLICY.quote(Plan.BASIC, BillingCountry.PE, 1, false).total())
        .isEqualByComparingTo("45");
    assertThat(POLICY.quote(Plan.CHURCH, BillingCountry.PE, 1, false).total())
        .isEqualByComparingTo("75");
    assertThat(POLICY.quote(Plan.NETWORK, BillingCountry.PE, 1, false).total())
        .isEqualByComparingTo("149");
    assertThat(POLICY.quote(Plan.BASIC, BillingCountry.CO, 1, false).total())
        .isEqualByComparingTo("49000");
    assertThat(POLICY.quote(Plan.CHURCH, BillingCountry.CO, 1, false).total())
        .isEqualByComparingTo("79000");
    assertThat(POLICY.quote(Plan.NETWORK, BillingCountry.CO, 1, false).total())
        .isEqualByComparingTo("159000");
  }

  @Test
  void aYearCostsTenMonths() {
    var quote = POLICY.quote(Plan.CHURCH, BillingCountry.PE, 12, false);

    assertThat(quote.monthly()).isEqualByComparingTo("75");
    assertThat(quote.total()).isEqualByComparingTo("750");
    assertThat(quote.currency()).isEqualTo("PEN");
    assertThat(quote.months()).isEqualTo(12);
    assertThat(POLICY.quote(Plan.NETWORK, BillingCountry.CO, 12, false).total())
        .isEqualByComparingTo("1590000");
  }

  @Test
  void theOwnNumberAddsItsOwnPriceToEveryMonth() {
    var month = POLICY.quote(Plan.BASIC, BillingCountry.PE, 1, true);
    var year = POLICY.quote(Plan.CHURCH, BillingCountry.CO, 12, true);

    assertThat(month.total()).isEqualByComparingTo("94");
    assertThat(month.ownNumber()).isTrue();
    assertThat(year.monthly()).isEqualByComparingTo(new BigDecimal("128000"));
    assertThat(year.total()).isEqualByComparingTo("1280000");
  }

  @Test
  void theFreePlanIsNotPaid() {
    assertThatThrownBy(() -> POLICY.quote(Plan.FREE, BillingCountry.PE, 1, false))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("Gratis");
  }

  @Test
  void onlyAMonthOrAYearCanBePaid() {
    for (var months : new int[] {0, 3, 6, 24}) {
      assertThatThrownBy(() -> POLICY.quote(Plan.CHURCH, BillingCountry.PE, months, false))
          .isInstanceOf(DomainValidationException.class)
          .hasMessageContaining("un mes o un año");
    }
  }

  @Test
  void theOwnNumberCannotBeBoughtWhereItIsIncluded() {
    assertThatThrownBy(() -> POLICY.quote(Plan.NETWORK, BillingCountry.PE, 1, true))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("ya incluye");
  }

  @Test
  void theNextPlanIsTheOneAboveAndNetworkHasNone() {
    assertThat(Plan.FREE.next()).contains(Plan.BASIC);
    assertThat(Plan.BASIC.next()).contains(Plan.CHURCH);
    assertThat(Plan.CHURCH.next()).contains(Plan.NETWORK);
    assertThat(Plan.NETWORK.next()).isEmpty();
  }

  @Test
  void aPriceOrAMaximumChangesFromTheConfigurationWithoutTouchingTheRest() {
    var properties = new BillingProperties();
    properties.getBasic().setMaxPeople(200);
    properties.getChurch().setPricePe(new BigDecimal("80"));

    var policy = new BillingPolicyConfig().billingPolicy(properties);

    assertThat(policy.limits(Plan.BASIC).maxPeople()).isEqualTo(200);
    assertThat(policy.limits(Plan.BASIC).aiMessages()).isEqualTo(150);
    assertThat(policy.monthlyPrice(Plan.CHURCH, BillingCountry.PE)).isEqualByComparingTo("80");
    assertThat(policy.monthlyPrice(Plan.CHURCH, BillingCountry.CO)).isEqualByComparingTo("79000");
  }
}
