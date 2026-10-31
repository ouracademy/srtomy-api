package org.ouracademy.ovejafiel.whatsapp.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.whatsapp.command.disconnect.DisconnectCommand;
import org.ouracademy.ovejafiel.whatsapp.command.disconnect.DisconnectCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.command.request_setup_link.RequestSetupLinkCommand;
import org.ouracademy.ovejafiel.whatsapp.command.request_setup_link.RequestSetupLinkCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.controller.dto.SetupLinkRequest;
import org.ouracademy.ovejafiel.whatsapp.controller.dto.SetupLinkResponse;
import org.ouracademy.ovejafiel.whatsapp.controller.dto.WhatsAppConnectionResponse;
import org.ouracademy.ovejafiel.whatsapp.query.get_connection.GetConnectionQuery;
import org.ouracademy.ovejafiel.whatsapp.query.get_connection.GetConnectionQueryHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Connecting the WhatsApp of a church. Everything needs {@code org.update} on the node, which only
 * the owner and the administrators hold. The key of the provider never leaves the API: the web app
 * only gets the link the pastor opens.
 */
@RestController
@AllArgsConstructor
@Tag(name = "whatsapp")
public class WhatsAppController {

  RequestSetupLinkCommandHandler requestSetupLinkCommandHandler;
  DisconnectCommandHandler disconnectCommandHandler;
  GetConnectionQueryHandler getConnectionQueryHandler;
  AuthorizeQueryHandler authorizeQueryHandler;

  @PostMapping("organizations/{id}/whatsapp/setup-link")
  @ResponseStatus(HttpStatus.CREATED)
  public SetupLinkResponse requestSetupLink(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid SetupLinkRequest request) {
    var node = new PartyId(id);
    authorize(jwt, node);
    var result =
        requestSetupLinkCommandHandler.handle(new RequestSetupLinkCommand(node, request.mode()));
    return new SetupLinkResponse(result.url(), result.expiresAt());
  }

  @GetMapping("organizations/{id}/whatsapp")
  public WhatsAppConnectionResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    var node = new PartyId(id);
    authorize(jwt, node);
    return WhatsAppConnectionResponse.from(
        getConnectionQueryHandler.handle(new GetConnectionQuery(node)));
  }

  @DeleteMapping("organizations/{id}/whatsapp")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void disconnect(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    var node = new PartyId(id);
    authorize(jwt, node);
    disconnectCommandHandler.handle(new DisconnectCommand(node));
  }

  private void authorize(Jwt jwt, PartyId node) {
    authorizeQueryHandler.handle(new AuthorizeQuery(jwt.getSubject(), node, Permission.ORG_UPDATE));
  }
}
