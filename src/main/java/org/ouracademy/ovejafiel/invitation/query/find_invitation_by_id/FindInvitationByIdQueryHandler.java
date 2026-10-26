package org.ouracademy.ovejafiel.invitation.query.find_invitation_by_id;

import java.util.Optional;
import org.ouracademy.ovejafiel.invitation.query.InvitationResult;

public interface FindInvitationByIdQueryHandler {
  Optional<InvitationResult> handle(FindInvitationByIdQuery query);
}
