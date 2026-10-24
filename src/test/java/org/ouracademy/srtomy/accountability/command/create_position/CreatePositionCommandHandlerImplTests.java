package org.ouracademy.srtomy.accountability.command.create_position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.AccountabilityFixtures.party;

import jakarta.persistence.EntityManager;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.command.PositionNames;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQuery;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQueryHandler;
import org.ouracademy.srtomy.party.domain.Party;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CreatePositionCommandHandlerImplTests {

  @Mock AccountabilityTypeRepository accountabilityTypeRepository;
  @Mock PositionNames positionNames;
  @Mock AuthorizeAccessGrantQueryHandler authorizeAccessGrantQueryHandler;
  @Mock EntityManager entityManager;
  @InjectMocks CreatePositionCommandHandlerImpl handler;

  @Test
  void savesAPositionOwnedByTheChurch() {
    var churchId = PartyId.generate();
    when(positionNames.requireAvailable(churchId, " Diácono ")).thenReturn("Diácono");
    when(entityManager.getReference(Party.class, churchId)).thenReturn(party(churchId, "Iglesia"));
    when(accountabilityTypeRepository.save(any(AccountabilityType.class)))
        .thenAnswer(call -> call.getArgument(0));

    var result =
        handler.handle(new CreatePositionCommand("me", churchId, " Diácono ", Set.of(), Set.of()));

    assertThat(result.position().name()).isEqualTo("Diácono");
    assertThat(result.position().churchId()).isEqualTo(churchId);
  }

  @Test
  void aPositionWithAccessIsAuthorizedBeforeBeingCreated() {
    var churchId = PartyId.generate();
    when(positionNames.requireAvailable(churchId, "Pastor")).thenReturn("Pastor");
    when(entityManager.getReference(Party.class, churchId)).thenReturn(party(churchId, "Iglesia"));
    when(accountabilityTypeRepository.save(any(AccountabilityType.class)))
        .thenAnswer(call -> call.getArgument(0));

    var result =
        handler.handle(
            new CreatePositionCommand(
                "me", churchId, "Pastor", Set.of(PredefinedRoles.ADMIN), Set.of("events.write")));

    assertThat(result.position().roleIds()).containsExactly(PredefinedRoles.ADMIN);
    assertThat(result.position().permissions()).containsExactly("events.write");
    verify(authorizeAccessGrantQueryHandler)
        .handle(
            new AuthorizeAccessGrantQuery(
                "me", Set.of(PredefinedRoles.ADMIN), Set.of("events.write"), churchId));
  }

  @Test
  void aCallerWhoCannotGrantTheLevelCreatesNothing() {
    var churchId = PartyId.generate();
    when(authorizeAccessGrantQueryHandler.handle(any())).thenThrow(new AccessDeniedException("no"));

    assertThatThrownBy(
            () ->
                handler.handle(
                    new CreatePositionCommand(
                        "me",
                        churchId,
                        "Pastor",
                        Set.of(PredefinedRoles.ADMIN),
                        Set.of("events.write"))))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(accountabilityTypeRepository);
  }

  @Test
  void aLabelNeedsNoAuthorization() {
    var churchId = PartyId.generate();
    when(positionNames.requireAvailable(churchId, "Hermano")).thenReturn("Hermano");
    when(entityManager.getReference(Party.class, churchId)).thenReturn(party(churchId, "Iglesia"));
    when(accountabilityTypeRepository.save(any(AccountabilityType.class)))
        .thenAnswer(call -> call.getArgument(0));

    handler.handle(new CreatePositionCommand("me", churchId, "Hermano", Set.of(), Set.of()));

    verifyNoInteractions(authorizeAccessGrantQueryHandler);
  }
}
