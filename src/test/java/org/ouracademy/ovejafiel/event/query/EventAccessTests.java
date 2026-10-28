package org.ouracademy.ovejafiel.event.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.AccessLevel;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventShare;
import org.ouracademy.ovejafiel.event.domain.GeneralAccess;
import org.ouracademy.ovejafiel.event.domain.ShareRole;
import org.ouracademy.ovejafiel.event.repository.EventShareRepository;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.find_accessible_organizations.AccessibleOrganizationResult;
import org.ouracademy.ovejafiel.iam.query.find_accessible_organizations.FindAccessibleOrganizationsQueryHandler;
import org.ouracademy.ovejafiel.iam.query.get_effective_permissions.EffectivePermissionsResult;
import org.ouracademy.ovejafiel.iam.query.get_effective_permissions.GetEffectivePermissionsQueryHandler;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.query.UserResult;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;

class EventAccessTests {

  GetEffectivePermissionsQueryHandler permissions = mock(GetEffectivePermissionsQueryHandler.class);
  FindAccessibleOrganizationsQueryHandler accessible =
      mock(FindAccessibleOrganizationsQueryHandler.class);
  FindChurchOfOrganizationQueryHandler findChurch =
      mock(FindChurchOfOrganizationQueryHandler.class);
  FindUserBySubjectQueryHandler findUser = mock(FindUserBySubjectQueryHandler.class);
  EventShareRepository shares = mock(EventShareRepository.class);
  EventAccess access = new EventAccess(permissions, accessible, findChurch, findUser, shares);

  PartyId churchId = PartyId.generate();
  Organization node = organization(PartyId.generate(), "Jóvenes", Organization.Type.GROUP);
  Event event = singleEvent(EventId.generate(), "Retiro", node, List.of());

  @BeforeEach
  void setUp() {
    when(permissions.handle(any()))
        .thenReturn(new EffectivePermissionsResult(EnumSet.noneOf(Permission.class)));
    when(findChurch.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(churchId)));
    when(accessible.handle(any())).thenReturn(List.of());
    when(findUser.handle(any())).thenReturn(Optional.empty());
    when(shares.findByEventIdAndEmailIgnoreCase(any(), any())).thenReturn(Optional.empty());
  }

  void permissionsAre(Permission... granted) {
    var set = EnumSet.noneOf(Permission.class);
    set.addAll(Set.of(granted));
    when(permissions.handle(any())).thenReturn(new EffectivePermissionsResult(set));
  }

  void userIs(String email, boolean verified) {
    when(findUser.handle(any()))
        .thenReturn(Optional.of(new UserResult(UserId.generate(), "me", email, verified)));
  }

  void inChurch() {
    when(accessible.handle(any()))
        .thenReturn(
            List.of(
                new AccessibleOrganizationResult(
                    churchId, "Iglesia", Organization.Type.CHURCH, null, false, List.of())));
  }

  @Test
  void someoneNotSignedInGetsNothingFromThePermissions() {
    event.shareVia(GeneralAccess.CHURCH);

    assertThat(access.levelOn(null, event)).isEqualTo(AccessLevel.NONE);
  }

  @Test
  void theTreeGivesViewingOrManaging() {
    assertThat(access.levelOn("me", event)).isEqualTo(AccessLevel.NONE);
    permissionsAre(Permission.EVENTS_READ);
    assertThat(access.levelOn("me", event)).isEqualTo(AccessLevel.VIEWER);
    permissionsAre(Permission.EVENTS_READ, Permission.EVENTS_WRITE);
    assertThat(access.levelOn("me", event)).isEqualTo(AccessLevel.MANAGER);
  }

  @Test
  void aShareGivesItsRoleToTheVerifiedEmailOnly() {
    userIs("ana@x.org", true);
    when(shares.findByEventIdAndEmailIgnoreCase(event.getId(), "ana@x.org"))
        .thenReturn(Optional.of(new EventShare(event, "ana@x.org", ShareRole.EDITOR, null)));
    assertThat(access.levelOn("me", event)).isEqualTo(AccessLevel.EDITOR);

    userIs("ana@x.org", false);
    assertThat(access.levelOn("me", event)).isEqualTo(AccessLevel.NONE);
  }

  @Test
  void theHighestOfTheTreeAndTheShareWins() {
    permissionsAre(Permission.EVENTS_READ);
    userIs("ana@x.org", true);
    when(shares.findByEventIdAndEmailIgnoreCase(any(), any()))
        .thenReturn(Optional.of(new EventShare(event, "ana@x.org", ShareRole.EDITOR, null)));

    assertThat(access.levelOn("me", event)).isEqualTo(AccessLevel.EDITOR);
  }

  @Test
  void aChurchEventIsViewedByWhoBelongsToTheChurch() {
    event.shareVia(GeneralAccess.CHURCH);
    assertThat(access.levelOn("me", event)).isEqualTo(AccessLevel.NONE);

    inChurch();
    assertThat(access.levelOn("me", event)).isEqualTo(AccessLevel.VIEWER);
  }

  @Test
  void aRestrictedEventIgnoresTheChurch() {
    inChurch();

    assertThat(access.levelOn("me", event)).isEqualTo(AccessLevel.NONE);
  }

  @Test
  void anyoneWithTheLinkCanOpenTheSharedPage() {
    event.shareVia(GeneralAccess.ANYONE_WITH_LINK);

    assertThat(access.canOpen(null, event)).isTrue();
    assertThat(access.canOpen("me", event)).isTrue();
  }

  @Test
  void aChurchEventOpensOnlyForTheChurch() {
    event.shareVia(GeneralAccess.CHURCH);

    assertThat(access.canOpen(null, event)).isFalse();
    assertThat(access.canOpen("me", event)).isFalse();
    inChurch();
    assertThat(access.canOpen("me", event)).isTrue();
  }

  @Test
  void aRestrictedEventOpensOnlyForWhoAlreadyHasAccess() {
    assertThat(access.canOpen("me", event)).isFalse();
  }

  @Test
  void onlyAVerifiedUserWithAnEmailCountsAsOne() {
    userIs("ana@x.org", true);
    assertThat(access.verifiedUser("me").email()).isEqualTo("ana@x.org");
    userIs("ana@x.org", false);
    assertThat(access.verifiedUser("me")).isNull();
    userIs(null, true);
    assertThat(access.verifiedUser("me")).isNull();
    assertThat(access.verifiedUser(null)).isNull();
  }
}
