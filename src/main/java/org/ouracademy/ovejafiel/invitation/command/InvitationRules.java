package org.ouracademy.ovejafiel.invitation.command;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.query.PositionResult;
import org.ouracademy.ovejafiel.accountability.query.get_person_accountabilities.GetPersonAccountabilitiesQuery;
import org.ouracademy.ovejafiel.accountability.query.get_person_accountabilities.GetPersonAccountabilitiesQueryHandler;
import org.ouracademy.ovejafiel.accountability.query.get_person_accountabilities.PersonAccountabilityResult;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.authorize_access_grant.AuthorizeAccessGrantQuery;
import org.ouracademy.ovejafiel.iam.query.authorize_access_grant.AuthorizeAccessGrantQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.stereotype.Component;

/**
 * Who may hand out what through an invitation, used when it is sent and again when it is accepted.
 * Nobody gives more access than they have: a position that carries permissions can only be given by
 * someone who holds all of them, and so can the access a person already has, since linking an
 * account to that person hands it over.
 */
@Component
@RequiredArgsConstructor
public class InvitationRules {

  private final AuthorizeAccessGrantQueryHandler authorizeAccessGrantQueryHandler;
  private final GetPersonAccountabilitiesQueryHandler getPersonAccountabilitiesQueryHandler;

  public List<PersonAccountabilityResult> accountabilitiesOf(PartyId personId) {
    return getPersonAccountabilitiesQueryHandler.handle(
        new GetPersonAccountabilitiesQuery(personId));
  }

  /**
   * Fails with {@code AccessDeniedException} unless {@code subject} may invite on {@code node}
   * (needs {@code invitations.send} there) and hold everything {@code position} gives. A position
   * without permissions is only a label and asks for nothing more.
   */
  public void requireMayGive(String subject, PartyId node, PositionResult position) {
    if (position.roleIds().isEmpty() && position.permissions().isEmpty()) {
      return;
    }
    authorizeAccessGrantQueryHandler.handle(
        new AuthorizeAccessGrantQuery(
            subject,
            position.roleIds(),
            position.permissions(),
            node,
            Permission.INVITATIONS_SEND));
  }

  /** Same for every position that gives access which the person holds, wherever they hold it. */
  public void requireMayLink(String subject, List<PersonAccountabilityResult> accountabilities) {
    for (var accountability : accountabilities) {
      if (accountability.position() != null) {
        requireMayGive(subject, accountability.organizationId(), accountability.position());
      }
    }
  }
}
