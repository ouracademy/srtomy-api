package org.ouracademy.ovejafiel.whatsapp.controller;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.whatsapp.command.opt_out.OptOutCommand;
import org.ouracademy.ovejafiel.whatsapp.command.opt_out.OptOutCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.command.resolve_identity.ResolveIdentityCommand;
import org.ouracademy.ovejafiel.whatsapp.command.resolve_identity.ResolveIdentityCommandHandler;
import org.ouracademy.ovejafiel.whatsapp.command.resolve_identity.ResolveIdentityResult;
import org.ouracademy.ovejafiel.whatsapp.controller.dto.IdentityRequest;
import org.ouracademy.ovejafiel.whatsapp.controller.dto.OptOutResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * For the WhatsApp agent only, never the web app: the call must carry the Google ID token of the
 * agent's service account (see {@code InternalSecurityConfig}). The agent asks who wrote and gets a
 * short-lived token to act as that person; it never holds a key that can do everything.
 */
@RestController
@AllArgsConstructor
@Hidden
public class WhatsAppIdentityController {

  ResolveIdentityCommandHandler resolveIdentityCommandHandler;
  OptOutCommandHandler optOutCommandHandler;

  @PostMapping("internal/whatsapp/identity")
  public ResolveIdentityResult identity(@RequestBody @Valid IdentityRequest request) {
    return resolveIdentityCommandHandler.handle(
        new ResolveIdentityCommand(request.phoneNumberId(), request.from()));
  }

  @PostMapping("internal/whatsapp/opt-out")
  public OptOutResponse optOut(@RequestBody @Valid IdentityRequest request) {
    var result =
        optOutCommandHandler.handle(new OptOutCommand(request.phoneNumberId(), request.from()));
    return new OptOutResponse(result.people());
  }
}
