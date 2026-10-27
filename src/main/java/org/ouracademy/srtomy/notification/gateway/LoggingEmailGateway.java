package org.ouracademy.srtomy.notification.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

/**
 * Used when no provider key is configured (local runs and tests): the email only reaches the log.
 */
@Slf4j
@Component
@ConditionalOnExpression("'${resend.api-key:}'.isEmpty()")
class LoggingEmailGateway implements EmailGateway {

  @Override
  public String send(String idempotencyKey, String to, String subject, String text) {
    log.info("Email to {} (not sent, no provider configured): {}\n{}", to, subject, text);
    return null;
  }
}
