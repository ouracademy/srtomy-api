package org.ouracademy.ovejafiel.invitation.query.list_invitations_of_organization;

import java.util.List;
import org.ouracademy.ovejafiel.invitation.query.InvitationResult;

public interface ListInvitationsOfOrganizationQueryHandler {
  /** Newest first; expired ones are included, flagged by their status. */
  List<InvitationResult> handle(ListInvitationsOfOrganizationQuery query);
}
