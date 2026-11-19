package org.ouracademy.ovejafiel.billing.domain;

import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Builds the {@link BillingPolicy} from {@link BillingProperties}. */
@Configuration
@EnableConfigurationProperties(BillingProperties.class)
public class BillingPolicyConfig {

  @Bean
  public BillingPolicy billingPolicy(BillingProperties properties) {
    var plans =
        Map.of(
            Plan.FREE, properties.getFree(),
            Plan.BASIC, properties.getBasic(),
            Plan.CHURCH, properties.getChurch(),
            Plan.NETWORK, properties.getNetwork());
    var limits = new EnumMap<Plan, BillingPolicy.Limits>(Plan.class);
    var prices = new EnumMap<Plan, Map<BillingCountry, java.math.BigDecimal>>(Plan.class);
    plans.forEach(
        (plan, p) -> {
          limits.put(plan, p.toLimits());
          if (plan.isPaid()) {
            prices.put(
                plan, Map.of(BillingCountry.PE, p.getPricePe(), BillingCountry.CO, p.getPriceCo()));
          }
        });
    var own = properties.getOwnNumber();
    return new BillingPolicy(
        Map.copyOf(limits),
        Map.copyOf(prices),
        Map.of(BillingCountry.PE, own.getPricePe(), BillingCountry.CO, own.getPriceCo()),
        properties.getYearlyMonthsCharged(),
        properties.getGraceDays());
  }
}
