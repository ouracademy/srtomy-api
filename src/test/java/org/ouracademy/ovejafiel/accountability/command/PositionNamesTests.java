package org.ouracademy.ovejafiel.accountability.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType.Category;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityTypeRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

@ExtendWith(MockitoExtension.class)
class PositionNamesTests {

  @Mock AccountabilityTypeRepository accountabilityTypeRepository;
  @InjectMocks PositionNames positionNames;

  @Test
  void anyFreeWordingIsAcceptedTrimmed() {
    var church = PartyId.generate();

    assertThat(positionNames.requireAvailable(church, "  Hermano servidor "))
        .isEqualTo("Hermano servidor");
  }

  @Test
  void blankAndTakenNamesAreRejected() {
    var church = PartyId.generate();
    when(accountabilityTypeRepository.existsByPartyIdAndCategoryAndNameIgnoreCase(
            church, Category.POSITION, "Pastor"))
        .thenReturn(true);

    assertThatThrownBy(() -> positionNames.requireAvailable(church, " "))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> positionNames.requireAvailable(church, "Pastor"))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("Pastor");
  }
}
