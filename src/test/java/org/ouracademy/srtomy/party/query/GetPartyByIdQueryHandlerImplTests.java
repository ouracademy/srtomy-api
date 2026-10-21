package org.ouracademy.srtomy.party.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.party.domain.Party;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.party.repository.PartyRepository;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GetPartyByIdQueryHandlerImplTests {

  @Mock PartyRepository partyRepository;

  GetPartyByIdQueryHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler = new GetPartyByIdQueryHandlerImpl(partyRepository);
  }

  static class TestParty extends Party {}

  @Test
  void handleReturnsTheIdAndNameOfAnExistingParty() {
    var id = PartyId.generate();
    var party = new TestParty();
    ReflectionTestUtils.setField(party, "id", id);
    party.setName("Iglesia Central");
    when(partyRepository.findById(id)).thenReturn(Optional.of(party));

    var result = handler.handle(new GetPartyByIdQuery(id));

    assertThat(result).contains(new GetPartyByIdResult(id, "Iglesia Central"));
  }

  @Test
  void handleReturnsEmptyWhenThePartyDoesNotExist() {
    var id = PartyId.generate();
    when(partyRepository.findById(id)).thenReturn(Optional.empty());

    assertThat(handler.handle(new GetPartyByIdQuery(id))).isEmpty();
  }
}
