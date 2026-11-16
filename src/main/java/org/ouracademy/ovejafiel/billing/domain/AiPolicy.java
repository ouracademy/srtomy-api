package org.ouracademy.ovejafiel.billing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * How much the AI assistant may answer for a church in a month. Two caps, both read from {@code
 * billing.ai.*}: a number of messages (the free plan has a few, the paid one more for every extra
 * block of people) and an estimated cost in dollars (a fixed amount for the free plan, a share of
 * what the church pays for the paid one), so a few very long conversations cannot cost more than
 * the church pays.
 *
 * @param usdPerUnit dollars one unit of each currency is worth, to compare the price with the cost
 * @param prices dollars per million tokens of each model; {@code defaultModel} prices any other
 */
public record AiPolicy(
    int freeMessages,
    int paidMessages,
    int messagesPerExtraBlock,
    BigDecimal freeMaxCostUsd,
    BigDecimal paidMaxCostShare,
    Map<BillingCountry, BigDecimal> usdPerUnit,
    Map<String, ModelPrice> prices,
    String defaultModel) {

  private static final BigDecimal MILLION = BigDecimal.valueOf(1_000_000);

  /** Dollars per million tokens. */
  public record ModelPrice(
      BigDecimal input, BigDecimal output, BigDecimal cacheRead, BigDecimal cacheWrite) {}

  /** Tokens of one answer, as the AI SDK reports them. */
  public record Tokens(long input, long output, long cacheRead, long cacheWrite) {}

  public int messageLimit(boolean paid, int extraBlocks) {
    return paid ? paidMessages + messagesPerExtraBlock * extraBlocks : freeMessages;
  }

  /**
   * @param monthlyPrice what the church pays a month, in the currency of {@code country}
   */
  public BigDecimal maxCostUsd(boolean paid, BillingCountry country, BigDecimal monthlyPrice) {
    if (!paid) {
      return freeMaxCostUsd;
    }
    var rate = usdPerUnit.getOrDefault(country, BigDecimal.ZERO);
    return monthlyPrice.multiply(rate).multiply(paidMaxCostShare).setScale(2, RoundingMode.HALF_UP);
  }

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
