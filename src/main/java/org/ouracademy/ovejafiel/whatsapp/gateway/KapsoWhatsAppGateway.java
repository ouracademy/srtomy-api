package org.ouracademy.ovejafiel.whatsapp.gateway;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.ouracademy.ovejafiel.outbox.PermanentDeliveryException;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;
import org.ouracademy.ovejafiel.whatsapp.domain.WhatsAppTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Talks to Kapso (https://docs.kapso.ai): the Platform API for customers and setup links, and its
 * WhatsApp Cloud API proxy for messages. Meta fees are paid by us with Kapso credits ({@code
 * partner_managed}) and billed to the church.
 */
@Slf4j
class KapsoWhatsAppGateway implements WhatsAppGateway {

  private static final Set<Integer> RETRYABLE_4XX = Set.of(408, 409, 425, 429);
  private static final String META_VERSION = "v24.0";
  private static final String LANGUAGE = "language";

  private final RestClient platform;
  private final RestClient meta;

  KapsoWhatsAppGateway(String apiKey, String url) {
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
  }

  @Override
  public String createCustomer(String externalId, String name) {
    var body = Map.of("customer", Map.of("name", name, "external_customer_id", externalId));
    var data = data(post(platform, "/customers", body));
    return (String) data.get("id");
  }

  @Override
  public PhoneInfo phoneInfo(String phoneNumberId) {
    var response =
        platform.get().uri("/whatsapp/phone_numbers/" + phoneNumberId).retrieve().body(Map.class);
    if (response == null) {
      return new PhoneInfo(null, null);
    }
    // The fields may come inside "data" or at the top.
    var body = response.get("data") instanceof Map<?, ?> data ? data : response;
    return new PhoneInfo(
        (String) body.get("display_phone_number"), (String) body.get("business_account_id"));
  }

  @Override
  public void createTemplate(String businessAccountId, WhatsAppTemplate template) {
    var parameters = new ArrayList<String>(template.examples());
    var body = new LinkedHashMap<String, Object>();
    body.put("name", template.metaName());
    body.put("category", WhatsAppTemplate.CATEGORY);
    body.put(LANGUAGE, WhatsAppTemplate.LANGUAGE);
    body.put("parameter_format", "POSITIONAL");
    body.put(
        "components",
        List.of(
            Map.of(
                "type",
                "BODY",
                "text",
                template.body(),
                "example",
                Map.of("body_text", List.of(parameters)))));
    post(meta, "/" + businessAccountId + "/message_templates", body);
  }

  @Override
  public void ensureMessageWebhook(String phoneNumberId, String url, String secret) {
    var path = "/whatsapp/phone_numbers/" + phoneNumberId + "/webhooks";
    var existing =
        platform
            .get()
            .uri(
                uri ->
                    uri.path(path)
                        .queryParam("url_contains", url)
                        .queryParam("kind", "kapso")
                        .build())
            .retrieve()
            .body(Map.class);
    if (existing != null
        && existing.get("data") instanceof List<?> hooks
        && hooks.stream()
            .anyMatch(h -> h instanceof Map<?, ?> hook && url.equals(hook.get("url")))) {
      log.info("Kapso: number {} already has its webhook of messages", phoneNumberId);
      return;
    }
    var webhook = new LinkedHashMap<String, Object>();
    webhook.put("kind", "kapso");
    webhook.put("url", url);
    webhook.put("events", List.of("whatsapp.message.received"));
    webhook.put("secret_key", secret);
    webhook.put("active", true);
    post(platform, path, Map.of("whatsapp_webhook", webhook));
    log.info("Kapso: webhook of messages made for number {}", phoneNumberId);
  }

  @Override
  public SetupLink createSetupLink(
      String customerId,
      ConnectionMode mode,
      String language,
      String successUrl,
      String failureUrl) {
    var link = new LinkedHashMap<String, Object>();
    link.put("allowed_connection_types", List.of(mode.kapsoValue()));
    link.put("meta_billing_mode", "partner_managed");
    link.put(LANGUAGE, language);
    if (successUrl != null && !successUrl.isBlank()) {
      link.put("success_redirect_url", successUrl);
    }
    if (failureUrl != null && !failureUrl.isBlank()) {
      link.put("failure_redirect_url", failureUrl);
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
            LANGUAGE,
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
      // The path and the status, and the answer cut short: what Kapso says is what tells why.
      log.warn(
          "Kapso refused POST {}: {} {}",
          uri,
          e.getStatusCode(),
          abbreviate(e.getResponseBodyAsString()));
      if (refusedForGood(e.getStatusCode())) {
        throw new PermanentDeliveryException(
            "Kapso refused the request: " + e.getStatusCode() + " " + e.getResponseBodyAsString(),
            e);
      }
      throw e;
    }
  }

  private static String abbreviate(String text) {
    return text.length() <= 300 ? text : text.substring(0, 300) + "…";
  }

  /** A 4xx means the request itself is wrong; timeouts and rate limits are worth another try. */
  private static boolean refusedForGood(HttpStatusCode status) {
    return status.is4xxClientError() && !RETRYABLE_4XX.contains(status.value());
  }
}
