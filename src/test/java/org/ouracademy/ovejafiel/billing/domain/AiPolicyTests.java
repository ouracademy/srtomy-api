package org.ouracademy.ovejafiel.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiPolicyTests {

  static final AiPolicy POLICY = new AiPolicy(AiPolicyConfig.DEFAULT_PRICES, "claude-sonnet-5-5");

  @Test
  void tokensArePricedPerModelAndAnUnknownModelCostsAsTheDefault() {
    var tokens = new AiPolicy.Tokens(2_000, 800, 20_000, 0);
    // 2,000 × 2 + 800 × 10 + 20,000 × 0.10 dollars per million.
    assertThat(POLICY.cost("claude-sonnet-5-5", tokens)).isEqualByComparingTo("0.014");
    // 2,000 × 0.10 + 800 × 0.50 + 20,000 × 0.01 dollars per million.
    assertThat(POLICY.cost("claude-haiku-5-5", tokens)).isEqualByComparingTo("0.0008");
    assertThat(POLICY.cost("some-new-model", tokens)).isEqualByComparingTo("0.014");
    assertThat(POLICY.knows("some-new-model")).isFalse();
    assertThat(POLICY.knows("claude-haiku-5-5")).isTrue();
  }

  @Test
  void haikuCostsAboutAFifteenthOfSonnetForTheSameAnswer() {
    var tokens = new AiPolicy.Tokens(2_000, 800, 20_000, 0);

    var haiku = POLICY.cost("claude-haiku-5-5", tokens);
    var sonnet = POLICY.cost("claude-sonnet-5-5", tokens);

    assertThat(sonnet.divide(haiku, 0, java.math.RoundingMode.HALF_UP).intValue())
        .isBetween(15, 20);
  }
}
