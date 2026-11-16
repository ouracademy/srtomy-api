package org.ouracademy.ovejafiel.billing.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.billing.command.sync_payment.SyncPaymentCommand;
import org.ouracademy.ovejafiel.billing.command.sync_payment.SyncPaymentCommandHandler;
import org.ouracademy.ovejafiel.billing.command.sync_payment.SyncPaymentResult;
import org.ouracademy.ovejafiel.billing.gateway.PaymentGateway;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.SecurityConfig;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(BillingWebhookController.class)
@ContextConfiguration(classes = OvejafielApplication.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class BillingWebhookControllerTests {

  static final String SIGNED = "V2-HMAC-SHA256, Signature: abc";

  @Autowired MockMvcTester mvc;

  @MockitoBean PaymentGateway gateway;
  @MockitoBean SyncPaymentCommandHandler syncPaymentCommandHandler;

  @Test
  void aSignedNotificationMakesThePaymentBeCheckedAndNeedsNoAccount() {
    given(gateway.isAuthentic(any(), any())).willReturn(true);
    given(syncPaymentCommandHandler.handle(any())).willReturn(new SyncPaymentResult(true, true));

    assertThat(
            mvc.post()
                .uri("/v1/webhooks/billing")
                .header("Authorization", SIGNED)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"payment_id\":\"DP-9\"}"))
        .hasStatusOk();

    verify(gateway).isAuthentic("{\"payment_id\":\"DP-9\"}".getBytes(), SIGNED);
    verify(syncPaymentCommandHandler).handle(new SyncPaymentCommand("DP-9"));
  }

  @Test
  void aPaymentThatIsNotOursIsStillAnswered200SoItIsNotRetried() {
    given(gateway.isAuthentic(any(), any())).willReturn(true);
    given(syncPaymentCommandHandler.handle(any())).willReturn(new SyncPaymentResult(false, false));

    assertThat(
            mvc.post()
                .uri("/v1/webhooks/billing")
                .header("Authorization", SIGNED)
                .content("{\"payment_id\":\"DP-other\"}"))
        .hasStatusOk();
  }

  @Test
  void aWrongOrMissingSignatureIsRefusedBeforeTheBodyIsRead() {
    given(gateway.isAuthentic(any(), any())).willReturn(false);

    assertThat(
            mvc.post()
                .uri("/v1/webhooks/billing")
                .header("Authorization", "V2-HMAC-SHA256, Signature: forged")
                .content("{\"payment_id\":\"DP-9\"}"))
        .hasStatus(401);
    assertThat(mvc.post().uri("/v1/webhooks/billing").content("{\"payment_id\":\"DP-9\"}"))
        .hasStatus(401);

    verifyNoInteractions(syncPaymentCommandHandler);
  }

  @Test
  void aSignedBodyThatNamesNoPaymentIsABadRequest() {
    given(gateway.isAuthentic(any(), any())).willReturn(true);

    for (var content : new String[] {"not json", "{}", "{\"payment_id\":\"\"}"}) {
      assertThat(
              mvc.post()
                  .uri("/v1/webhooks/billing")
                  .header("Authorization", SIGNED)
                  .content(content))
          .hasStatus(400);
    }
    verifyNoInteractions(syncPaymentCommandHandler);
  }
}
