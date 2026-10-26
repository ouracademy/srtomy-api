package org.ouracademy.ovejafiel.accountability.command.delete_accountabilities_by_parent_and_types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.LEADER;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.accountability.domain.Accountability;
import org.ouracademy.ovejafiel.accountability.repository.AccountabilityRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class DeleteAccountabilitiesByParentAndTypesCommandHandlerImplTests {

  @Mock AccountabilityRepository accountabilityRepository;
  @Mock Accountability member;
  @Mock Accountability leader;

  DeleteAccountabilitiesByParentAndTypesCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new DeleteAccountabilitiesByParentAndTypesCommandHandlerImpl(accountabilityRepository);
  }

  @Test
  void handleDeletesExactlyTheAccountabilitiesFoundForTheParentAndTypes() {
    var groupId = PartyId.generate();
    var types = List.of(IS_PART_OF, LEADER);
    var found = List.of(member, leader);
    when(accountabilityRepository.findByParentIdAndTypeIdIn(groupId, types)).thenReturn(found);

    var result = handler.handle(new DeleteAccountabilitiesByParentAndTypesCommand(groupId, types));

    assertThat(result).isEqualTo(new DeleteAccountabilitiesByParentAndTypesResult());
    verify(accountabilityRepository).deleteAll(found);
  }

  @Test
  void handleDeletesNothingWhenNoAccountabilityMatches() {
    var groupId = PartyId.generate();
    var types = List.of(IS_PART_OF);
    when(accountabilityRepository.findByParentIdAndTypeIdIn(groupId, types)).thenReturn(List.of());

    handler.handle(new DeleteAccountabilitiesByParentAndTypesCommand(groupId, types));

    verify(accountabilityRepository).deleteAll(List.of());
  }
}
