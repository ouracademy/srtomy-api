package org.ouracademy.ovejafiel.person.query.find_people_by_phone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;
import org.ouracademy.ovejafiel.person.repository.PersonRepository.PhoneMatch;

class FindPeopleByPhoneQueryHandlerImplTests {

  PersonRepository repository = mock(PersonRepository.class);
  FindPeopleByPhoneQueryHandlerImpl handler = new FindPeopleByPhoneQueryHandlerImpl(repository);
  PartyId church = PartyId.generate();

  @Test
  void comparesByDigitsWhateverTheFormatOfTheNumberThatArrives() {
    var id = UUID.randomUUID();
    var match = mock(PhoneMatch.class);
    when(match.getId()).thenReturn(id);
    when(match.getName()).thenReturn("Ana Díaz");
    when(match.getWhatsappConsent()).thenReturn(true);
    when(repository.findByPhoneInChurch("51999888777", church.value())).thenReturn(List.of(match));

    var result = handler.handle(new FindPeopleByPhoneQuery("+51 999-888-777", church));

    assertThat(result.people())
        .containsExactly(new FindPeopleByPhoneResult.Match(new PartyId(id), "Ana Díaz"));
  }

  @Test
  void whoAskedToStopIsReportedAsSuch() {
    var match = mock(PhoneMatch.class);
    when(match.getId()).thenReturn(UUID.randomUUID());
    when(match.getName()).thenReturn("Luis");
    when(match.getWhatsappConsent()).thenReturn(false);
    when(repository.findByPhoneInChurch("51999888777", church.value())).thenReturn(List.of(match));

    var result = handler.handle(new FindPeopleByPhoneQuery("51999888777", church));

    assertThat(result.people()).singleElement().extracting("whatsappConsent").isEqualTo(false);
  }

  @Test
  void aPhoneWithTooFewDigitsMatchesNobodyAndIsNotSearched() {
    assertThat(handler.handle(new FindPeopleByPhoneQuery("12345", church)).people()).isEmpty();
    assertThat(handler.handle(new FindPeopleByPhoneQuery(null, church)).people()).isEmpty();
    assertThat(handler.handle(new FindPeopleByPhoneQuery("abc", church)).people()).isEmpty();
    verifyNoInteractions(repository);
  }

  @Test
  void nobodyWithThatPhoneIsAnEmptyList() {
    when(repository.findByPhoneInChurch(any(), any())).thenReturn(List.of());

    assertThat(handler.handle(new FindPeopleByPhoneQuery("51999888777", church)).people())
        .isEmpty();
    verify(repository).findByPhoneInChurch("51999888777", church.value());
  }
}
