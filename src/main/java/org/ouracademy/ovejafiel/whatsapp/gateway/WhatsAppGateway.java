package org.ouracademy.ovejafiel.whatsapp.gateway;

import java.time.Instant;
import java.util.List;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;

/** What the app needs from the WhatsApp provider (Kapso today). */
public interface WhatsAppGateway {

  /** The link the pastor opens to connect the number. */
  record SetupLink(String url, Instant expiresAt) {}

  /**
   * @param externalId our id for the church, so the provider's customer can be found from ours
   * @return the id the provider gave the customer
   */
  String createCustomer(String externalId, String name);

  SetupLink createSetupLink(String customerId, ConnectionMode mode, String language);

  /**
   * Sends an approved template.
   *
   * @return the id the provider gave the message, or null when it gives none
   * @throws org.ouracademy.ovejafiel.outbox.PermanentDeliveryException when the provider refuses
   *     the message for good (trying again cannot work)
   * @throws RuntimeException for any other failure
   */
  String sendTemplate(
      String phoneNumberId, String to, String template, String language, List<String> variables);
}
