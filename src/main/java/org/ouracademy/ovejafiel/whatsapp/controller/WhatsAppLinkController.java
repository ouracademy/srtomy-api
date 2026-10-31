package org.ouracademy.ovejafiel.whatsapp.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.whatsapp.command.create_link_code.CreateLinkCodeCommand;
import org.ouracademy.ovejafiel.whatsapp.command.create_link_code.CreateLinkCodeCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.command.unlink_whatsapp.UnlinkWhatsAppCommand;
import org.ouracademy.ovejafiel.whatsapp.command.unlink_whatsapp.UnlinkWhatsAppCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.controller.dto.LinkCodeResponse;
import org.ouracademy.ovejafiel.whatsapp.controller.dto.WhatsAppLinkResponse;
import org.ouracademy.ovejafiel.whatsapp.query.get_link.GetLinkQuery;
import org.ouracademy.ovejafiel.whatsapp.query.get_link.GetLinkQueryHandler;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * A signed-in person links their own WhatsApp to their own account. No permission on the node is
 * needed (it is about the caller, not the church): the handlers check that the caller has a person
 * in that church.
 */
@RestController
@AllArgsConstructor
@Tag(name = "whatsapp")
public class WhatsAppLinkController {

  CreateLinkCodeCommandHandler createLinkCodeCommandHandler;
  UnlinkWhatsAppCommandHandler unlinkWhatsAppCommandHandler;
  GetLinkQueryHandler getLinkQueryHandler;

  /**
   * The code to send from the WhatsApp to link, valid for ten minutes. Asking again replaces it.
   */
  @PostMapping("organizations/{id}/whatsapp/link-code")
  @ResponseStatus(HttpStatus.CREATED)
  public LinkCodeResponse createCode(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    var result =
        createLinkCodeCommandHandler.handle(
            new CreateLinkCodeCommand(jwt.getSubject(), new PartyId(id)));
    return new LinkCodeResponse(result.code(), result.expiresAt(), result.displayNumber());
  }

  @GetMapping("organizations/{id}/whatsapp/link")
  public WhatsAppLinkResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    var result = getLinkQueryHandler.handle(new GetLinkQuery(jwt.getSubject(), new PartyId(id)));
    return new WhatsAppLinkResponse(result.linked(), result.maskedPhone(), result.verifiedAt());
  }

  @DeleteMapping("organizations/{id}/whatsapp/link")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void unlink(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    unlinkWhatsAppCommandHandler.handle(
        new UnlinkWhatsAppCommand(jwt.getSubject(), new PartyId(id)));
  }
}
