package org.ouracademy.ovejafiel.iam.query.authorize_access_grant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.iam.command.GrantRules;
import org.ouracademy.ovejafiel.iam.domain.IamRole;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.domain.PredefinedRoles;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.iam.query.expand_permissions.ExpandPermissionsQueryHandler;
import org.ouracademy.ovejafiel.iam.query.expand_permissions.ExpandPermissionsResult;
import org.ouracademy.ovejafiel.iam.repository.IamRoleRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.security.access.AccessDeniedException;

class AuthorizeAccessGrantQueryHandlerImplTests {

  AuthorizeQueryHandler authorizeQueryHandler = mock(AuthorizeQueryHandler.class);
  IamRoleRepository iamRoleRepository = mock(IamRoleRepository.class);
  GrantRules grantRules = mock(GrantRules.class);
  ExpandPermissionsQueryHandler expandPermissionsQueryHandler =
      mock(ExpandPermissionsQueryHandler.class);
  FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler =
      mock(FindChurchOfOrganizationQueryHandler.class);
  AuthorizeAccessGrantQueryHandlerImpl handler =
      new AuthorizeAccessGrantQueryHandlerImpl(
          authorizeQueryHandler,
          iamRoleRepository,
          grantRules,
          expandPermissionsQueryHandler,
          findChurchOfOrganizationQueryHandler);

  PartyId church = PartyId.generate();
  PartyId node = PartyId.generate();

  @BeforeEach
  void setUp() {
    when(findChurchOfOrganizationQueryHandler.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    when(expandPermissionsQueryHandler.handle(any()))
        .thenReturn(new ExpandPermissionsResult(List.of("events.write", "org.read")));
  }

  IamRole predefined(IamRoleId id) {
    var role = mock(IamRole.class);
    when(role.getId()).thenReturn(id);
    when(role.isPredefined()).thenReturn(true);
    when(iamRoleRepository.findById(id)).thenReturn(Optional.of(role));
    return role;
  }

  AuthorizeAccessGrantQuery query(Set<IamRoleId> roles, Set<String> permissions) {
    return new AuthorizeAccessGrantQuery("me", roles, permissions, node);
  }

  @Test
  void passesWhenTheCallerManagesAccessAndHoldsEverythingEffective() {
    predefined(PredefinedRoles.LEADER);

    var result = handler.handle(query(Set.of(PredefinedRoles.LEADER), Set.of("events.write")));

    assertThat(result.permissions()).containsExactly("events.write", "org.read");
    verify(authorizeQueryHandler).handle(new AuthorizeQuery("me", node, Permission.IAM_MANAGE));
    verify(grantRules).requireHoldsAll("me", List.of("events.write", "org.read"), node);
  }

  @Test
  void anInvitationOnlyNeedsTheInvitationPermissionButStillHoldsEverything() {
    handler.handle(
        new AuthorizeAccessGrantQuery(
            "me", Set.of(), Set.of("events.write"), node, Permission.INVITATIONS_SEND));

    verify(authorizeQueryHandler)
        .handle(new AuthorizeQuery("me", node, Permission.INVITATIONS_SEND));
    verify(grantRules).requireHoldsAll("me", List.of("events.write", "org.read"), node);
  }

  @Test
  void loosePermissionsAloneAreEnough() {
    handler.handle(query(Set.of(), Set.of("events.write")));

    verify(grantRules).requireHoldsAll("me", List.of("events.write", "org.read"), node);
  }

  @Test
  void withoutIamManageNothingElseIsChecked() {
    doThrow(new AccessDeniedException("no")).when(authorizeQueryHandler).handle(any());

    assertThatThrownBy(() -> handler.handle(query(Set.of(PredefinedRoles.LEADER), Set.of())))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(iamRoleRepository, grantRules);
  }

  @Test
  void anUnknownPermissionIsRejected() {
    assertThatThrownBy(() -> handler.handle(query(Set.of(), Set.of("org.fly"))))
        .isInstanceOf(DomainValidationException.class);
    verifyNoInteractions(grantRules);
  }

  @Test
  void ownerIsNeverGivenByAPosition() {
    predefined(PredefinedRoles.OWNER);

    assertThatThrownBy(() -> handler.handle(query(Set.of(PredefinedRoles.OWNER), Set.of())))
        .isInstanceOf(DomainValidationException.class);
    verifyNoInteractions(grantRules);
  }

  @Test
  void anUnknownRoleIsNotFound() {
    var id = IamRoleId.generate();
    when(iamRoleRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(query(Set.of(id), Set.of())))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void aCustomRoleOfAnotherChurchIsRejected() {
    var foreign = mock(IamRole.class);
    var foreignChurch = mock(Organization.class);
    var id = IamRoleId.generate();
    when(foreign.getId()).thenReturn(id);
    when(foreign.isPredefined()).thenReturn(false);
    when(foreign.getChurch()).thenReturn(foreignChurch);
    when(foreignChurch.getId()).thenReturn(PartyId.generate());
    when(iamRoleRepository.findById(id)).thenReturn(Optional.of(foreign));

    assertThatThrownBy(() -> handler.handle(query(Set.of(id), Set.of())))
        .isInstanceOf(DomainValidationException.class);
    verifyNoInteractions(grantRules);
  }

  @Test
  void aCustomRoleOfTheSameChurchPasses() {
    var own = mock(IamRole.class);
    var ownChurch = mock(Organization.class);
    var id = IamRoleId.generate();
    when(own.getId()).thenReturn(id);
    when(own.isPredefined()).thenReturn(false);
    when(own.getChurch()).thenReturn(ownChurch);
    when(ownChurch.getId()).thenReturn(church);
    when(iamRoleRepository.findById(id)).thenReturn(Optional.of(own));

    handler.handle(query(Set.of(id), Set.of()));

    verify(grantRules).requireHoldsAll("me", List.of("events.write", "org.read"), node);
  }

  @Test
  void aCallerWhoLacksSomeEffectivePermissionIsDenied() {
    doThrow(new AccessDeniedException("no")).when(grantRules).requireHoldsAll(any(), any(), any());

    assertThatThrownBy(() -> handler.handle(query(Set.of(), Set.of("events.write"))))
        .isInstanceOf(AccessDeniedException.class);
  }
}
