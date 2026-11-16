package org.ouracademy.ovejafiel.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AiPolicyTests {

  static final AiPolicy POLICY =
      new AiPolicy(
          30,
          300,
          100,
          BigDecimal.ONE,
          new BigDecimal("0.40"),
          Map.of(
              BillingCountry.PE,
              new BigDecimal("0.27"),
              BillingCountry.CO,
              new BigDecimal("0.00025")),
          AiPolicyConfig.DEFAULT_PRICES,
          "claude-sonnet-5-5");

  @Test
  void thePaidPlanAddsMessagesForEveryExtraBlockOfPeople() {
    assertThat(POLICY.messageLimit(false, 3)).isEqualTo(30);
    assertThat(POLICY.messageLimit(true, 0)).isEqualTo(300);
    assertThat(POLICY.messageLimit(true, 2)).isEqualTo(500);
  }

  @Test
  void theSpendingCapIsAShareOfWhatTheChurchPaysInDollars() {
    assertThat(POLICY.maxCostUsd(false, BillingCountry.PE, BigDecimal.ZERO))
        .isEqualByComparingTo("1");
    // S/ 75 are about US$ 20.25; 40 % of it.
    assertThat(POLICY.maxCostUsd(true, BillingCountry.PE, BigDecimal.valueOf(75)))
        .isEqualByComparingTo("8.10");
    // COP 85.000 are about US$ 21.25.
    assertThat(POLICY.maxCostUsd(true, BillingCountry.CO, BigDecimal.valueOf(85000)))
        .isEqualByComparingTo("8.50");
  }

  @Test
  void tokensArePricedPerModelAndAnUnknownModelCostsAsTheDefault() {
    var tokens = new AiPolicy.Tokens(2_000, 800, 20_000, 0);
    // 2,000 × 2 + 800 × 10 + 20,000 × 0.10 dollars per million.
    assertThat(POLICY.cost("claude-sonnet-5-5", tokens)).isEqualByComparingTo("0.014");
    assertThat(POLICY.cost("claude-haiku-5-5", tokens)).isEqualByComparingTo("0.0008");
    assertThat(POLICY.cost("some-new-model", tokens)).isEqualByComparingTo("0.014");
    assertThat(POLICY.knows("some-new-model")).isFalse();
  }
}
