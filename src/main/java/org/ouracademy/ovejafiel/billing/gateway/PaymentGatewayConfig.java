package org.ouracademy.ovejafiel.billing.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The one {@link PaymentGateway} of the app: dLocal Go when its keys are configured, the sandbox
 * otherwise (local runs and tests).
 */
@Configuration
class PaymentGatewayConfig {

  @Bean
  PaymentGateway paymentGateway(
      @Value("${dlocalgo.api-key:}") String apiKey,
      @Value("${dlocalgo.secret-key:}") String secretKey,
      @Value("${dlocalgo.url:https://api.dlocalgo.com}") String url,
      @Value("${dlocalgo.notification-url:}") String notificationUrl) {
    if (apiKey.isBlank() || secretKey.isBlank()) {
      return new SandboxPaymentGateway();
    }
    return new DLocalGoPaymentGateway(apiKey, secretKey, url, notificationUrl);
  }
}
