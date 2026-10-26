package org.ouracademy.ovejafiel.notification.gateway;

/** Delivers a plain-text email through some provider. */
public interface EmailGateway {

  /**
   * @throws RuntimeException when the provider does not accept the message
   */
  void send(String to, String subject, String text);
}
