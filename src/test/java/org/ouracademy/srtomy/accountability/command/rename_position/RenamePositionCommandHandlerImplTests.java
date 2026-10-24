package org.ouracademy.srtomy.accountability.command.rename_position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.AccountabilityFixtures.party;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.command.PositionNames;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType.Category;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class RenamePositionCommandHandlerImplTests {

  @Mock AccountabilityTypeRepository accountabilityTypeRepository;
  @Mock PositionNames positionNames;
  @InjectMocks RenamePositionCommandHandlerImpl handler;

  @Test
  void renamesAfterCheckingTheNewNameIsFree() {
    var churchId = PartyId.generate();
    var usher = new AccountabilityType("Ujier", party(churchId, "Iglesia"), Category.POSITION);
    when(accountabilityTypeRepository.findById(usher.getId())).thenReturn(Optional.of(usher));
    when(positionNames.requireAvailable(churchId, "Servidor")).thenReturn("Servidor");

    var result = handler.handle(new RenamePositionCommand(usher.getId(), "Servidor"));

    assertThat(result).get().extracting(r -> r.position().name()).isEqualTo("Servidor");
  }

  @Test
  void changingOnlyTheCaseSkipsTheDuplicateCheck() {
    var usher =
        new AccountabilityType("ujier", party(PartyId.generate(), "Iglesia"), Category.POSITION);
    when(accountabilityTypeRepository.findById(usher.getId())).thenReturn(Optional.of(usher));

    handler.handle(new RenamePositionCommand(usher.getId(), "Ujier"));

    assertThat(usher.getName()).isEqualTo("Ujier");
    verifyNoInteractions(positionNames);
  }

  @Test
  void aMemberStateIsNotAPosition() {
    var state = new AccountabilityType("VISITOR", null, Category.MEMBER_PROGRESS_STATE);
    when(accountabilityTypeRepository.findById(state.getId())).thenReturn(Optional.of(state));

    assertThat(handler.handle(new RenamePositionCommand(state.getId(), "X"))).isEmpty();
    assertThat(handler.handle(new RenamePositionCommand(AccountabilityTypeId.generate(), "X")))
        .isEmpty();
  }
}
