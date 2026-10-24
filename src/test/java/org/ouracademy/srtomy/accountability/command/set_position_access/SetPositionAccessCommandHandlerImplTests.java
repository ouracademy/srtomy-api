package org.ouracademy.srtomy.accountability.command.set_position_access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.AccountabilityFixtures.party;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType.Category;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQuery;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQueryHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class SetPositionAccessCommandHandlerImplTests {

  @Mock AccountabilityTypeRepository accountabilityTypeRepository;
  @Mock AuthorizeAccessGrantQueryHandler authorizeAccessGrantQueryHandler;
  @InjectMocks SetPositionAccessCommandHandlerImpl handler;

  PartyId churchId = PartyId.generate();

  AccountabilityType position(Category category, org.ouracademy.srtomy.iam.domain.IamRoleId level) {
    var type = new AccountabilityType("Pastor", party(churchId, "Iglesia"), category, level);
    when(accountabilityTypeRepository.findById(type.getId())).thenReturn(Optional.of(type));
    return type;
  }

  @Test
  void changesTheLevelAfterAuthorizingTheOldAndTheNewOne() {
    var pastor = position(Category.POSITION, PredefinedRoles.LEADER);

    var result =
        handler.handle(new SetPositionAccessCommand("me", pastor.getId(), PredefinedRoles.ADMIN));

    assertThat(result)
        .get()
        .extracting(r -> r.position().accessRoleId())
        .isEqualTo(PredefinedRoles.ADMIN);
    var order = inOrder(authorizeAccessGrantQueryHandler);
    order
        .verify(authorizeAccessGrantQueryHandler)
        .handle(new AuthorizeAccessGrantQuery("me", PredefinedRoles.LEADER, churchId));
    order
        .verify(authorizeAccessGrantQueryHandler)
        .handle(new AuthorizeAccessGrantQuery("me", PredefinedRoles.ADMIN, churchId));
  }

  @Test
  void aNullLevelMakesItAPlainLabel() {
    var usher = position(Category.POSITION, PredefinedRoles.COLLABORATOR);

    var result = handler.handle(new SetPositionAccessCommand("me", usher.getId(), null));

    assertThat(result).get().extracting(r -> r.position().accessRoleId()).isNull();
    assertThat(usher.hasAccess()).isFalse();
  }

  @Test
  void aCallerWhoCannotGrantTheNewLevelChangesNothing() {
    var usher = position(Category.POSITION, null);
    when(authorizeAccessGrantQueryHandler.handle(any())).thenThrow(new AccessDeniedException("no"));

    assertThatThrownBy(
            () ->
                handler.handle(
                    new SetPositionAccessCommand("me", usher.getId(), PredefinedRoles.ADMIN)))
        .isInstanceOf(AccessDeniedException.class);
    assertThat(usher.hasAccess()).isFalse();
  }

  @Test
  void aUnknownPositionIsEmpty() {
    var id = AccountabilityTypeId.generate();
    when(accountabilityTypeRepository.findById(id)).thenReturn(Optional.empty());

    assertThat(handler.handle(new SetPositionAccessCommand("me", id, PredefinedRoles.ADMIN)))
        .isEmpty();
    verifyNoInteractions(authorizeAccessGrantQueryHandler);
  }

  @Test
  void aTypeThatIsNotAPositionIsEmpty() {
    var other = position(Category.MEMBER_PROGRESS_STATE, null);

    assertThat(handler.handle(new SetPositionAccessCommand("me", other.getId(), null))).isEmpty();
    verify(accountabilityTypeRepository).findById(other.getId());
  }
}
