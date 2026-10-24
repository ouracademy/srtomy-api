package org.ouracademy.srtomy.accountability.command.delete_position;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class DeletePositionCommandHandlerImplTests {

  @Mock AccountabilityTypeRepository accountabilityTypeRepository;
  @Mock AccountabilityRepository accountabilityRepository;
  @InjectMocks DeletePositionCommandHandlerImpl handler;

  @Test
  void unassignsAndDeletesThePosition() {
    var position =
        new AccountabilityType("Ujier", party(PartyId.generate(), "Iglesia"), Category.POSITION);
    when(accountabilityTypeRepository.findById(position.getId())).thenReturn(Optional.of(position));

    assertThat(handler.handle(new DeletePositionCommand(position.getId())).deleted()).isTrue();
    verify(accountabilityRepository).deleteAllByType(position);
    verify(accountabilityTypeRepository).delete(position);
  }

  @Test
  void anUnknownPositionIsNotDeleted() {
    var id = AccountabilityTypeId.generate();
    when(accountabilityTypeRepository.findById(id)).thenReturn(Optional.empty());

    assertThat(handler.handle(new DeletePositionCommand(id)).deleted()).isFalse();
    verifyNoInteractions(accountabilityRepository);
  }
}
