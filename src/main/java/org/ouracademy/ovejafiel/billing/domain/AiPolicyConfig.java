package org.ouracademy.ovejafiel.billing.domain;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reads the AI caps from {@code billing.ai.*}. The defaults are the proposal of spec 018 (still to
 * be confirmed by the product owner after measuring the real cost per message). The model prices
 * are Anthropic's list prices of November 2026 (cache writes at 1.25 times the input); a model can
 * be added or changed with {@code billing.ai.prices.<model>.input} and so on.
 */
@Configuration
@EnableConfigurationProperties(AiPolicyConfig.ModelPrices.class)
class AiPolicyConfig {

  /** {@code billing.ai.prices}: dollars per million tokens of each model. */
  @ConfigurationProperties("billing.ai")
  record ModelPrices(Map<String, AiPolicy.ModelPrice> prices) {}

  static final Map<String, AiPolicy.ModelPrice> DEFAULT_PRICES =
      Map.of(
          "claude-sonnet-5-5", price("2.00", "10.00", "0.10", "2.50"),
          "claude-haiku-5-5", price("0.10", "0.50", "0.01", "0.125"),
          "claude-opus-5-5", price("4.00", "20.00", "0.20", "5.00"));

  private static AiPolicy.ModelPrice price(
      String input, String output, String cacheRead, String cacheWrite) {
    return new AiPolicy.ModelPrice(
        new BigDecimal(input),
        new BigDecimal(output),
        new BigDecimal(cacheRead),
        new BigDecimal(cacheWrite));
  }

  @Bean
  AiPolicy aiPolicy(
      ModelPrices configured,
      @Value("${billing.ai.free.messages:30}") int freeMessages,
      @Value("${billing.ai.paid.messages:300}") int paidMessages,
      @Value("${billing.ai.paid.messages-per-extra-block:100}") int perBlock,
      @Value("${billing.ai.free.max-cost-usd:1}") BigDecimal freeMaxCost,
      @Value("${billing.ai.paid.max-cost-share:0.40}") BigDecimal paidShare,
      @Value("${billing.ai.usd-per-unit.pe:0.27}") BigDecimal usdPerSol,
      @Value("${billing.ai.usd-per-unit.co:0.00025}") BigDecimal usdPerPeso,
      @Value("${billing.ai.default-model:claude-sonnet-5-5}") String defaultModel) {
    var prices = new HashMap<>(DEFAULT_PRICES);
    if (configured.prices() != null) {
      prices.putAll(configured.prices());
    }
    return new AiPolicy(
        freeMessages,
        paidMessages,
        perBlock,
        freeMaxCost,
        paidShare,
        Map.of(BillingCountry.PE, usdPerSol, BillingCountry.CO, usdPerPeso),
        Map.copyOf(prices),
        defaultModel);
  }
}
