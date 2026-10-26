package org.ouracademy.ovejafiel.person.query.find_people_by_email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;

class FindPeopleByEmailQueryHandlerImplTests {

  PersonRepository repository = mock(PersonRepository.class);
  FindPeopleByEmailQueryHandlerImpl handler = new FindPeopleByEmailQueryHandlerImpl(repository);

  PartyId node = PartyId.generate();

  @Test
  void findsThePeopleOfTheNodeWithThatEmail() {
    var person = mock(Person.class);
    var id = PartyId.generate();
    when(person.getId()).thenReturn(id);
    when(repository.findByEmailAndOrganization("ana@x.org", node)).thenReturn(List.of(person));

    assertThat(handler.handle(new FindPeopleByEmailQuery(" ana@x.org ", node)).personIds())
        .containsExactly(id);
  }

  @Test
  void aBlankEmailMatchesNobody() {
    assertThat(handler.handle(new FindPeopleByEmailQuery(" ", node)).personIds()).isEmpty();
    assertThat(handler.handle(new FindPeopleByEmailQuery(null, node)).personIds()).isEmpty();
    verifyNoInteractions(repository);
  }
}
