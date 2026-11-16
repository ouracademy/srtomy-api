package org.ouracademy.ovejafiel.billing.gateway;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.ouracademy.ovejafiel.billing.domain.InvoiceStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

/**
 * dLocal Go (https://docs.dlocalgo.com): one account charges in soles and in Colombian pesos, with
 * card and the local methods of each country (Yape, PSE, Nequi...). We only use one-time payments:
 * {@code POST /v1/payments} gives a checkout link, and {@code GET /v1/payments/{id}} says how it
 * went. Its notifications carry only the id of the payment, signed with HMAC-SHA256 over the API
 * key followed by the body.
 */
class DLocalGoPaymentGateway implements PaymentGateway {

  private static final String SIGNATURE_PREFIX = "V2-HMAC-SHA256, Signature: ";

  private final String apiKey;
  private final String secretKey;
  private final String notificationUrl;
  private final RestClient http;

  DLocalGoPaymentGateway(String apiKey, String secretKey, String url, String notificationUrl) {
    this.apiKey = apiKey;
    this.secretKey = secretKey;
    this.notificationUrl = notificationUrl;
    this.http =
        RestClient.builder()
            .baseUrl(url)
            .defaultHeader("Authorization", "Bearer " + apiKey + ":" + secretKey)
            .build();
  }

  @Override
  public CreatedPayment createPayment(PaymentRequest request) {
    var body = new LinkedHashMap<String, Object>();
    body.put("amount", request.amount());
    body.put("currency", request.currency());
    body.put("country", request.country());
    body.put("order_id", request.orderId());
    body.put("description", request.description());
    body.put("success_url", request.successUrl());
    body.put("back_url", request.backUrl());
    if (!notificationUrl.isBlank()) {
      body.put("notification_url", notificationUrl);
    }
    var response =
        http.post()
            .uri("/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .body(Map.class);
    if (response == null
        || !(response.get("id") instanceof String id)
        || !(response.get("redirect_url") instanceof String checkoutUrl)) {
      throw new IllegalStateException("dLocal Go answered without the payment id or its link");
    }
    return new CreatedPayment(id, checkoutUrl);
  }

  @Override
  public PaymentState findPayment(String paymentId) {
    var response = http.get().uri("/v1/payments/{id}", paymentId).retrieve().body(Map.class);
    if (response == null) {
      throw new IllegalStateException("dLocal Go answered nothing for the payment");
    }
    return new PaymentState(
        paymentId,
        response.get("order_id") instanceof String orderId ? orderId : null,
        status(response.get("status")));
  }

  /** PENDING, PAID, REJECTED, CANCELLED or EXPIRED; anything else is still pending for us. */
  static InvoiceStatus status(Object value) {
    if (value instanceof String name) {
      for (var status : InvoiceStatus.values()) {
        if (status.name().equals(name)) {
          return status;
        }
      }
    }
    return InvoiceStatus.PENDING;
  }

  @Override
  public boolean isAuthentic(byte[] rawBody, String authorization) {
    if (authorization == null || !authorization.startsWith(SIGNATURE_PREFIX)) {
      return false;
    }
    var received = authorization.substring(SIGNATURE_PREFIX.length()).trim();
    var expected = signature(rawBody);
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), received.getBytes(StandardCharsets.UTF_8));
  }

  String signature(byte[] rawBody) {
    try {
      var mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      mac.update(apiKey.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(mac.doFinal(rawBody));
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new IllegalStateException(e);
    }
  }
}
