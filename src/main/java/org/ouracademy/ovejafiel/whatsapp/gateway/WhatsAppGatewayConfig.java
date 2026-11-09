package org.ouracademy.ovejafiel.whatsapp.gateway;

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

  @Bean
  WhatsAppGateway whatsAppGateway(
      @Value("${kapso.api-key:}") String apiKey,
      @Value("${kapso.url:https://api.kapso.ai}") String url,
      @Value("${kapso.success-redirect-url:}") String successRedirectUrl,
      @Value("${kapso.failure-redirect-url:}") String failureRedirectUrl) {
    if (apiKey.isBlank()) {
      return new LoggingWhatsAppGateway();
    }
    return new KapsoWhatsAppGateway(apiKey, url, successRedirectUrl, failureRedirectUrl);
  }
}
