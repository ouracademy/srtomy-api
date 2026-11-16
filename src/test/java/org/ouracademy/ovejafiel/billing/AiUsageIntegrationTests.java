package org.ouracademy.ovejafiel.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.shared.ChurchApiTests;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

/**
 * The quota of the AI assistant from end to end, as the agent calls it: a free plan of two messages
 * and five cents a month, and a WhatsApp trial that ends as soon as it starts.
 */
@TestPropertySource(
    properties = {
      "billing.ai.free.messages=2",
      "billing.ai.free.max-cost-usd=0.05",
      "billing.free.whatsapp-trial-days=0"
    })
class AiUsageIntegrationTests extends ChurchApiTests {

  record Reply(int status, JsonNode body) {}

  Reply internal(String path, String body, String token) {
    var result =
        client
            .post()
            .uri("/internal/billing/ai-usage/" + path)
            .headers(h -> h.setBearerAuth(token))
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .exchange()
            .expectBody(String.class)
            .returnResult();
    try {
      var text = result.getResponseBody();
      return new Reply(
          result.getStatus().value(),
          text == null || text.isBlank() ? JSON.nullNode() : JSON.readTree(text));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  JsonNode reserve(String node, String channel) {
    var reply =
        internal(
            "reserve",
            "{\"nodeId\":\"%s\",\"channel\":\"%s\"}".formatted(node, channel),
            TestSecurityConfig.agentToken(jwtEncoder));
    assertThat(reply.status()).isEqualTo(200);
    return reply.body();
  }

  JsonNode record(String node, long input, long output) {
    var reply =
        internal(
            "record",
            ("{\"nodeId\":\"%s\",\"model\":\"claude-sonnet-5-5\",\"inputTokens\":%d,"
                    + "\"outputTokens\":%d,\"cacheReadTokens\":0,\"cacheWriteTokens\":0}")
                .formatted(node, input, output),
            TestSecurityConfig.agentToken(jwtEncoder));
    assertThat(reply.status()).isEqualTo(200);
    return reply.body();
  }

  @Test
  void theFreePlanAnswersItsMessagesOfTheMonthAndThenSaysWhy() {
    var first = reserve(location, "PORTAL");
    assertThat(first.get("allowed").asBoolean()).isTrue();
    assertThat(first.get("remaining").asInt()).isEqualTo(1);
    // Any node of the church counts for the church.
    assertThat(reserve(church, "PORTAL").get("remaining").asInt()).isZero();

    var third = reserve(location, "PORTAL");

    assertThat(third.get("allowed").asBoolean()).isFalse();
    assertThat(third.get("reason").asText()).isEqualTo("quota");
    assertThat(third.get("message").asText()).contains("2 mensajes gratis");
    var ai = get("/organizations/" + church + "/billing").get("ai");
    assertThat(ai.get("used").asInt()).isEqualTo(2);
    assertThat(ai.get("limit").asInt()).isEqualTo(2);
  }

  @Test
  void aFewExpensiveAnswersStopTheAssistantBeforeTheMessagesRunOut() {
    reserve(location, "PORTAL");
    // 10,000 output tokens of Sonnet 5.5 are 10 cents, over the 5 of the free plan.
    var recorded = record(location, 1_000, 10_000);
    assertThat(recorded.get("costUsd").decimalValue()).isEqualByComparingTo("0.102");

    var next = reserve(location, "PORTAL");

    assertThat(next.get("allowed").asBoolean()).isFalse();
    assertThat(next.get("reason").asText()).isEqualTo("spend");
    var ai = get("/organizations/" + church + "/billing").get("ai");
    assertThat(ai.get("costUsd").decimalValue()).isEqualByComparingTo("0.102");
    assertThat(ai.get("maxCostUsd").decimalValue()).isEqualByComparingTo("0.05");
  }

  @Test
  void whatsappNeedsThePlanOrItsTrialButThePortalDoesNot() {
    post("/organizations/" + church + "/whatsapp/setup-link", "{\"mode\":\"COEXISTENCE\"}");

    var whatsapp = reserve(location, "WHATSAPP");
    assertThat(whatsapp.get("allowed").asBoolean()).isFalse();
    assertThat(whatsapp.get("reason").asText()).isEqualTo("plan");
    assertThat(reserve(location, "PORTAL").get("allowed").asBoolean()).isTrue();

    post("/organizations/" + church + "/billing/checkout", "{\"country\":\"PE\",\"months\":1}");
    post("/organizations/" + church + "/billing/sync", null);
    var paid = reserve(location, "WHATSAPP");
    assertThat(paid.get("allowed").asBoolean()).isTrue();
    // The paid plan has 300 messages; one was used from the portal.
    assertThat(paid.get("remaining").asInt()).isEqualTo(298);
  }

  @Test
  void manyAnswersAtTheSameTimeNeverGoPastTheQuota() throws Exception {
    var pool = Executors.newFixedThreadPool(8);
    try {
      Callable<Boolean> one = () -> reserve(location, "PORTAL").get("allowed").asBoolean();
      var allowed = 0;
      for (var future : pool.invokeAll(java.util.Collections.nCopies(8, one))) {
        allowed += future.get() ? 1 : 0;
      }
      assertThat(allowed).isEqualTo(2);
    } finally {
      pool.shutdown();
    }
  }

  @Test
  void onlyTheAgentMayReserveOrRecord() {
    var body = "{\"nodeId\":\"%s\",\"channel\":\"PORTAL\"}".formatted(location);
    assertThat(internal("reserve", body, TestSecurityConfig.schedulerToken(jwtEncoder)).status())
        .isEqualTo(403);
    assertThat(internal("reserve", body, TestSecurityConfig.validToken(jwtEncoder, owner)).status())
        .isEqualTo(401);
  }

  @Test
  void anUnknownNodeIsNotFound() {
    var reply =
        internal(
            "reserve",
            "{\"nodeId\":\"0195f0a0-0000-7000-8000-00000000dead\",\"channel\":\"PORTAL\"}",
            TestSecurityConfig.agentToken(jwtEncoder));
    assertThat(reply.status()).isEqualTo(404);
  }
}
