package org.ouracademy.srtomy.accountability.command.create_position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.AccountabilityFixtures.party;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.command.PositionNames;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.party.domain.Party;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class CreatePositionCommandHandlerImplTests {

  @Mock AccountabilityTypeRepository accountabilityTypeRepository;
  @Mock PositionNames positionNames;
  @Mock EntityManager entityManager;
  @InjectMocks CreatePositionCommandHandlerImpl handler;

  @Test
  void savesAPositionOwnedByTheChurch() {
    var churchId = PartyId.generate();
    when(positionNames.requireAvailable(churchId, " Diácono ")).thenReturn("Diácono");
    when(entityManager.getReference(Party.class, churchId)).thenReturn(party(churchId, "Iglesia"));
    when(accountabilityTypeRepository.save(any(AccountabilityType.class)))
        .thenAnswer(call -> call.getArgument(0));

    var result = handler.handle(new CreatePositionCommand(churchId, " Diácono "));

    assertThat(result.position().name()).isEqualTo("Diácono");
    assertThat(result.position().churchId()).isEqualTo(churchId);
  }
}
