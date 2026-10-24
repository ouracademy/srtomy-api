package org.ouracademy.srtomy.invitation.query.find_invitation_by_id;

import java.util.Optional;
import org.ouracademy.srtomy.invitation.query.InvitationResult;

public interface FindInvitationByIdQueryHandler {
  Optional<InvitationResult> handle(FindInvitationByIdQuery query);
}
