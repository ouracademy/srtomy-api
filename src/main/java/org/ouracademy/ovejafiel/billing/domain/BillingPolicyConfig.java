package org.ouracademy.ovejafiel.billing.domain;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reads the plans from the configuration ({@code billing.*}), so a price changes without touching
 * the code. The defaults are the plans published on the landing page.
 */
@Configuration
class BillingPolicyConfig {

  @Bean
  BillingPolicy billingPolicy(
      @Value("${billing.free.max-people:50}") int freeMaxPeople,
      @Value("${billing.free.max-locations:1}") int freeMaxLocations,
      @Value("${billing.free.whatsapp-trial-days:15}") int whatsappTrialDays,
      @Value("${billing.paid.included-people:150}") int includedPeople,
      @Value("${billing.paid.block-size:100}") int blockSize,
      @Value("${billing.paid.yearly-months-charged:10}") int yearlyMonthsCharged,
      @Value("${billing.paid.grace-days:7}") int graceDays,
      @Value("${billing.paid.prices.pe.base:75}") BigDecimal peBase,
      @Value("${billing.paid.prices.pe.extra-block:35}") BigDecimal peExtra,
      @Value("${billing.paid.prices.co.base:85000}") BigDecimal coBase,
      @Value("${billing.paid.prices.co.extra-block:40000}") BigDecimal coExtra) {
    return new BillingPolicy(
        freeMaxPeople,
        freeMaxLocations,
        whatsappTrialDays,
        includedPeople,
        blockSize,
        yearlyMonthsCharged,
        graceDays,
        Map.of(
            BillingCountry.PE, new BillingPolicy.Price(peBase, peExtra),
            BillingCountry.CO, new BillingPolicy.Price(coBase, coExtra)));
  }
}
