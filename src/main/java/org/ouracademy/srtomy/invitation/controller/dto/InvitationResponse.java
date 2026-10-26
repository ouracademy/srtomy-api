package org.ouracademy.srtomy.invitation.controller.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import org.ouracademy.srtomy.invitation.query.InvitationResult;

public record InvitationResponse(
    UUID id,
    String email,
    UUID organizationId,
    String organizationName,
    UUID positionId,
    String positionName,
    UUID personId,
    String personName,
    boolean needsName,
    String status,
    LocalDateTime expiresAt,
    String invitedByEmail,
    LocalDateTime createDate,
    // PENDING (queued or retrying), SENT or FAILED; null for invitations from before the outbox.
    String emailStatus) {

  public static InvitationResponse from(InvitationResult result) {
    return new InvitationResponse(
        result.id().value(),
        result.email(),
        result.organizationId().value(),
        result.organizationName(),
        result.positionId() == null ? null : result.positionId().value(),
        result.positionName(),
        result.personId() == null ? null : result.personId().value(),
        result.personName(),
        result.needsName(),
        result.status().name(),
        result.expiresAt(),
        result.invitedByEmail(),
        result.createDate(),
        result.emailStatus() == null ? null : result.emailStatus().name());
  }
}
