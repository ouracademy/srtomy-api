package org.ouracademy.ovejafiel.notification.gateway;

/** Delivers a plain-text email through some provider. */
public interface EmailGateway {

  /**
   * @param idempotencyKey the same key sent twice delivers once, so a retry after a timeout cannot
   *     duplicate the email
   * @return the id the provider gave the email, or null when it gives none
   * @throws org.ouracademy.ovejafiel.outbox.PermanentDeliveryException when the provider refuses
   *     the email for good (trying again cannot work)
   * @throws RuntimeException for any other failure
   */
  String send(String idempotencyKey, String to, String subject, String text);
}
