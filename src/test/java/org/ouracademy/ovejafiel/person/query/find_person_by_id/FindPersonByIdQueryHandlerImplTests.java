package org.ouracademy.ovejafiel.person.query.find_person_by_id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.person.PersonFixtures.person;

import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;

@ExtendWith(MockitoExtension.class)
class FindPersonByIdQueryHandlerImplTests {

  @Mock PersonRepository personRepository;
  @InjectMocks FindPersonByIdQueryHandlerImpl handler;

  @Test
  void handleMapsThePersonToAResult() {
    var id = PartyId.generate();
    when(personRepository.findById(id)).thenReturn(Optional.of(person(id, "Ana", "Diaz")));

    var result = handler.handle(new FindPersonByIdQuery(id)).orElseThrow();

    assertThat(result.id()).isEqualTo(id);
    assertThat(result.name()).isEqualTo("Ana Diaz");
    assertThat(result.firstName()).isEqualTo("Ana");
    assertThat(result.lastName()).isEqualTo("Diaz");
    assertThat(result.phoneNumber()).isEqualTo("999");
    assertThat(result.age()).isEqualTo(30);
    assertThat(result.birthdate()).isEqualTo(LocalDate.of(1995, 5, 17));
    assertThat(result.hasBeenBaptized()).isTrue();
  }

  @Test
  void handleIsEmptyForAMissingPerson() {
    var id = PartyId.generate();
    when(personRepository.findById(id)).thenReturn(Optional.empty());

    assertThat(handler.handle(new FindPersonByIdQuery(id))).isEmpty();
  }
}
