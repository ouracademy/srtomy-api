package org.ouracademy.srtomy.invitation.query.list_invitations_for_email;

import java.util.List;
import org.ouracademy.srtomy.invitation.query.InvitationResult;

public interface ListInvitationsForEmailQueryHandler {
  List<InvitationResult> handle(ListInvitationsForEmailQuery query);
}
