package org.ouracademy.ovejafiel.invitation.query;

import java.time.LocalDateTime;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.invitation.domain.Invitation;
import org.ouracademy.ovejafiel.invitation.domain.InvitationId;
import org.ouracademy.ovejafiel.invitation.domain.InvitationStatus;
import org.ouracademy.ovejafiel.party.domain.PartyId;

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
    LocalDateTime createDate) {

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
        invitation.getCreateDate());
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
