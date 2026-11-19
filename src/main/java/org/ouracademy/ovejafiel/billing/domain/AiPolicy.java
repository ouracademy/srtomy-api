package org.ouracademy.ovejafiel.billing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * What the tokens of the AI assistant cost, to estimate what a church spends: dollars per million
 * tokens of each model, from {@code billing.ai.prices}. How much a plan may spend is in {@link
 * BillingPolicy.Limits}.
 *
 * @param prices dollars per million tokens of each model
 * @param defaultModel the model that prices any other one
 */
public record AiPolicy(Map<String, ModelPrice> prices, String defaultModel) {

  private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

  /** Dollars per million tokens. */
  public record ModelPrice(
      BigDecimal input, BigDecimal output, BigDecimal cacheRead, BigDecimal cacheWrite) {}

  /** Tokens of one answer, as the AI SDK reports them. */
  public record Tokens(long input, long output, long cacheRead, long cacheWrite) {}

  /**
   * The estimated cost of {@code tokens} with {@code model}; an unknown model costs as the default.
   */
  public BigDecimal cost(String model, Tokens tokens) {
    var price = model == null ? null : prices.get(model);
    if (price == null) {
      price = prices.get(defaultModel);
    }
    if (price == null) {
      return BigDecimal.ZERO;
    }
    return price
        .input()
        .multiply(BigDecimal.valueOf(tokens.input()))
        .add(price.output().multiply(BigDecimal.valueOf(tokens.output())))
        .add(price.cacheRead().multiply(BigDecimal.valueOf(tokens.cacheRead())))
        .add(price.cacheWrite().multiply(BigDecimal.valueOf(tokens.cacheWrite())))
        .divide(MILLION, 6, RoundingMode.HALF_UP);
  }

  public boolean knows(String model) {
    return model != null && prices.containsKey(model);
  }
}
