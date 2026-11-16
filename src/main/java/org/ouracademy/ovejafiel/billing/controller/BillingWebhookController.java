package org.ouracademy.ovejafiel.billing.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ouracademy.ovejafiel.billing.command.sync_payment.SyncPaymentCommand;
import org.ouracademy.ovejafiel.billing.command.sync_payment.SyncPaymentCommandHandler;
import org.ouracademy.ovejafiel.billing.gateway.PaymentGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Called by the payment gateway, never by the web app, so it carries no Auth0 token: the signature
 * of the body is the credential, and anything with a wrong one is refused before it is read. The
 * notification only names a payment; how it went is asked to the gateway, so a forged or repeated
 * notification cannot give a church paid time.
 */
@Slf4j
@RestController
@Hidden
@RequiredArgsConstructor
public class BillingWebhookController {

  private static final ObjectMapper JSON = new ObjectMapper();

  private final PaymentGateway gateway;
  private final SyncPaymentCommandHandler syncPaymentCommandHandler;

  @PostMapping("webhooks/billing")
  public ResponseEntity<Void> receive(
      @RequestBody byte[] body,
      @RequestHeader(value = "Authorization", required = false) String authorization) {
    if (!gateway.isAuthentic(body, authorization)) {
      log.warn("Billing webhook refused: wrong or missing signature");
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    JsonNode payload;
    try {
      payload = JSON.readTree(body);
    } catch (java.io.IOException _) {
      log.warn("Billing webhook refused: the body is not JSON");
      return ResponseEntity.badRequest().build();
    }
    var paymentId = payload.path("payment_id").asText("");
    if (paymentId.isBlank()) {
      log.warn("Billing webhook refused: no payment id");
      return ResponseEntity.badRequest().build();
    }
    var result = syncPaymentCommandHandler.handle(new SyncPaymentCommand(paymentId));
    log.info(
        "Billing webhook: payment={}, known={}, paid={}", paymentId, result.known(), result.paid());
    // 200 also for a payment that is not ours: retrying it would never change that.
    return ResponseEntity.ok().build();
  }
}
