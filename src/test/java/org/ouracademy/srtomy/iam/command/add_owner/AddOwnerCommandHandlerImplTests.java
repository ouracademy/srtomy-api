package org.ouracademy.srtomy.iam.command.add_owner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
class AddOwnerCommandHandlerImplTests {

  @Mock IamRoleRepository iamRoleRepository;
  @Mock GrantRules grantRules;
  @Mock GrantRoleCommandHandler grantRoleCommandHandler;
  @Mock FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  @Mock FindUserByEmailQueryHandler findUserByEmailQueryHandler;
  @Mock FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;
  @InjectMocks AddOwnerCommandHandlerImpl handler;

  PartyId church = PartyId.generate();
  UserResult caller = new UserResult(UserId.generate(), "me", "me@x.org", true);
  UserResult target = new UserResult(UserId.generate(), "you", "you@x.org", true);
  IamRole owner = mock(IamRole.class);

  @BeforeEach
  void setUp() {
    lenient()
        .when(findChurchOfOrganizationQueryHandler.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
  }

  AddOwnerCommand command() {
    return new AddOwnerCommand("me", "you@x.org", church);
  }

  void ownerRoleExists() {
    when(owner.getId()).thenReturn(PredefinedRoles.OWNER);
    when(owner.getPermissions()).thenReturn(List.of("org.read"));
    when(iamRoleRepository.findById(PredefinedRoles.OWNER)).thenReturn(Optional.of(owner));
  }

  @Test
  void makesTheUserWithThatEmailAnOwnerOfTheChurch() {
    ownerRoleExists();
    when(findUserBySubjectQueryHandler.handle(any())).thenReturn(Optional.of(caller));
    when(findUserByEmailQueryHandler.handle(any())).thenReturn(Optional.of(target));
    when(grantRoleCommandHandler.handle(any())).thenReturn(new GrantRoleResult(true));

    assertThat(handler.handle(command()).created()).isTrue();

    verify(grantRules).requireCanGrant("me", PredefinedRoles.OWNER, List.of("org.read"), church);
    verify(grantRoleCommandHandler)
        .handle(new GrantRoleCommand(target.id(), PredefinedRoles.OWNER, church, caller.id()));
  }

  @Test
  void aCallerThatCannotGrantOwnerIsDenied() {
    ownerRoleExists();
    doThrow(new AccessDeniedException("no"))
        .when(grantRules)
        .requireCanGrant(any(), any(), any(), any());

    assertThatThrownBy(() -> handler.handle(command())).isInstanceOf(AccessDeniedException.class);
    verify(grantRoleCommandHandler, never()).handle(any());
  }

  @Test
  void aMissingEmailIsAValidationError() {
    ownerRoleExists();
    when(findUserBySubjectQueryHandler.handle(any())).thenReturn(Optional.of(caller));
    when(findUserByEmailQueryHandler.handle(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(command()))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void anUnregisteredCallerIsDenied() {
    ownerRoleExists();
    when(findUserBySubjectQueryHandler.handle(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(command())).isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void anUnknownOrganizationIsNotFound() {
    when(findChurchOfOrganizationQueryHandler.handle(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(command())).isInstanceOf(NoSuchElementException.class);
  }
}
