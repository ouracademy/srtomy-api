package org.ouracademy.ovejafiel.whatsapp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.SecurityConfig;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.ouracademy.ovejafiel.whatsapp.command.disconnect.DisconnectCommand;
import org.ouracademy.ovejafiel.whatsapp.command.disconnect.DisconnectCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.command.disconnect.DisconnectResult;
import org.ouracademy.ovejafiel.whatsapp.command.request_setup_link.RequestSetupLinkCommand;
import org.ouracademy.ovejafiel.whatsapp.command.request_setup_link.RequestSetupLinkCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.command.request_setup_link.RequestSetupLinkResult;
import org.ouracademy.ovejafiel.whatsapp.domain.ConnectionMode;
import org.ouracademy.ovejafiel.whatsapp.query.get_connection.GetConnectionQueryHandler;
import org.ouracademy.ovejafiel.whatsapp.query.get_connection.GetConnectionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(WhatsAppController.class)
@ContextConfiguration(classes = OvejafielApplication.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class WhatsAppControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean RequestSetupLinkCommandHandler requestSetupLinkCommandHandler;
  @MockitoBean DisconnectCommandHandler disconnectCommandHandler;
  @MockitoBean GetConnectionQueryHandler getConnectionQueryHandler;
  @MockitoBean AuthorizeQueryHandler authorizeQueryHandler;

  PartyId node = PartyId.generate();
  String uri = "/v1/organizations/" + node.value() + "/whatsapp";

  String bearer() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  @Test
  void requestingTheLinkNeedsOrgUpdateAndReturnsTheUrl() {
    var expires = Instant.parse("2026-12-01T10:00:00Z");
    given(requestSetupLinkCommandHandler.handle(any()))
        .willReturn(new RequestSetupLinkResult("https://setup.kapso.ai/s/abc", expires));

    var body =
        assertThat(
                mvc.post()
                    .uri(uri + "/setup-link")
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"mode\":\"COEXISTENCE\"}"))
            .hasStatus(201)
            .bodyJson();
    body.extractingPath("$.url").isEqualTo("https://setup.kapso.ai/s/abc");

    verify(authorizeQueryHandler)
        .handle(new AuthorizeQuery("test-user", node, Permission.ORG_UPDATE));
    verify(requestSetupLinkCommandHandler)
        .handle(new RequestSetupLinkCommand(node, ConnectionMode.COEXISTENCE));
  }

  @Test
  void aMissingOrUnknownModeIsRejected() {
    for (var content : new String[] {"{}", "{\"mode\":\"OTHER\"}"}) {
      assertThat(
              mvc.post()
                  .uri(uri + "/setup-link")
                  .header("Authorization", bearer())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(content))
          .hasStatus(400);
    }
    verifyNoInteractions(requestSetupLinkCommandHandler);
  }

  @Test
  void withoutThePermissionNothingHappens() {
    given(authorizeQueryHandler.handle(any())).willThrow(new AccessDeniedException("no"));

    assertThat(
            mvc.post()
                .uri(uri + "/setup-link")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"mode\":\"DEDICATED\"}"))
        .hasStatus(403);
    assertThat(mvc.get().uri(uri).header("Authorization", bearer())).hasStatus(403);
    assertThat(mvc.delete().uri(uri).header("Authorization", bearer())).hasStatus(403);
    verifyNoInteractions(
        requestSetupLinkCommandHandler, getConnectionQueryHandler, disconnectCommandHandler);
  }

  @Test
  void everythingNeedsASignedInUser() {
    assertThat(mvc.get().uri(uri)).hasStatus(401);
  }

  @Test
  void showsTheStateOfTheConnection() {
    given(getConnectionQueryHandler.handle(any()))
        .willReturn(
            new GetConnectionResult(
                GetConnectionResult.State.CONNECTED,
                ConnectionMode.DEDICATED,
                "+51 999 111 222",
                null,
                true,
                PartyId.generate()));

    var body =
        assertThat(mvc.get().uri(uri).header("Authorization", bearer())).hasStatusOk().bodyJson();
    body.extractingPath("$.status").isEqualTo("CONNECTED");
    body.extractingPath("$.displayNumber").isEqualTo("+51 999 111 222");
    body.extractingPath("$.inherited").isEqualTo(true);
  }

  @Test
  void disconnectingAnswersNoContent() {
    given(disconnectCommandHandler.handle(any())).willReturn(new DisconnectResult(true));

    assertThat(mvc.delete().uri(uri).header("Authorization", bearer())).hasStatus(204);

    verify(disconnectCommandHandler).handle(new DisconnectCommand(node));
  }

  @Test
  void aChurchWithoutWhatsappCannotBeDisconnected() {
    given(disconnectCommandHandler.handle(any())).willThrow(new NoSuchElementException("none"));

    assertThat(mvc.delete().uri(uri).header("Authorization", bearer())).hasStatus(404);
  }
}
