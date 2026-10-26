package org.ouracademy.srtomy.invitation.query;

import java.time.LocalDateTime;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.invitation.domain.Invitation;
import org.ouracademy.srtomy.invitation.domain.InvitationId;
import org.ouracademy.srtomy.invitation.domain.InvitationStatus;
import org.ouracademy.srtomy.outbox.domain.OutboxStatus;
import org.ouracademy.srtomy.party.domain.PartyId;

/** An invitation as exposed outside the module (never the JPA entity, never the token). */
public record InvitationResult(
    InvitationId id,
    String email,
    PartyId organizationId,
    String organizationName,
    AccountabilityTypeId positionId,
    String positionName,
    PartyId personId,
    String personName,
    boolean needsName,
    InvitationStatus status,
    LocalDateTime expiresAt,
    String invitedByEmail,
    LocalDateTime createDate,
    // Null until asked of the outbox (and for invitations from before it).
    OutboxStatus emailStatus) {

  public static InvitationResult from(Invitation invitation, LocalDateTime now) {
    var position = invitation.getPosition();
    var person = invitation.getPerson();
    var personName =
        person != null
            ? person.getName()
            : java.util.stream.Stream.of(invitation.getFirstName(), invitation.getLastName())
                .filter(part -> part != null && !part.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
    // Whoever accepts must say their name when there is no person yet to take it from.
    var needsName =
        person == null && (isBlank(invitation.getFirstName()) || isBlank(invitation.getLastName()));
    return new InvitationResult(
        invitation.getId(),
        invitation.getEmail(),
        invitation.getOrganization().getId(),
        invitation.getOrganization().getName(),
        position == null ? null : position.getId(),
        position == null ? null : position.getName(),
        person == null ? null : person.getId(),
        personName == null || personName.isEmpty() ? null : personName,
        needsName,
        invitation.effectiveStatus(now),
        invitation.getExpiresAt(),
        invitation.getInvitedBy().getEmail(),
        invitation.getCreateDate(),
        null);
  }

  public InvitationResult withEmailStatus(OutboxStatus emailStatus) {
    return new InvitationResult(
        id,
        email,
        organizationId,
        organizationName,
        positionId,
        positionName,
        personId,
        personName,
        needsName,
        status,
        expiresAt,
        invitedByEmail,
        createDate,
        emailStatus);
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
