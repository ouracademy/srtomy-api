package org.ouracademy.ovejafiel.whatsapp.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.outbox.PermanentDeliveryException;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;
import org.ouracademy.ovejafiel.whatsapp.domain.WhatsAppTemplate;

/** Against a local server that plays Kapso: what is sent and how its answers are read. */
class KapsoWhatsAppGatewayTests {

  record Call(String method, String path, String apiKey, String body) {}

  private HttpServer server;
  private final List<Call> calls = new CopyOnWriteArrayList<>();
  private volatile int status = 200;
  private volatile String answer = "{}";
  private KapsoWhatsAppGateway gateway;

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          var body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          calls.add(
              new Call(
                  exchange.getRequestMethod(),
                  exchange.getRequestURI().getPath(),
                  exchange.getRequestHeaders().getFirst("X-API-Key"),
                  body));
          var bytes = answer.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    gateway =
        new KapsoWhatsAppGateway("key-1", "http://127.0.0.1:" + server.getAddress().getPort());
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  @Test
  void createsTheCustomerWithOurIdAsExternalId() {
    answer = "{\"data\":{\"id\":\"cus-9\",\"name\":\"Iglesia\"}}";

    var id = gateway.createCustomer("church-1", "Iglesia");

    assertThat(id).isEqualTo("cus-9");
    var call = calls.getFirst();
    assertThat(call.method()).isEqualTo("POST");
    assertThat(call.path()).isEqualTo("/platform/v1/customers");
    assertThat(call.apiKey()).isEqualTo("key-1");
    assertThat(call.body())
        .contains("\"external_customer_id\":\"church-1\"", "\"name\":\"Iglesia\"");
  }

  @Test
  void createsASpanishLinkInTheChosenModePaidByUs() {
    answer =
        "{\"data\":{\"id\":\"l1\",\"url\":\"https://setup.kapso.ai/s/abc\","
            + "\"expires_at\":\"2026-11-30T10:00:00Z\"}}";

    var link =
        gateway.createSetupLink(
            "cus-9",
            ConnectionMode.COEXISTENCE,
            "es",
            "https://app.example/ok",
            "https://app.example/fail");

    assertThat(link.url()).isEqualTo("https://setup.kapso.ai/s/abc");
    assertThat(link.expiresAt()).isEqualTo(Instant.parse("2026-11-30T10:00:00Z"));
    var call = calls.getFirst();
    assertThat(call.path()).isEqualTo("/platform/v1/customers/cus-9/setup_links");
    assertThat(call.body())
        .contains(
            "\"allowed_connection_types\":[\"coexistence\"]",
            "\"meta_billing_mode\":\"partner_managed\"",
            "\"language\":\"es\"",
            "\"success_redirect_url\":\"https://app.example/ok\"",
            "\"failure_redirect_url\":\"https://app.example/fail\"");
  }

  @Test
  void sendsTheTemplateWithItsVariablesAndReturnsTheMessageId() {
    answer = "{\"messaging_product\":\"whatsapp\",\"messages\":[{\"id\":\"wamid.77\"}]}";

    var id =
        gateway.sendTemplate("pn-1", "51999888777", "welcome", "es", List.of("Ana", "Central"));

    assertThat(id).isEqualTo("wamid.77");
    var call = calls.getFirst();
    assertThat(call.path()).isEqualTo("/meta/whatsapp/v24.0/pn-1/messages");
    assertThat(call.apiKey()).isEqualTo("key-1");
    assertThat(call.body())
        .contains(
            "\"to\":\"51999888777\"",
            "\"type\":\"template\"",
            "\"name\":\"welcome\"",
            "\"code\":\"es\"",
            "\"text\":\"Ana\"",
            "\"text\":\"Central\"");
  }

  @Test
  void readsTheNumberPeopleSeeAndTheBusinessAccountOfAConnectedNumber() {
    answer =
        "{\"data\":{\"display_phone_number\":\"+51 999 888 777\","
            + "\"business_account_id\":\"waba-9\"}}";

    var info = gateway.phoneInfo("pn-1");

    assertThat(info.displayNumber()).isEqualTo("+51 999 888 777");
    assertThat(info.businessAccountId()).isEqualTo("waba-9");
    assertThat(calls.getFirst().method()).isEqualTo("GET");
    assertThat(calls.getFirst().path()).isEqualTo("/platform/v1/whatsapp/phone_numbers/pn-1");
  }

