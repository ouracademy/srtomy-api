package org.ouracademy.ovejafiel.accountability.command.end_accountabilities_of_child;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class EndAccountabilitiesOfChildCommandHandlerImplTests {

  @Mock AccountabilityRepository accountabilityRepository;

  @Test
  void handleClosesTheAccountabilitiesInsteadOfDeletingThem() {
    var handler = new EndAccountabilitiesOfChildCommandHandlerImpl(accountabilityRepository);
    var child = PartyId.generate();
    var parents = List.of(PartyId.generate(), PartyId.generate());
    when(accountabilityRepository.endByChildIdAndParentIdIn(
            eq(child), eq(parents), any(LocalDateTime.class)))
        .thenReturn(3);

    var result = handler.handle(new EndAccountabilitiesOfChildCommand(child, parents));

    assertThat(result.ended()).isEqualTo(3);
    verify(accountabilityRepository)
        .endByChildIdAndParentIdIn(eq(child), eq(parents), any(LocalDateTime.class));
  }

  @Test
  void handleWithoutParentsDoesNothing() {
    var handler = new EndAccountabilitiesOfChildCommandHandlerImpl(accountabilityRepository);

    var result =
        handler.handle(new EndAccountabilitiesOfChildCommand(PartyId.generate(), List.of()));

    assertThat(result.ended()).isZero();
    verifyNoInteractions(accountabilityRepository);
  }
}
