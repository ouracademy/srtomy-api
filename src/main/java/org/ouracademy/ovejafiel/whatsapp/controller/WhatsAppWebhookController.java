package org.ouracademy.ovejafiel.whatsapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import org.ouracademy.ovejafiel.whatsapp.command.confirm_connection.ConfirmConnectionCommand;
import org.ouracademy.ovejafiel.whatsapp.command.confirm_connection.ConfirmConnectionCommandHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called by Kapso, never by the web app, so it carries no Auth0 token: the signature of the body is
 * the credential. Anything with a wrong signature is refused before it is read.
 */
@RestController
@Hidden
public class WhatsAppWebhookController {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String PHONE_NUMBER_CREATED = "whatsapp.phone_number.created";

  private final ConfirmConnectionCommandHandler confirmConnectionCommandHandler;
  private final String secret;

  WhatsAppWebhookController(
      ConfirmConnectionCommandHandler confirmConnectionCommandHandler,
      @Value("${kapso.webhook-secret:}") String secret) {
    this.confirmConnectionCommandHandler = confirmConnectionCommandHandler;
    this.secret = secret;
  }

  @PostMapping("webhooks/whatsapp")
  public ResponseEntity<Void> receive(
      @RequestBody byte[] body,
      @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
      @RequestHeader(value = "X-Webhook-Event", required = false) String event,
      @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
    if (!WebhookSignature.isValid(body, signature, secret)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    if (PHONE_NUMBER_CREATED.equals(event)) {
      JsonNode payload;
      try {
        payload = JSON.readTree(body);
      } catch (java.io.IOException e) {
        return ResponseEntity.badRequest().build();
      }
      var customerId = payload.path("customer").path("id").asText("");
      var phoneNumberId = payload.path("phone_number_id").asText("");
      if (customerId.isBlank() || phoneNumberId.isBlank()) {
        return ResponseEntity.badRequest().build();
      }
      confirmConnectionCommandHandler.handle(
          new ConfirmConnectionCommand(idempotencyKey, customerId, phoneNumberId));
    }
    // Other events are not used yet; answering 200 keeps the provider from retrying them.
    return ResponseEntity.ok().build();
  }
}
