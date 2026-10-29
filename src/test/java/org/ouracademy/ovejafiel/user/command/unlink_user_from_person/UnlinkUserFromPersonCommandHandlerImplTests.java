package org.ouracademy.ovejafiel.user.command.unlink_user_from_person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.repository.UserPersonLinkRepository;

@ExtendWith(MockitoExtension.class)
class UnlinkUserFromPersonCommandHandlerImplTests {

  @Mock UserPersonLinkRepository userPersonLinkRepository;

  @Test
  void handleSaysWhetherThereWasAUserToUnlink() {
    var handler = new UnlinkUserFromPersonCommandHandlerImpl(userPersonLinkRepository);
    var linked = PartyId.generate();
    var notLinked = PartyId.generate();
    when(userPersonLinkRepository.deleteByPersonId(linked)).thenReturn(1L);
    when(userPersonLinkRepository.deleteByPersonId(notLinked)).thenReturn(0L);

    assertThat(handler.handle(new UnlinkUserFromPersonCommand(linked)).unlinked()).isTrue();
    assertThat(handler.handle(new UnlinkUserFromPersonCommand(notLinked)).unlinked()).isFalse();
  }
}
