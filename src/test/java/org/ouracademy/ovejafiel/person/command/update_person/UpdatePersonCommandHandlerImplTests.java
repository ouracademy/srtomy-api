package org.ouracademy.ovejafiel.person.command.update_person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.person.PersonFixtures.person;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.accountability.command.set_member_state.SetMemberStateCommand;
import org.ouracademy.ovejafiel.accountability.command.set_member_state.SetMemberStateCommandHandler;
import org.ouracademy.ovejafiel.accountability.command.sync_person_positions.SyncPersonPositionsCommand;
import org.ouracademy.ovejafiel.accountability.command.sync_person_positions.SyncPersonPositionsCommandHandler;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.command.PersonInfo;
import org.ouracademy.ovejafiel.person.domain.MemberState;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;

@ExtendWith(MockitoExtension.class)
class UpdatePersonCommandHandlerImplTests {

  static final PersonInfo INFO =
      new PersonInfo("Luis", "Rojas", "111", null, 41, LocalDate.of(1984, 1, 2), false);

  @Mock PersonRepository personRepository;
  @Mock SyncPersonPositionsCommandHandler syncPersonPositionsCommandHandler;
  @Mock FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  @Mock SetMemberStateCommandHandler setMemberStateCommandHandler;

  UpdatePersonCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new UpdatePersonCommandHandlerImpl(
            personRepository,
            syncPersonPositionsCommandHandler,
            findChurchOfOrganizationQueryHandler,
            setMemberStateCommandHandler);
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

  PartyId existingPerson(PartyId locationId) {
    var id = PartyId.generate();
    var person = person(id, "Ana", "Diaz");
    when(personRepository.findById(id)).thenReturn(Optional.of(person));
    when(personRepository.save(person)).thenReturn(person);
    when(findChurchOfOrganizationQueryHandler.handle(new FindChurchOfOrganizationQuery(locationId)))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(PartyId.generate())));
    return id;
  }

  @Test
  void aPositionMakesThemAMemberWhateverTheStateAsked() {
    var locationId = PartyId.generate();
    var id = existingPerson(locationId);

    handler.handle(
        new UpdatePersonCommand(
            "me",
            id,
            INFO,
            locationId,
            Set.of(AccountabilityTypeId.generate()),
            MemberState.VISITOR));

    verify(setMemberStateCommandHandler)
        .handle(new SetMemberStateCommand(id, locationId, SystemAccountabilityTypes.MEMBER));
  }

  @Test
  void theStateAskedForIsSetWhenThereAreNoPositions() {
    var locationId = PartyId.generate();
    var id = existingPerson(locationId);

    handler.handle(
        new UpdatePersonCommand(
            "me", id, INFO, locationId, Set.of(), MemberState.MEMBERSHIP_IN_PROGRESS));

    verify(setMemberStateCommandHandler)
        .handle(
            new SetMemberStateCommand(
                id, locationId, SystemAccountabilityTypes.MEMBERSHIP_IN_PROGRESS));
  }

  @Test
  void theStateIsLeftAloneWhenNothingAsksForIt() {
    var locationId = PartyId.generate();
    var id = existingPerson(locationId);

    handler.handle(new UpdatePersonCommand("me", id, INFO, locationId, Set.of()));

    verifyNoInteractions(setMemberStateCommandHandler);
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
