package org.ouracademy.srtomy.accountability.command.delete_position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQuery;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQueryHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class DeletePositionCommandHandlerImplTests {

  @Mock AccountabilityTypeRepository accountabilityTypeRepository;
  @Mock AccountabilityRepository accountabilityRepository;
  @Mock AuthorizeAccessGrantQueryHandler authorizeAccessGrantQueryHandler;
  @InjectMocks DeletePositionCommandHandlerImpl handler;

  @Test
  void unassignsAndDeletesThePosition() {
    var position =
        new AccountabilityType("Ujier", party(PartyId.generate(), "Iglesia"), Category.POSITION);
    when(accountabilityTypeRepository.findById(position.getId())).thenReturn(Optional.of(position));

    assertThat(handler.handle(new DeletePositionCommand("me", position.getId())).deleted())
        .isTrue();
    verify(accountabilityRepository).deleteAllByType(position);
    verify(accountabilityTypeRepository).delete(position);
  }

  @Test
  void anUnknownPositionIsNotDeleted() {
    var id = AccountabilityTypeId.generate();
    when(accountabilityTypeRepository.findById(id)).thenReturn(Optional.empty());

    assertThat(handler.handle(new DeletePositionCommand("me", id)).deleted()).isFalse();
    verifyNoInteractions(accountabilityRepository);
  }

  @Test
  void deletingAPositionWithAccessIsAuthorizedAgainstItsLevel() {
    var churchId = PartyId.generate();
    var position =
        new AccountabilityType(
            "Pastor", party(churchId, "Iglesia"), Category.POSITION, PredefinedRoles.ADMIN);
    when(accountabilityTypeRepository.findById(position.getId())).thenReturn(Optional.of(position));

    handler.handle(new DeletePositionCommand("me", position.getId()));

    verify(authorizeAccessGrantQueryHandler)
        .handle(new AuthorizeAccessGrantQuery("me", PredefinedRoles.ADMIN, churchId));
    verify(accountabilityTypeRepository).delete(position);
  }

  @Test
  void aCallerWhoCannotGrantTheLevelDeletesNothing() {
    var position =
        new AccountabilityType(
            "Pastor",
            party(PartyId.generate(), "Iglesia"),
            Category.POSITION,
            PredefinedRoles.ADMIN);
    when(accountabilityTypeRepository.findById(position.getId())).thenReturn(Optional.of(position));
    when(authorizeAccessGrantQueryHandler.handle(any())).thenThrow(new AccessDeniedException("no"));

    assertThatThrownBy(() -> handler.handle(new DeletePositionCommand("me", position.getId())))
        .isInstanceOf(AccessDeniedException.class);
    verify(accountabilityTypeRepository, never()).delete(any());
    verifyNoInteractions(accountabilityRepository);
  }
}
