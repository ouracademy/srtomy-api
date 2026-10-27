package org.ouracademy.ovejafiel.user.query.find_linked_person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.accountability.AccountabilityFixtures;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.domain.UserPersonLink;
import org.ouracademy.ovejafiel.user.repository.UserPersonLinkRepository;

class FindLinkedPersonQueryHandlerImplTests {

  UserPersonLinkRepository repository = mock(UserPersonLinkRepository.class);
  FindLinkedPersonQueryHandlerImpl handler = new FindLinkedPersonQueryHandlerImpl(repository);

  UserId user = UserId.generate();
  PartyId church = PartyId.generate();

  @Test
  void returnsThePersonOfTheLink() {
    var personId = PartyId.generate();
    var link = mock(UserPersonLink.class);
    when(link.getPerson()).thenReturn(AccountabilityFixtures.party(personId, "Ana"));
    when(repository.findByUserIdAndChurchId(user, church)).thenReturn(Optional.of(link));

    assertThat(handler.handle(new FindLinkedPersonQuery(user, church)))
        .map(FindLinkedPersonResult::personId)
        .contains(personId);
  }

  @Test
  void isEmptyWhenTheUserHasNoPersonInTheChurch() {
    when(repository.findByUserIdAndChurchId(user, church)).thenReturn(Optional.empty());

    assertThat(handler.handle(new FindLinkedPersonQuery(user, church))).isEmpty();
  }
}
