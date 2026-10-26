package org.ouracademy.srtomy.notification;

import org.ouracademy.srtomy.notification.gateway.EmailGateway;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Replaces the email provider with {@link SentEmails}, so tests see what would be delivered. */
@TestConfiguration
public class TestEmailConfig {

  @Bean
  public SentEmails sentEmails() {
    return new SentEmails();
  }

  @Bean
  @Primary
  public EmailGateway recordingEmailGateway(SentEmails sentEmails) {
    return (idempotencyKey, to, subject, text) ->
        sentEmails.deliver(new SentEmails.Email(idempotencyKey, to, subject, text));
  }
}
