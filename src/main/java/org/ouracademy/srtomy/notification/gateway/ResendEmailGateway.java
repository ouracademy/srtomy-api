package org.ouracademy.srtomy.notification.gateway;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ouracademy.srtomy.outbox.PermanentDeliveryException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Sends through Resend's HTTP API (https://resend.com/docs/api-reference/emails/send-email). */
@Component
@ConditionalOnExpression("!'${resend.api-key:}'.isEmpty()")
class ResendEmailGateway implements EmailGateway {

  private static final Set<Integer> RETRYABLE_4XX = Set.of(408, 409, 425, 429);

  private final RestClient restClient;
  private final String from;
  private final String replyTo;

  ResendEmailGateway(
      @Value("${resend.api-key}") String apiKey,
      @Value("${resend.from:Sr. Tommy <invitaciones@ovejafiel.com>}") String from,
      @Value("${resend.url:https://api.resend.com}") String url,
      @Value("${resend.reply-to:}") String replyTo) {
    this.from = from;
    this.replyTo = replyTo;
    this.restClient =
        RestClient.builder()
            .baseUrl(url)
            .defaultHeader("Authorization", "Bearer " + apiKey)
            .build();
  }

  @Override
  public String send(String idempotencyKey, String to, String subject, String text) {
    try {
      var response =
          restClient
              .post()
              .uri("/emails")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Idempotency-Key", idempotencyKey)
              .body(body(to, subject, text))
              .retrieve()
              .body(Map.class);
      return response == null ? null : (String) response.get("id");
    } catch (HttpClientErrorException e) {
      if (refusedForGood(e.getStatusCode())) {
        throw new PermanentDeliveryException(
            "Resend refused the email: " + e.getStatusCode() + " " + e.getResponseBodyAsString(),
            e);
      }
      throw e;
    }
  }

  /**
   * A 4xx means the request itself is wrong (bad key, unverified domain, invalid address): sending
   * it again cannot work. Timeouts, rate limits and a concurrent request with the same key are
   * worth another try.
   */
  private static boolean refusedForGood(HttpStatusCode status) {
    return status.is4xxClientError() && !RETRYABLE_4XX.contains(status.value());
  }

  private Map<String, Object> body(String to, String subject, String text) {
    var body = new LinkedHashMap<String, Object>();
    body.put("from", from);
    body.put("to", List.of(to));
    body.put("subject", subject);
    body.put("text", text);
    if (!replyTo.isBlank()) {
      body.put("reply_to", replyTo);
    }
    return body;
  }
}
