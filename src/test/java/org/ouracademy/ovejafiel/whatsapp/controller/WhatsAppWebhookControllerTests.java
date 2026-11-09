package org.ouracademy.ovejafiel.whatsapp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.SecurityConfig;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.ouracademy.ovejafiel.whatsapp.command.confirm_connection.ConfirmConnectionCommand;
import org.ouracademy.ovejafiel.whatsapp.command.confirm_connection.ConfirmConnectionCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.command.update_connection_state.UpdateConnectionStateCommand;
import org.ouracademy.ovejafiel.whatsapp.command.update_connection_state.UpdateConnectionStateCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.domain.DisconnectReason;
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
  @MockitoBean UpdateConnectionStateCommandHandler updateConnectionStateCommandHandler;

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
    verify(confirmConnectionCommandHandler, never()).handle(any());
  }

  private static final String NUMBER_EVENT =
      "{\"id\":\"wae_1\",\"phone_number_id\":\"123\",\"project\":{\"id\":\"p\"}}";
  private static final String ACCOUNT_EVENT =
      "{\"id\":\"wae_2\",\"phone_numbers\":[{\"id\":\"123\"},{\"id\":\"456\"}]}";

  @Test
  void aNumberThatIsDeletedOffboardedOrDisconnectedIsMarkedAsLostWithItsReason() {
    assertThat(post(NUMBER_EVENT, sign(NUMBER_EVENT), "whatsapp.phone_number.deleted"))
        .hasStatusOk();
    assertThat(post(NUMBER_EVENT, sign(NUMBER_EVENT), "whatsapp.phone_number.offboarded"))
        .hasStatusOk();
    assertThat(post(NUMBER_EVENT, sign(NUMBER_EVENT), "whatsapp.phone_number.disconnected"))
        .hasStatusOk();

    for (var reason :
        List.of(
            DisconnectReason.NUMBER_DELETED,
            DisconnectReason.NUMBER_OFFBOARDED,
            DisconnectReason.ACCESS_REMOVED)) {
      verify(updateConnectionStateCommandHandler)
          .handle(new UpdateConnectionStateCommand("key-1", List.of("123"), reason));
    }
  }

  @Test
  void aReconnectedNumberIsRestored() {
    assertThat(post(NUMBER_EVENT, sign(NUMBER_EVENT), "whatsapp.phone_number.reconnected"))
        .hasStatusOk();

    verify(updateConnectionStateCommandHandler)
        .handle(new UpdateConnectionStateCommand("key-1", List.of("123"), null));
  }

  @Test
  void aDisabledAccountLosesAllItsNumbersAndAReinstatedOneGetsThemBack() {
    assertThat(post(ACCOUNT_EVENT, sign(ACCOUNT_EVENT), "whatsapp.account.disabled")).hasStatusOk();
    assertThat(post(ACCOUNT_EVENT, sign(ACCOUNT_EVENT), "whatsapp.account.reinstated"))
        .hasStatusOk();

    verify(updateConnectionStateCommandHandler)
        .handle(
            new UpdateConnectionStateCommand(
                "key-1", List.of("123", "456"), DisconnectReason.ACCOUNT_DISABLED));
    verify(updateConnectionStateCommandHandler)
        .handle(new UpdateConnectionStateCommand("key-1", List.of("123", "456"), null));
  }

  @Test
  void theEventNameCanComeInTheBodyOfAV2Payload() {
    var body = "{\"event\":\"whatsapp.phone_number.deleted\",\"phone_number_id\":\"123\"}";

    assertThat(post(body, sign(body), null)).hasStatusOk();

    verify(updateConnectionStateCommandHandler)
        .handle(
            new UpdateConnectionStateCommand(
                "key-1", List.of("123"), DisconnectReason.NUMBER_DELETED));
  }

  @Test
  void aLifecycleEventWithoutNumbersIsABadRequestAndOneWithAWrongSignatureIsRefused() {
    var empty = "{\"project\":{\"id\":\"p\"}}";

    assertThat(post(empty, sign(empty), "whatsapp.phone_number.deleted")).hasStatus(400);
    assertThat(post(NUMBER_EVENT, "deadbeef", "whatsapp.phone_number.deleted")).hasStatus(401);
    verifyNoInteractions(updateConnectionStateCommandHandler);
  }

  @Test
  void restrictionsAreOnlyLoggedBecauseTheNumberStillWorks() {
    assertThat(post(ACCOUNT_EVENT, sign(ACCOUNT_EVENT), "whatsapp.account.restricted"))
        .hasStatusOk();

    verifyNoInteractions(updateConnectionStateCommandHandler);
  }
}
