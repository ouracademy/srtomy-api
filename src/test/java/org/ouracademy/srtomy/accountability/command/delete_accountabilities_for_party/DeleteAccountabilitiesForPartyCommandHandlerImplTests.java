package org.ouracademy.srtomy.accountability.command.delete_accountabilities_for_party;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.repository.AccountabilityRepository;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class DeleteAccountabilitiesForPartyCommandHandlerImplTests {

  @Mock AccountabilityRepository accountabilityRepository;

  @Test
  void handleDeletesTheAccountabilitiesWhereThePartyIsParentAndWhereItIsChild() {
    var partyId = PartyId.generate();
    var handler = new DeleteAccountabilitiesForPartyCommandHandlerImpl(accountabilityRepository);

    var result = handler.handle(new DeleteAccountabilitiesForPartyCommand(partyId));

    assertThat(result).isEqualTo(new DeleteAccountabilitiesForPartyResult());
    verify(accountabilityRepository).deleteAllByParentId(partyId);
    verify(accountabilityRepository).deleteAllByChildId(partyId);
  }
}
