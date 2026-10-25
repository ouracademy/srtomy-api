package org.ouracademy.srtomy.person.command.update_person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.person.PersonFixtures.person;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.command.sync_person_positions.SyncPersonPositionsCommand;
import org.ouracademy.srtomy.accountability.command.sync_person_positions.SyncPersonPositionsCommandHandler;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.command.PersonInfo;
import org.ouracademy.srtomy.person.repository.PersonRepository;

@ExtendWith(MockitoExtension.class)
class UpdatePersonCommandHandlerImplTests {

  static final PersonInfo INFO =
      new PersonInfo("Luis", "Rojas", "111", null, 41, LocalDate.of(1984, 1, 2), false);

  @Mock PersonRepository personRepository;
  @Mock SyncPersonPositionsCommandHandler syncPersonPositionsCommandHandler;
  @Mock FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;

  UpdatePersonCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new UpdatePersonCommandHandlerImpl(
            personRepository,
            syncPersonPositionsCommandHandler,
            findChurchOfOrganizationQueryHandler);
  }

  @Test
  void handleOverwritesThePersonDataAndSyncsTheirPositionsWithinTheChurch() {
    var id = PartyId.generate();
    var locationId = PartyId.generate();
    var churchId = PartyId.generate();
    var usher = AccountabilityTypeId.generate();
    var person = person(id, "Ana", "Diaz");
    when(personRepository.findById(id)).thenReturn(Optional.of(person));
    when(personRepository.save(person)).thenReturn(person);
    when(findChurchOfOrganizationQueryHandler.handle(new FindChurchOfOrganizationQuery(locationId)))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(churchId)));

    var result = handler.handle(new UpdatePersonCommand("me", id, INFO, locationId, Set.of(usher)));

    assertThat(result).contains(new UpdatePersonResult(id));
    assertThat(person.getFirstName()).isEqualTo("Luis");
    assertThat(person.getLastName()).isEqualTo("Rojas");
    assertThat(person.getFullName()).isEqualTo("Luis Rojas");
    assertThat(person.getPhoneNumber()).isEqualTo("111");
    assertThat(person.getAge()).isEqualTo(41);
    assertThat(person.getBirthdate()).isEqualTo(LocalDate.of(1984, 1, 2));
    assertThat(person.getHasBeenBaptized()).isFalse();
    verify(syncPersonPositionsCommandHandler)
        .handle(new SyncPersonPositionsCommand("me", id, locationId, churchId, Set.of(usher)));
  }

  @Test
  void handleIsEmptyForAMissingPerson() {
    var id = PartyId.generate();
    when(personRepository.findById(id)).thenReturn(Optional.empty());

    var result =
        handler.handle(new UpdatePersonCommand("me", id, INFO, PartyId.generate(), Set.of()));

    assertThat(result).isEmpty();
    verifyNoInteractions(syncPersonPositionsCommandHandler);
  }
}
