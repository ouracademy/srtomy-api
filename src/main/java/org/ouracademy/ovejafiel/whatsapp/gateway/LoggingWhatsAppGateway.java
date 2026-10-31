package org.ouracademy.ovejafiel.whatsapp.gateway;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/** Used when no provider key is configured (local runs and tests): nothing leaves the log. */
@Slf4j
@Component
@ConditionalOnExpression("'${kapso.api-key:}'.isEmpty()")
class LoggingWhatsAppGateway implements WhatsAppGateway {

  @Override
  public String createCustomer(String externalId, String name) {
    log.info("WhatsApp customer {} ({}) not created, no provider configured", name, externalId);
    return "local-" + externalId;
  }

  @Override
  public String displayNumber(String phoneNumberId) {
    return null;
  }

  @Override
  public SetupLink createSetupLink(String customerId, ConnectionMode mode, String language) {
    log.info("WhatsApp setup link for {} ({}) not made, no provider configured", customerId, mode);
    return new SetupLink(
        "https://setup.invalid/" + UUID.randomUUID(), Instant.now().plus(30, ChronoUnit.DAYS));
  }

  @Override
  public String sendTemplate(
      String phoneNumberId, String to, String template, String language, List<String> variables) {
    log.info("WhatsApp {} to {} not sent, no provider configured: {}", template, to, variables);
    return null;
  }
}
