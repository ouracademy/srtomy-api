package org.ouracademy.srtomy.directory.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.query.get_person_accountabilities.GetPersonAccountabilitiesQueryHandler;
import org.ouracademy.srtomy.accountability.query.get_person_accountabilities.PersonAccountabilityResult;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.query.find_accessible_organizations.AccessibleOrganizationResult;
import org.ouracademy.srtomy.iam.query.find_accessible_organizations.FindAccessibleOrganizationsQueryHandler;
import org.ouracademy.srtomy.iam.query.get_effective_permissions.EffectivePermissionsResult;
import org.ouracademy.srtomy.iam.query.get_effective_permissions.GetEffectivePermissionsQueryHandler;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.user.domain.UserId;
import org.ouracademy.srtomy.user.query.UserResult;
import org.ouracademy.srtomy.user.query.find_linked_person.FindLinkedPersonQueryHandler;
import org.ouracademy.srtomy.user.query.find_linked_person.FindLinkedPersonResult;
import org.ouracademy.srtomy.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.springframework.security.access.AccessDeniedException;

class ChurchAccessTests {

  FindAccessibleOrganizationsQueryHandler accessible =
      mock(FindAccessibleOrganizationsQueryHandler.class);
  GetEffectivePermissionsQueryHandler permissions = mock(GetEffectivePermissionsQueryHandler.class);
  FindUserBySubjectQueryHandler findUser = mock(FindUserBySubjectQueryHandler.class);
  FindLinkedPersonQueryHandler findLinkedPerson = mock(FindLinkedPersonQueryHandler.class);
  GetPersonAccountabilitiesQueryHandler accountabilities =
      mock(GetPersonAccountabilitiesQueryHandler.class);
  ChurchAccess access =
      new ChurchAccess(accessible, permissions, findUser, findLinkedPerson, accountabilities);

  PartyId church = PartyId.generate();
  PartyId group = PartyId.generate();

  AccessibleOrganizationResult node(PartyId id, boolean reachable) {
    return new AccessibleOrganizationResult(
        id, "Nodo", Organization.Type.CHURCH, null, reachable, List.of());
  }

  @Test
  void anyNodeOfTheChurchInTheUsersTreeMakesThemAMember() {
    when(accessible.handle(any())).thenReturn(List.of(node(church, false), node(group, true)));

    access.requireMember("me", church);
  }

  @Test
  void aUserWithoutTheChurchInTheirTreeIsDenied() {
    when(accessible.handle(any())).thenReturn(List.of(node(PartyId.generate(), true)));

    assertThatThrownBy(() -> access.requireMember("me", church))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void canReadsTheEffectivePermissionsOnTheNode() {
    when(permissions.handle(any()))
        .thenReturn(new EffectivePermissionsResult(EnumSet.of(Permission.MEMBERS_READ)));

    assertThat(access.can("me", group, Permission.MEMBERS_READ)).isTrue();
    assertThat(access.can("me", group, Permission.MEMBERS_WRITE)).isFalse();
  }

  @Test
  void aPersonWithAnyAccountabilityInTheNodeIsAMemberOfIt() {
    var user = new UserResult(UserId.generate(), "me", "me@x.org", true);
    var person = PartyId.generate();
    when(findUser.handle(any())).thenReturn(Optional.of(user));
    when(findLinkedPerson.handle(any()))
        .thenReturn(Optional.of(new FindLinkedPersonResult(person)));
    when(accountabilities.handle(any()))
        .thenReturn(
            List.of(new PersonAccountabilityResult(group, AccountabilityTypeId.generate(), null)));

    assertThat(access.isMemberOf("me", church, group)).isTrue();
    assertThat(access.isMemberOf("me", church, PartyId.generate())).isFalse();
  }

  @Test
  void withoutALinkedPersonNobodyIsAMember() {
    when(findUser.handle(any()))
        .thenReturn(Optional.of(new UserResult(UserId.generate(), "me", null, false)));
    when(findLinkedPerson.handle(any())).thenReturn(Optional.empty());

    assertThat(access.isMemberOf("me", church, group)).isFalse();
    when(findUser.handle(any())).thenReturn(Optional.empty());
    assertThat(access.isMemberOf("me", church, group)).isFalse();
  }
}