  @Test
  void theFieldsMayAlsoComeAtTheTopOfTheAnswer() {
    answer = "{\"display_phone_number\":\"+57 300\",\"business_account_id\":\"w\"}";

    assertThat(gateway.phoneInfo("pn-1").displayNumber()).isEqualTo("+57 300");
  }

  @Test
  void submitsTheTemplateToMetaAsUtilityInSpanishWithAnExamplePerVariable() {
    answer = "{\"id\":\"123\",\"status\":\"PENDING\"}";

    gateway.createTemplate("waba-9", WhatsAppTemplate.EVENT_REMINDER);

    var call = calls.getFirst();
    assertThat(call.method()).isEqualTo("POST");
    assertThat(call.path()).isEqualTo("/meta/whatsapp/v24.0/waba-9/message_templates");
    assertThat(call.apiKey()).isEqualTo("key-1");
    assertThat(call.body())
        .contains(
            "\"name\":\"event_reminder\"",
            "\"category\":\"UTILITY\"",
            "\"language\":\"es\"",
            "\"parameter_format\":\"POSITIONAL\"",
            "{{4}}",
            "\"body_text\":[[\"Ana\",\"Culto de jóvenes\"");
  }

  @Test
  void aTemplateMetaRefusesRaisesAPermanentError() {
    status = 400;
    answer = "{\"error\":\"template already exists\"}";

    assertThatThrownBy(() -> gateway.createTemplate("waba-9", WhatsAppTemplate.EVENT_REMINDER))
        .isInstanceOf(PermanentDeliveryException.class)
        .hasMessageContaining("400");
  }

  @Test
  void aMessageWithoutIdInTheAnswerReturnsNull() {
    answer = "{}";

    assertThat(gateway.sendTemplate("pn", "519", "welcome", "es", List.of("a", "b"))).isNull();
  }

  @Test
  void aRefusalOfTheRequestIsPermanent() {
    status = 422;
    answer = "{\"error\":\"recipient not on whatsapp\"}";

    assertThatThrownBy(() -> gateway.sendTemplate("pn", "519", "welcome", "es", List.of("a", "b")))
        .isInstanceOf(PermanentDeliveryException.class)
        .hasMessageContaining("422");
  }

  @Test
  void rateLimitsAndServerErrorsAreWorthAnotherTry() {
    status = 429;
    assertThatThrownBy(() -> gateway.sendTemplate("pn", "519", "welcome", "es", List.of("a", "b")))
        .isNotInstanceOf(PermanentDeliveryException.class);

    status = 503;
    assertThatThrownBy(() -> gateway.sendTemplate("pn", "519", "welcome", "es", List.of("a", "b")))
        .isNotInstanceOf(PermanentDeliveryException.class);
  }

  @Test
  void makesTheWebhookOfMessagesOfTheNumberWithOurSecret() {
    answer = "{\"data\":[]}";

    gateway.ensureMessageWebhook("pn-7", "https://agent.example/webhooks/kapso", "agent-secret");

    assertThat(calls).hasSize(2);
    var list = calls.get(0);
    assertThat(list.method()).isEqualTo("GET");
    assertThat(list.path()).isEqualTo("/platform/v1/whatsapp/phone_numbers/pn-7/webhooks");
    var create = calls.get(1);
    assertThat(create.method()).isEqualTo("POST");
    assertThat(create.path()).isEqualTo("/platform/v1/whatsapp/phone_numbers/pn-7/webhooks");
    assertThat(create.apiKey()).isEqualTo("key-1");
    assertThat(create.body())
        .contains("\"url\":\"https://agent.example/webhooks/kapso\"")
        .contains("\"secret_key\":\"agent-secret\"")
        .contains("\"whatsapp.message.received\"")
        .contains("\"kind\":\"kapso\"");
  }

  @Test
  void doesNotMakeASecondWebhookWhenTheNumberAlreadyHasIt() {
    answer = "{\"data\":[{\"id\":\"w1\",\"url\":\"https://agent.example/webhooks/kapso\"}]}";

    gateway.ensureMessageWebhook("pn-7", "https://agent.example/webhooks/kapso", "agent-secret");

    assertThat(calls).hasSize(1);
    assertThat(calls.getFirst().method()).isEqualTo("GET");
  }
}
