package org.ouracademy.srtomy.invitation.command;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.query.PositionResult;
import org.ouracademy.srtomy.accountability.query.get_person_accountabilities.GetPersonAccountabilitiesQueryHandler;
import org.ouracademy.srtomy.accountability.query.get_person_accountabilities.PersonAccountabilityResult;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQuery;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQueryHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.security.access.AccessDeniedException;

class InvitationRulesTests {

  AuthorizeAccessGrantQueryHandler authorize = mock(AuthorizeAccessGrantQueryHandler.class);
  InvitationRules rules =
      new InvitationRules(authorize, mock(GetPersonAccountabilitiesQueryHandler.class));

  PartyId church = PartyId.generate();
  PartyId node = PartyId.generate();

  PositionResult position(Set<String> permissions) {
    return new PositionResult(
        AccountabilityTypeId.generate(), "Cargo", church, Set.of(), permissions);
  }

  @Test
  void aLabelNeedsNothingMore() {
    rules.requireMayGive("me", node, position(Set.of()));

    verifyNoInteractions(authorize);
  }

  @Test
  void aPositionWithAccessIsCheckedAgainstTheInvitationPermission() {
    var withRole =
        new PositionResult(
            AccountabilityTypeId.generate(),
            "Cargo",
            church,
            Set.of(PredefinedRoles.LEADER),
            Set.of());

    rules.requireMayGive("me", node, withRole);

    verify(authorize)
        .handle(
            new AuthorizeAccessGrantQuery(
                "me", Set.of(PredefinedRoles.LEADER), Set.of(), node, Permission.INVITATIONS_SEND));
  }

  @Test
  void linkingChecksEachPositionWhereItIsHeld() {
    var admin = position(Set.of("iam.manage"));
    var memberOnly = new PersonAccountabilityResult(node, AccountabilityTypeId.generate(), null);
    var holds = new PersonAccountabilityResult(church, admin.id(), admin);

    rules.requireMayLink("me", List.of(memberOnly, holds));

    verify(authorize)
        .handle(
            new AuthorizeAccessGrantQuery(
                "me", Set.of(), Set.of("iam.manage"), church, Permission.INVITATIONS_SEND));
  }

  @Test
  void linkingIsDeniedWhenTheCallerCannotGiveWhatThePersonHolds() {
    var admin = position(Set.of("iam.manage"));
    doThrow(new AccessDeniedException("no")).when(authorize).handle(any());

    assertThatThrownBy(
            () ->
                rules.requireMayLink(
                    "me", List.of(new PersonAccountabilityResult(church, admin.id(), admin))))
        .isInstanceOf(AccessDeniedException.class);
  }
}
