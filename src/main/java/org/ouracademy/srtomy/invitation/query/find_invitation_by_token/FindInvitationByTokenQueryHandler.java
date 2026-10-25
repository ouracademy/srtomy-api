package org.ouracademy.srtomy.invitation.query.find_invitation_by_token;

import java.util.Optional;
import org.ouracademy.srtomy.invitation.query.InvitationResult;

public interface FindInvitationByTokenQueryHandler {
  Optional<InvitationResult> handle(FindInvitationByTokenQuery query);
}
