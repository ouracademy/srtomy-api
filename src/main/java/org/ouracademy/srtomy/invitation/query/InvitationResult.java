package org.ouracademy.srtomy.invitation.query;

import java.time.LocalDateTime;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.invitation.domain.Invitation;
import org.ouracademy.srtomy.invitation.domain.InvitationId;
import org.ouracademy.srtomy.invitation.domain.InvitationStatus;
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
            : invitation.getFirstName() + " " + invitation.getLastName();
    return new InvitationResult(
        invitation.getId(),
        invitation.getEmail(),
        invitation.getOrganization().getId(),
        invitation.getOrganization().getName(),
        position == null ? null : position.getId(),
        position == null ? null : position.getName(),
        person == null ? null : person.getId(),
        personName,
        invitation.effectiveStatus(now),
        invitation.getExpiresAt(),
        invitation.getInvitedBy().getEmail(),
        invitation.getCreateDate());
  }
}
