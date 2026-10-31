package org.ouracademy.ovejafiel.whatsapp.gateway;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.ouracademy.ovejafiel.outbox.PermanentDeliveryException;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Talks to Kapso (https://docs.kapso.ai): the Platform API for customers and setup links, and its
 * WhatsApp Cloud API proxy for messages. Meta fees are paid by us with Kapso credits ({@code
 * partner_managed}) and billed to the church.
 */
@Component
@ConditionalOnExpression("!'${kapso.api-key:}'.isEmpty()")
class KapsoWhatsAppGateway implements WhatsAppGateway {

  private static final Set<Integer> RETRYABLE_4XX = Set.of(408, 409, 425, 429);
  private static final String META_VERSION = "v24.0";

  private final RestClient platform;
  private final RestClient meta;
  private final String successRedirectUrl;
  private final String failureRedirectUrl;

  KapsoWhatsAppGateway(
      @Value("${kapso.api-key}") String apiKey,
      @Value("${kapso.url:https://api.kapso.ai}") String url,
      @Value("${kapso.success-redirect-url:}") String successRedirectUrl,
      @Value("${kapso.failure-redirect-url:}") String failureRedirectUrl) {
    this.platform =
        RestClient.builder()
            .baseUrl(url + "/platform/v1")
            .defaultHeader("X-API-Key", apiKey)
            .build();
    this.meta =
        RestClient.builder()
            .baseUrl(url + "/meta/whatsapp/" + META_VERSION)
            .defaultHeader("X-API-Key", apiKey)
            .build();
    this.successRedirectUrl = successRedirectUrl;
    this.failureRedirectUrl = failureRedirectUrl;
  }

  @Override
  public String createCustomer(String externalId, String name) {
    var body = Map.of("customer", Map.of("name", name, "external_customer_id", externalId));
    var data = data(post(platform, "/customers", body));
    return (String) data.get("id");
  }

  @Override
  public SetupLink createSetupLink(String customerId, ConnectionMode mode, String language) {
    var link = new LinkedHashMap<String, Object>();
    link.put("allowed_connection_types", List.of(mode.kapsoValue()));
    link.put("meta_billing_mode", "partner_managed");
    link.put("language", language);
    if (!successRedirectUrl.isBlank()) {
      link.put("success_redirect_url", successRedirectUrl);
    }
    if (!failureRedirectUrl.isBlank()) {
      link.put("failure_redirect_url", failureRedirectUrl);
    }
    var data =
        data(
            post(
                platform, "/customers/" + customerId + "/setup_links", Map.of("setup_link", link)));
    return new SetupLink((String) data.get("url"), Instant.parse((String) data.get("expires_at")));
  }

  @Override
  public String sendTemplate(
      String phoneNumberId, String to, String template, String language, List<String> variables) {
    var parameters = new ArrayList<Map<String, String>>();
    for (var variable : variables) {
      parameters.add(Map.of("type", "text", "text", variable));
    }
    var body = new LinkedHashMap<String, Object>();
    body.put("messaging_product", "whatsapp");
    body.put("to", to);
    body.put("type", "template");
    body.put(
        "template",
        Map.of(
            "name",
            template,
            "language",
            Map.of("code", language),
            "components",
            List.of(Map.of("type", "body", "parameters", parameters))));
    var response = post(meta, "/" + phoneNumberId + "/messages", body);
    if (response != null
        && response.get("messages") instanceof List<?> messages
        && !messages.isEmpty()
        && messages.getFirst() instanceof Map<?, ?> first) {
      return (String) first.get("id");
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> data(Map<String, Object> response) {
    return (Map<String, Object>) response.get("data");
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> post(RestClient client, String uri, Object body) {
    try {
      return client
          .post()
          .uri(uri)
          .contentType(MediaType.APPLICATION_JSON)
          .body(body)
          .retrieve()
          .body(Map.class);
    } catch (HttpClientErrorException e) {
      if (refusedForGood(e.getStatusCode())) {
        throw new PermanentDeliveryException(
            "Kapso refused the request: " + e.getStatusCode() + " " + e.getResponseBodyAsString(),
            e);
      }
      throw e;
    }
  }

  /** A 4xx means the request itself is wrong; timeouts and rate limits are worth another try. */
  private static boolean refusedForGood(HttpStatusCode status) {
    return status.is4xxClientError() && !RETRYABLE_4XX.contains(status.value());
  }
}
