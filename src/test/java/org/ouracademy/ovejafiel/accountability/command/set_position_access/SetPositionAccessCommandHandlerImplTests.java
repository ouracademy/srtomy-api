package org.ouracademy.ovejafiel.accountability.command.set_position_access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.accountability.AccountabilityFixtures.party;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType.Category;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.domain.PredefinedRoles;
import org.ouracademy.ovejafiel.iam.query.authorize_access_grant.AuthorizeAccessGrantQuery;
import org.ouracademy.ovejafiel.iam.query.authorize_access_grant.AuthorizeAccessGrantQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class SetPositionAccessCommandHandlerImplTests {

  @Mock AccountabilityTypeRepository accountabilityTypeRepository;
  @Mock AuthorizeAccessGrantQueryHandler authorizeAccessGrantQueryHandler;
  @InjectMocks SetPositionAccessCommandHandlerImpl handler;

  PartyId churchId = PartyId.generate();

  AccountabilityType position(Category category, Set<IamRoleId> roleIds, Set<String> permissions) {
    var type =
        new AccountabilityType(
            "Pastor", party(churchId, "Iglesia"), category, roleIds, permissions);
    when(accountabilityTypeRepository.findById(type.getId())).thenReturn(Optional.of(type));
    return type;
  }

  @Test
  void replacesWhatItGivesAfterAuthorizingTheOldAndTheNew() {
    var pastor = position(Category.POSITION, Set.of(PredefinedRoles.LEADER), Set.of());

    var result =
        handler.handle(
            new SetPositionAccessCommand(
                "me", pastor.getId(), Set.of(PredefinedRoles.ADMIN), Set.of("events.write")));

    assertThat(result)
        .get()
        .extracting(r -> r.position().roleIds())
        .isEqualTo(Set.of(PredefinedRoles.ADMIN));
    assertThat(pastor.getPermissions()).containsExactly("events.write");
    var order = inOrder(authorizeAccessGrantQueryHandler);
    order
        .verify(authorizeAccessGrantQueryHandler)
        .handle(
            new AuthorizeAccessGrantQuery(
                "me", Set.of(PredefinedRoles.LEADER), Set.of(), churchId));
    order
        .verify(authorizeAccessGrantQueryHandler)
        .handle(
            new AuthorizeAccessGrantQuery(
                "me", Set.of(PredefinedRoles.ADMIN), Set.of("events.write"), churchId));
  }

  @Test
  void noRolesAndNoPermissionsMakeItAPlainLabel() {
    var usher =
        position(Category.POSITION, Set.of(PredefinedRoles.COLLABORATOR), Set.of("members.read"));

    var result =
        handler.handle(new SetPositionAccessCommand("me", usher.getId(), Set.of(), Set.of()));

    assertThat(result).get().extracting(r -> r.position().permissions()).isEqualTo(Set.of());
    assertThat(usher.hasAccess()).isFalse();
  }

  @Test
  void aCallerWhoCannotGrantTheNewPermissionsChangesNothing() {
    var usher = position(Category.POSITION, Set.of(), Set.of());
    when(authorizeAccessGrantQueryHandler.handle(any())).thenThrow(new AccessDeniedException("no"));

    assertThatThrownBy(
            () ->
                handler.handle(
                    new SetPositionAccessCommand(
                        "me", usher.getId(), Set.of(PredefinedRoles.ADMIN), Set.of())))
        .isInstanceOf(AccessDeniedException.class);
    assertThat(usher.hasAccess()).isFalse();
  }

  @Test
  void aPositionWithoutAccessNeedsNoAuthorizationToStayALabel() {
    var label = position(Category.POSITION, Set.of(), Set.of());

    handler.handle(new SetPositionAccessCommand("me", label.getId(), Set.of(), Set.of()));

    verifyNoInteractions(authorizeAccessGrantQueryHandler);
  }

  @Test
  void aUnknownPositionIsEmpty() {
    var id = AccountabilityTypeId.generate();
    when(accountabilityTypeRepository.findById(id)).thenReturn(Optional.empty());

    assertThat(
            handler.handle(
                new SetPositionAccessCommand("me", id, Set.of(PredefinedRoles.ADMIN), Set.of())))
        .isEmpty();
    verifyNoInteractions(authorizeAccessGrantQueryHandler);
  }

  @Test
  void aTypeThatIsNotAPositionIsEmpty() {
    var other = position(Category.MEMBER_PROGRESS_STATE, Set.of(), Set.of());

    assertThat(
            handler.handle(new SetPositionAccessCommand("me", other.getId(), Set.of(), Set.of())))
        .isEmpty();
  }
}
