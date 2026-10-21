package org.ouracademy.srtomy.person.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.USHER;
import static org.ouracademy.srtomy.person.PersonFixtures.person;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.command.SyncPersonRolesCommand;
import org.ouracademy.srtomy.accountability.command.SyncPersonRolesCommandHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.repository.PersonRepository;

@ExtendWith(MockitoExtension.class)
class UpdatePersonCommandHandlerImplTests {

  static final PersonInfo INFO =
      new PersonInfo("Luis", "Rojas", "111", 41, LocalDate.of(1984, 1, 2), false);

  @Mock PersonRepository personRepository;
  @Mock SyncPersonRolesCommandHandler syncPersonRolesCommandHandler;

  UpdatePersonCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler = new UpdatePersonCommandHandlerImpl(personRepository, syncPersonRolesCommandHandler);
  }

  @Test
  void handleOverwritesThePersonDataAndSyncsTheirRoles() {
    var id = PartyId.generate();
    var locationId = PartyId.generate();
    var person = person(id, "Ana", "Diaz");
    when(personRepository.findById(id)).thenReturn(Optional.of(person));
    when(personRepository.save(person)).thenReturn(person);

    var result = handler.handle(new UpdatePersonCommand(id, INFO, locationId, Set.of(USHER)));

    assertThat(result).contains(new UpdatePersonResult(id));
    assertThat(person.getFirstName()).isEqualTo("Luis");
    assertThat(person.getLastName()).isEqualTo("Rojas");
    assertThat(person.getFullName()).isEqualTo("Luis Rojas");
    assertThat(person.getPhoneNumber()).isEqualTo("111");
    assertThat(person.getAge()).isEqualTo(41);
    assertThat(person.getBirthdate()).isEqualTo(LocalDate.of(1984, 1, 2));
    assertThat(person.getHasBeenBaptized()).isFalse();
    verify(syncPersonRolesCommandHandler)
        .handle(new SyncPersonRolesCommand(id, locationId, Set.of(USHER)));
  }

  @Test
  void handleIsEmptyForAMissingPerson() {
    var id = PartyId.generate();
    when(personRepository.findById(id)).thenReturn(Optional.empty());

    var result =
        handler.handle(new UpdatePersonCommand(id, INFO, PartyId.generate(), Set.of(USHER)));

    assertThat(result).isEmpty();
    verifyNoInteractions(syncPersonRolesCommandHandler);
  }
}
