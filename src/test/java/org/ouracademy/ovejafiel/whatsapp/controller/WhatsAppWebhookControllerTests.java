package org.ouracademy.ovejafiel.whatsapp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.SecurityConfig;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.ouracademy.ovejafiel.whatsapp.command.confirm_connection.ConfirmConnectionCommand;
import org.ouracademy.ovejafiel.whatsapp.command.confirm_connection.ConfirmConnectionCommandHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(WhatsAppWebhookController.class)
@ContextConfiguration(classes = OvejafielApplication.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
@TestPropertySource(properties = "kapso.webhook-secret=s3cret")
class WhatsAppWebhookControllerTests {

  private static final String URI = "/v1/webhooks/whatsapp";
  private static final String CONNECTED =
      "{\"phone_number_id\":\"123\",\"project\":{\"id\":\"p\"},\"customer\":{\"id\":\"cus-1\"}}";

  @Autowired MockMvcTester mvc;

  @MockitoBean ConfirmConnectionCommandHandler confirmConnectionCommandHandler;

  private static String sign(String body) {
    return HexFormat.of()
        .formatHex(WebhookSignature.hmac(body.getBytes(StandardCharsets.UTF_8), "s3cret"));
  }

  private MockMvcTester.MockMvcRequestBuilder post(String body, String signature, String event) {
    var request = mvc.post().uri(URI).contentType(MediaType.APPLICATION_JSON).content(body);
    if (signature != null) {
      request.header("X-Webhook-Signature", signature);
    }
    if (event != null) {
      request.header("X-Webhook-Event", event);
    }
    return request.header("X-Idempotency-Key", "key-1");
  }

  @Test
  void aSignedConnectionEventConfirmsTheConnectionWithoutAnAuth0Token() {
    assertThat(post(CONNECTED, sign(CONNECTED), "whatsapp.phone_number.created")).hasStatusOk();

    verify(confirmConnectionCommandHandler)
        .handle(new ConfirmConnectionCommand("key-1", "cus-1", "123"));
  }

  @Test
  void aWrongOrMissingSignatureIsRefusedBeforeAnythingIsRead() {
    assertThat(post(CONNECTED, "deadbeef", "whatsapp.phone_number.created")).hasStatus(401);
    assertThat(post(CONNECTED, null, "whatsapp.phone_number.created")).hasStatus(401);
    verifyNoInteractions(confirmConnectionCommandHandler);
  }

  @Test
  void aSignatureOfAnotherBodyIsRefused() {
    assertThat(post(CONNECTED, sign("{}"), "whatsapp.phone_number.created")).hasStatus(401);
    verifyNoInteractions(confirmConnectionCommandHandler);
  }

  @Test
  void eventsWeDoNotUseAreAcknowledgedAndIgnored() {
    assertThat(post(CONNECTED, sign(CONNECTED), "whatsapp.message.sent")).hasStatusOk();
    verifyNoInteractions(confirmConnectionCommandHandler);
  }

  @Test
  void aConnectionEventWithoutIdsIsABadRequest() {
    var body = "{\"project\":{\"id\":\"p\"}}";

    assertThat(post(body, sign(body), "whatsapp.phone_number.created")).hasStatus(400);
    assertThat(post("not json", sign("not json"), "whatsapp.phone_number.created")).hasStatus(400);
    verify(confirmConnectionCommandHandler, org.mockito.Mockito.never()).handle(any());
  }
}
