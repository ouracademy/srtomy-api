package org.ouracademy.srtomy.iam.query.authorize_access_grant;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.iam.command.GrantRules;
import org.ouracademy.srtomy.iam.domain.IamRole;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.srtomy.iam.repository.IamRoleRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;
import org.springframework.security.access.AccessDeniedException;

class AuthorizeAccessGrantQueryHandlerImplTests {

  AuthorizeQueryHandler authorizeQueryHandler = mock(AuthorizeQueryHandler.class);
  IamRoleRepository iamRoleRepository = mock(IamRoleRepository.class);
  GrantRules grantRules = mock(GrantRules.class);
  FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler =
      mock(FindChurchOfOrganizationQueryHandler.class);
  AuthorizeAccessGrantQueryHandlerImpl handler =
      new AuthorizeAccessGrantQueryHandlerImpl(
          authorizeQueryHandler,
          iamRoleRepository,
          grantRules,
          findChurchOfOrganizationQueryHandler);

  PartyId church = PartyId.generate();
  PartyId node = PartyId.generate();

  @BeforeEach
  void setUp() {
    when(findChurchOfOrganizationQueryHandler.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
  }

  IamRole predefined(IamRoleId id) {
    var role = mock(IamRole.class);
    when(role.getId()).thenReturn(id);
    when(role.isPredefined()).thenReturn(true);
    when(role.getPermissions()).thenReturn(List.of("org.read"));
    when(iamRoleRepository.findById(id)).thenReturn(Optional.of(role));
    return role;
  }

  @Test
  void passesWhenTheCallerManagesAccessAndHoldsTheLevel() {
    predefined(PredefinedRoles.LEADER);

    var result = handler.handle(new AuthorizeAccessGrantQuery("me", PredefinedRoles.LEADER, node));

    assertThat(result.roleId()).isEqualTo(PredefinedRoles.LEADER);
    verify(authorizeQueryHandler).handle(new AuthorizeQuery("me", node, Permission.IAM_MANAGE));
    verify(grantRules).requireCanGrant("me", PredefinedRoles.LEADER, List.of("org.read"), node);
  }

  @Test
  void withoutIamManageNothingElseIsChecked() {
    doThrow(new AccessDeniedException("no")).when(authorizeQueryHandler).handle(any());

    assertThatThrownBy(
            () -> handler.handle(new AuthorizeAccessGrantQuery("me", PredefinedRoles.LEADER, node)))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(iamRoleRepository, grantRules);
  }

  @Test
  void ownerIsNeverAPositionLevel() {
    predefined(PredefinedRoles.OWNER);

    assertThatThrownBy(
            () -> handler.handle(new AuthorizeAccessGrantQuery("me", PredefinedRoles.OWNER, node)))
        .isInstanceOf(DomainValidationException.class);
    verifyNoInteractions(grantRules);
  }

  @Test
  void anUnknownLevelIsNotFound() {
    var id = IamRoleId.generate();
    when(iamRoleRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(new AuthorizeAccessGrantQuery("me", id, node)))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void aCustomLevelOfAnotherChurchIsRejected() {
    var foreign = mock(IamRole.class);
    var foreignChurch = mock(Organization.class);
    var id = IamRoleId.generate();
    when(foreign.getId()).thenReturn(id);
    when(foreign.isPredefined()).thenReturn(false);
    when(foreign.getChurch()).thenReturn(foreignChurch);
    when(foreignChurch.getId()).thenReturn(PartyId.generate());
    when(iamRoleRepository.findById(id)).thenReturn(Optional.of(foreign));

    assertThatThrownBy(() -> handler.handle(new AuthorizeAccessGrantQuery("me", id, node)))
        .isInstanceOf(DomainValidationException.class);
    verifyNoInteractions(grantRules);
  }

  @Test
  void aCustomLevelOfTheSameChurchPasses() {
    var own = mock(IamRole.class);
    var ownChurch = mock(Organization.class);
    var id = IamRoleId.generate();
    when(own.getId()).thenReturn(id);
    when(own.isPredefined()).thenReturn(false);
    when(own.getChurch()).thenReturn(ownChurch);
    when(own.getPermissions()).thenReturn(List.of("org.read"));
    when(ownChurch.getId()).thenReturn(church);
    when(iamRoleRepository.findById(id)).thenReturn(Optional.of(own));

    handler.handle(new AuthorizeAccessGrantQuery("me", id, node));

    verify(grantRules).requireCanGrant("me", id, List.of("org.read"), node);
  }

  @Test
  void aCallerWhoCannotGrantTheLevelIsDenied() {
    predefined(PredefinedRoles.ADMIN);
    doThrow(new AccessDeniedException("no"))
        .when(grantRules)
        .requireCanGrant(any(), any(), any(), any());

    assertThatThrownBy(
            () -> handler.handle(new AuthorizeAccessGrantQuery("me", PredefinedRoles.ADMIN, node)))
        .isInstanceOf(AccessDeniedException.class);
  }
}
