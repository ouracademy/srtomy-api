package org.ouracademy.srtomy.notification.gateway;

/** Delivers a plain-text email through some provider. */
public interface EmailGateway {

  /**
   * @param idempotencyKey the same key sent twice delivers once, so a retry after a timeout cannot
   *     duplicate the email
   * @throws RuntimeException when the provider does not accept the message
   */
  void send(String idempotencyKey, String to, String subject, String text);
}
