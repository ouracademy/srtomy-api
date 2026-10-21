package org.ouracademy.srtomy.person.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.person.PersonFixtures.person;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.repository.PersonRepository;

@ExtendWith(MockitoExtension.class)
class FindPeopleByIdsQueryHandlerImplTests {

  @Mock PersonRepository personRepository;
  @InjectMocks FindPeopleByIdsQueryHandlerImpl handler;

  @Test
  void handleLoadsAllPeopleInOneCallAndSkipsIdsThatAreNotPeople() {
    var ana = PartyId.generate();
    var luis = PartyId.generate();
    var unknown = PartyId.generate();
    when(personRepository.findAllById(List.of(ana, luis, unknown)))
        .thenReturn(List.of(person(ana, "Ana", "Diaz"), person(luis, "Luis", "Rojas")));

    var result = handler.handle(new FindPeopleByIdsQuery(List.of(ana, luis, unknown)));

    assertThat(result).extracting(FindPersonResult::id).containsExactlyInAnyOrder(ana, luis);
    assertThat(result).extracting(FindPersonResult::firstName).contains("Ana", "Luis");
  }

  @Test
  void handleDoesNotQueryForNoIds() {
    assertThat(handler.handle(new FindPeopleByIdsQuery(List.of()))).isEmpty();
    verifyNoInteractions(personRepository);
  }
}
