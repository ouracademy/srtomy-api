package org.ouracademy.srtomy.invitation.controller.dto;

import java.util.UUID;
import org.ouracademy.srtomy.invitation.command.accept_invitation.AcceptInvitationResult;

public record AcceptInvitationResponse(UUID organizationId, UUID churchId, UUID personId) {

  public static AcceptInvitationResponse from(AcceptInvitationResult result) {
    return new AcceptInvitationResponse(
        result.organizationId().value(), result.churchId().value(), result.personId().value());
  }
}
