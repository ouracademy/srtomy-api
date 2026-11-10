package org.ouracademy.ovejafiel.whatsapp.gateway;

import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The one {@link WhatsAppGateway} of the app: Kapso when its key is configured, a logging stand-in
 * otherwise (local runs and tests). It is chosen here, in one bean, so that nothing injecting the
 * gateway has two candidates.
 */
@Configuration
class WhatsAppGatewayConfig {

  private static final Set<String> BILLING_MODES = Set.of("customer_managed", "partner_managed");

  @Bean
  WhatsAppGateway whatsAppGateway(
      @Value("${kapso.api-key:}") String apiKey,
      @Value("${kapso.url:https://api.kapso.ai}") String url,
      @Value("${kapso.billing-mode:customer_managed}") String billingMode) {
    if (apiKey.isBlank()) {
      return new LoggingWhatsAppGateway();
    }
    if (!BILLING_MODES.contains(billingMode)) {
      // A typo here would reach Kapso as a refused link, one church at a time: fail at startup.
      throw new IllegalArgumentException(
          "kapso.billing-mode must be one of " + BILLING_MODES + " but is " + billingMode);
    }
    return new KapsoWhatsAppGateway(apiKey, url, billingMode);
  }
}
