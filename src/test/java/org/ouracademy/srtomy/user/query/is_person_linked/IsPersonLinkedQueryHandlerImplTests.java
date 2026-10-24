package org.ouracademy.srtomy.user.query.is_person_linked;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.user.repository.UserPersonLinkRepository;

class IsPersonLinkedQueryHandlerImplTests {

  UserPersonLinkRepository repository = mock(UserPersonLinkRepository.class);
  IsPersonLinkedQueryHandlerImpl handler = new IsPersonLinkedQueryHandlerImpl(repository);

  @Test
  void reportsWhetherSomeUserRepresentsThePerson() {
    var linked = PartyId.generate();
    var free = PartyId.generate();
    when(repository.existsByPersonId(linked)).thenReturn(true);

    assertThat(handler.handle(new IsPersonLinkedQuery(linked)).linked()).isTrue();
    assertThat(handler.handle(new IsPersonLinkedQuery(free)).linked()).isFalse();
  }
}
