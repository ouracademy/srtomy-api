package org.ouracademy.ovejafiel.whatsapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.ouracademy.ovejafiel.whatsapp.command.confirm_connection.ConfirmConnectionCommand;
import org.ouracademy.ovejafiel.whatsapp.command.confirm_connection.ConfirmConnectionCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.command.update_connection_state.UpdateConnectionStateCommand;
import org.ouracademy.ovejafiel.whatsapp.command.update_connection_state.UpdateConnectionStateCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.domain.DisconnectReason;
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
 *
 * <p>It is the project webhook of Kapso (Integrations > Webhooks > Platform webhooks, payload
 * version v2). It tells us when a church connects its number and when a number stops working or
 * works again. The messages do not come here: they go to the agent, through a webhook of each
 * number.
 */
@Slf4j
@RestController
@Hidden
public class WhatsAppWebhookController {

  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String PHONE_NUMBER_CREATED = "whatsapp.phone_number.created";

  /** What each event says about the number: why it was lost. */
  private static final Map<String, DisconnectReason> LOST =
      Map.of(
          "whatsapp.phone_number.deleted", DisconnectReason.NUMBER_DELETED,
          "whatsapp.phone_number.offboarded", DisconnectReason.NUMBER_OFFBOARDED,
          "whatsapp.phone_number.disconnected", DisconnectReason.ACCESS_REMOVED,
          "whatsapp.account.disabled", DisconnectReason.ACCOUNT_DISABLED);

  private static final List<String> RESTORED =
      List.of("whatsapp.phone_number.reconnected", "whatsapp.account.reinstated");

  /** Meta limits the account, but the number still works: worth a look, nothing to switch off. */
  private static final List<String> WARNINGS =
      List.of("whatsapp.account.restricted", "whatsapp.account.violation");

  private final ConfirmConnectionCommandHandler confirmConnectionCommandHandler;
  private final UpdateConnectionStateCommandHandler updateConnectionStateCommandHandler;
  private final String secret;

  WhatsAppWebhookController(
      ConfirmConnectionCommandHandler confirmConnectionCommandHandler,
      UpdateConnectionStateCommandHandler updateConnectionStateCommandHandler,
      @Value("${kapso.webhook-secret:}") String secret) {
    this.confirmConnectionCommandHandler = confirmConnectionCommandHandler;
    this.updateConnectionStateCommandHandler = updateConnectionStateCommandHandler;
    this.secret = secret;
  }

  @PostMapping("webhooks/whatsapp")
  public ResponseEntity<Void> receive(
      @RequestBody byte[] body,
      @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
      @RequestHeader(value = "X-Webhook-Event", required = false) String eventHeader,
      @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey) {
    if (!WebhookSignature.isValid(body, signature, secret)) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    JsonNode payload;
    try {
      payload = JSON.readTree(body);
    } catch (java.io.IOException e) {
      return ResponseEntity.badRequest().build();
    }
    var event = eventHeader != null ? eventHeader : payload.path("event").asText("");
    if (PHONE_NUMBER_CREATED.equals(event)) {
      var customerId = payload.path("customer").path("id").asText("");
      var phoneNumberId = payload.path("phone_number_id").asText("");
      if (customerId.isBlank() || phoneNumberId.isBlank()) {
        return ResponseEntity.badRequest().build();
      }
      confirmConnectionCommandHandler.handle(
          new ConfirmConnectionCommand(idempotencyKey, customerId, phoneNumberId));
    } else if (LOST.containsKey(event) || RESTORED.contains(event)) {
      var phoneNumberIds = phoneNumberIds(payload);
      if (phoneNumberIds.isEmpty()) {
        return ResponseEntity.badRequest().build();
      }
      updateConnectionStateCommandHandler.handle(
          new UpdateConnectionStateCommand(idempotencyKey, phoneNumberIds, LOST.get(event)));
    } else if (WARNINGS.contains(event)) {
      log.warn("Kapso reports {} for {}", event, phoneNumberIds(payload));
    }
    // Other events are not used yet; answering 200 keeps the provider from retrying them.
    return ResponseEntity.ok().build();
  }

  /**
   * One number on the events of a number, the numbers of the account on the events of the account.
   */
  private static List<String> phoneNumberIds(JsonNode payload) {
    var ids = new ArrayList<String>();
    var single = payload.path("phone_number_id").asText("");
    if (!single.isBlank()) {
      ids.add(single);
    }
    for (var number : payload.path("phone_numbers")) {
      var id = number.path("id").asText("");
      if (!id.isBlank()) {
        ids.add(id);
      }
    }
    return ids;
  }
}
