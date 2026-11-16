package org.ouracademy.ovejafiel.billing.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.billing.domain.InvoiceStatus;

/** Against a local server that plays dLocal Go: what is sent and how its answers are read. */
class DLocalGoPaymentGatewayTests {

  record Call(String method, String path, String authorization, String body) {}

  private HttpServer server;
  private final List<Call> calls = new CopyOnWriteArrayList<>();
  private volatile String answer = "{}";
  private DLocalGoPaymentGateway gateway;

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
                  exchange.getRequestHeaders().getFirst("Authorization"),
                  body));
          var bytes = answer.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
    gateway =
        new DLocalGoPaymentGateway(
            "key-1",
            "secret-1",
            "http://127.0.0.1:" + server.getAddress().getPort(),
            "https://api.example/v1/webhooks/billing");
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
  }

  @Test
  void createsAPaymentWithOurInvoiceAsOrderAndReturnsTheCheckout() {
    answer = "{\"id\":\"DP-9\",\"status\":\"PENDING\",\"redirect_url\":\"https://checkout/9\"}";

    var payment =
        gateway.createPayment(
            new PaymentGateway.PaymentRequest(
                "inv-1",
                new BigDecimal("75"),
                "PEN",
                "PE",
                "Plan Iglesia",
                "https://app/ok",
                "https://app/back"));

    assertThat(payment.id()).isEqualTo("DP-9");
    assertThat(payment.checkoutUrl()).isEqualTo("https://checkout/9");
    var call = calls.getFirst();
    assertThat(call.method()).isEqualTo("POST");
    assertThat(call.path()).isEqualTo("/v1/payments");
    assertThat(call.authorization()).isEqualTo("Bearer key-1:secret-1");
    assertThat(call.body())
        .contains(
            "\"order_id\":\"inv-1\"",
            "\"amount\":75",
            "\"currency\":\"PEN\"",
            "\"country\":\"PE\"",
            "\"success_url\":\"https://app/ok\"",
            "\"back_url\":\"https://app/back\"",
            "\"notification_url\":\"https://api.example/v1/webhooks/billing\"");
  }

  @Test
  void anAnswerWithoutIdOrLinkIsAnError() {
    answer = "{\"status\":\"PENDING\"}";

    assertThatThrownBy(
            () ->
                gateway.createPayment(
                    new PaymentGateway.PaymentRequest(
                        "inv-1", BigDecimal.ONE, "PEN", "PE", "x", "https://a", "https://b")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void readsHowAPaymentWent() {
    answer = "{\"id\":\"DP-9\",\"order_id\":\"inv-1\",\"status\":\"PAID\"}";

    var state = gateway.findPayment("DP-9");

    assertThat(state.status()).isEqualTo(InvoiceStatus.PAID);
    assertThat(state.orderId()).isEqualTo("inv-1");
    assertThat(calls.getFirst().path()).isEqualTo("/v1/payments/DP-9");
    assertThat(calls.getFirst().method()).isEqualTo("GET");
  }

  @Test
  void anUnknownStatusIsStillPending() {
    assertThat(DLocalGoPaymentGateway.status("AUTHORIZED")).isEqualTo(InvoiceStatus.PENDING);
    assertThat(DLocalGoPaymentGateway.status(null)).isEqualTo(InvoiceStatus.PENDING);
    assertThat(DLocalGoPaymentGateway.status("REJECTED")).isEqualTo(InvoiceStatus.REJECTED);
    assertThat(DLocalGoPaymentGateway.status("EXPIRED")).isEqualTo(InvoiceStatus.EXPIRED);
  }

  @Test
  void aNotificationSignedWithOurKeysIsAuthentic() {
    var body = "{\"payment_id\":\"DP-9\"}".getBytes(StandardCharsets.UTF_8);
    var header = "V2-HMAC-SHA256, Signature: " + gateway.signature(body);

    assertThat(gateway.isAuthentic(body, header)).isTrue();
  }

  @Test
  void theSignatureFollowsTheFormulaOfTheDocumentation() {
    // The example of https://docs.dlocalgo.com (notifications): HMAC-SHA256 of the API key followed
    // by the body, with the secret key.
    var documented = new DLocalGoPaymentGateway("MyMerchantApiKey", "MyMerchantSecretKey", "x", "");
    var body = "{\"payment_id\":\"DP-283\"}".getBytes(StandardCharsets.UTF_8);

    // Computed apart from this code (Python's hmac) for the same inputs.
    assertThat(documented.signature(body))
        .isEqualTo("67ba82a93deddefd9dfb8cfb36b3248d336ae1924a79dd07bb69c7e238a5aae7");
  }

  @Test
  void anythingElseIsRefused() {
    var body = "{\"payment_id\":\"DP-9\"}".getBytes(StandardCharsets.UTF_8);
    var good = gateway.signature(body);
    var other = "{\"payment_id\":\"DP-10\"}".getBytes(StandardCharsets.UTF_8);

    assertThat(gateway.isAuthentic(body, null)).isFalse();
    assertThat(gateway.isAuthentic(body, good)).isFalse();
    assertThat(gateway.isAuthentic(body, "Bearer " + good)).isFalse();
    assertThat(gateway.isAuthentic(body, "V2-HMAC-SHA256, Signature: 00")).isFalse();
    assertThat(gateway.isAuthentic(other, "V2-HMAC-SHA256, Signature: " + good)).isFalse();
  }

  @Test
  void withoutKeysTheSandboxChargesNobodyAndAcceptsNoNotification() {
    var config = new PaymentGatewayConfig();
    var sandbox = config.paymentGateway("", "", "https://api.dlocalgo.com", "");

    assertThat(sandbox).isInstanceOf(SandboxPaymentGateway.class);
    var payment =
        sandbox.createPayment(
            new PaymentGateway.PaymentRequest(
                "inv-7", BigDecimal.TEN, "PEN", "PE", "x", "https://app/ok", "https://app/back"));
    assertThat(payment.checkoutUrl()).isEqualTo("https://app/ok");
    var state = sandbox.findPayment(payment.id());
    assertThat(state.status()).isEqualTo(InvoiceStatus.PAID);
    assertThat(state.orderId()).isEqualTo("inv-7");
    assertThat(sandbox.isAuthentic("{}".getBytes(StandardCharsets.UTF_8), "anything")).isFalse();

    assertThat(config.paymentGateway("k", "s", "https://api.dlocalgo.com", ""))
        .isInstanceOf(DLocalGoPaymentGateway.class);
    assertThat(config.paymentGateway("k", "", "https://api.dlocalgo.com", ""))
        .isInstanceOf(SandboxPaymentGateway.class);
  }
}
