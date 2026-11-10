package org.ouracademy.ovejafiel.whatsapp.gateway;

import java.time.Instant;
import java.util.List;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;
import org.ouracademy.ovejafiel.whatsapp.domain.WhatsAppTemplate;

/** What the app needs from the WhatsApp provider (Kapso today). */
public interface WhatsAppGateway {

  /** The link the pastor opens to connect the number. */
  record SetupLink(String url, Instant expiresAt) {}

  /**
   * @param externalId our id for the church, so the provider's customer can be found from ours
   * @return the id the provider gave the customer
   */
  String createCustomer(String externalId, String name);

  /** What the provider knows of a connected number; fields it does not give are null. */
  record PhoneInfo(String displayNumber, String businessAccountId) {}

  PhoneInfo phoneInfo(String phoneNumberId);

  /**
   * Submits a template to Meta, through the provider, for the church's WhatsApp account. Meta
   * reviews it (up to a day); sending it before it is approved fails.
   *
   * @throws org.ouracademy.ovejafiel.outbox.PermanentDeliveryException when the provider refuses it
   *     (for instance because it already exists)
   */
  void createTemplate(String businessAccountId, WhatsAppTemplate template);

  /**
   * Makes sure the provider delivers the messages that arrive at the number to {@code url}, signed
   * with {@code secret}. Calling it again does nothing when the webhook already exists, so a
   * retried delivery cannot make two (the member would be answered twice).
   */
  void ensureMessageWebhook(String phoneNumberId, String url, String secret);

  /**
   * @param successUrl where the provider sends the pastor when the registration ends well (the
   *     portal, to see the result); null or blank for none
   * @param failureUrl the same when it fails
   */
  SetupLink createSetupLink(
      String customerId,
      ConnectionMode mode,
      String language,
      String successUrl,
      String failureUrl);

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
