package org.ouracademy.srtomy.iam.command.grant_access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.iam.command.GrantRules;
import org.ouracademy.srtomy.iam.command.grant_role.GrantRoleCommand;
import org.ouracademy.srtomy.iam.command.grant_role.GrantRoleCommandHandler;
import org.ouracademy.srtomy.iam.command.grant_role.GrantRoleResult;
import org.ouracademy.srtomy.iam.domain.IamRole;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.iam.repository.IamRoleRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;
import org.ouracademy.srtomy.user.domain.UserId;
import org.ouracademy.srtomy.user.query.UserResult;
import org.ouracademy.srtomy.user.query.find_user_by_email.FindUserByEmailQueryHandler;
import org.ouracademy.srtomy.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class GrantAccessCommandHandlerImplTests {

  @Mock IamRoleRepository iamRoleRepository;
  @Mock GrantRules grantRules;
  @Mock GrantRoleCommandHandler grantRoleCommandHandler;
  @Mock FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  @Mock FindUserByEmailQueryHandler findUserByEmailQueryHandler;
  @Mock FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;
  @InjectMocks GrantAccessCommandHandlerImpl handler;

  PartyId church = PartyId.generate();
  PartyId node = PartyId.generate();
  UserResult caller = new UserResult(UserId.generate(), "me", "me@x.org", true);
  UserResult target = new UserResult(UserId.generate(), "you", "you@x.org", true);
  IamRole leader = mock(IamRole.class);

  @BeforeEach
  void setUp() {
    lenient()
        .when(findChurchOfOrganizationQueryHandler.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
  }

  GrantAccessCommand command() {
    return new GrantAccessCommand("me", "you@x.org", PredefinedRoles.LEADER, node);
  }

  @Test
  void grantsTheRoleToTheUserWithThatEmail() {
    when(leader.getId()).thenReturn(PredefinedRoles.LEADER);
    when(leader.isPredefined()).thenReturn(true);
    when(leader.getPermissions()).thenReturn(List.of("org.read"));
    when(iamRoleRepository.findById(PredefinedRoles.LEADER)).thenReturn(Optional.of(leader));
    when(findUserBySubjectQueryHandler.handle(any())).thenReturn(Optional.of(caller));
    when(findUserByEmailQueryHandler.handle(any())).thenReturn(Optional.of(target));
    when(grantRoleCommandHandler.handle(any())).thenReturn(new GrantRoleResult(true));

    assertThat(handler.handle(command()).created()).isTrue();

    verify(grantRules).requireCanGrant("me", PredefinedRoles.LEADER, List.of("org.read"), node);
    verify(grantRoleCommandHandler)
        .handle(new GrantRoleCommand(target.id(), PredefinedRoles.LEADER, node, caller.id()));
  }

  @Test
  void anUnknownRoleIsNotFound() {
    when(iamRoleRepository.findById(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(command())).isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void aCustomRoleOfAnotherChurchIsRejected() {
    var foreign = mock(IamRole.class);
    var foreignChurch = mock(Organization.class);
    when(foreignChurch.getId()).thenReturn(PartyId.generate());
    when(foreign.isPredefined()).thenReturn(false);
    when(foreign.getChurch()).thenReturn(foreignChurch);
    when(iamRoleRepository.findById(any())).thenReturn(Optional.of(foreign));

    assertThatThrownBy(() -> handler.handle(command()))
        .isInstanceOf(DomainValidationException.class);
    verify(grantRoleCommandHandler, never()).handle(any());
  }

  @Test
  void aMissingEmailIsAValidationError() {
    when(leader.getId()).thenReturn(PredefinedRoles.LEADER);
    when(leader.isPredefined()).thenReturn(true);
    when(iamRoleRepository.findById(any())).thenReturn(Optional.of(leader));
    when(findUserBySubjectQueryHandler.handle(any())).thenReturn(Optional.of(caller));
    when(findUserByEmailQueryHandler.handle(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(command()))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void anUnregisteredCallerIsDenied() {
    when(leader.getId()).thenReturn(PredefinedRoles.LEADER);
    when(leader.isPredefined()).thenReturn(true);
    when(iamRoleRepository.findById(any())).thenReturn(Optional.of(leader));
    when(findUserBySubjectQueryHandler.handle(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(command())).isInstanceOf(AccessDeniedException.class);
  }
}
