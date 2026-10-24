package org.ouracademy.srtomy.person.command.create_person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.VISITOR;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.command.create_accountabilities.CreateAccountabilitiesCommand;
import org.ouracademy.srtomy.accountability.command.create_accountabilities.CreateAccountabilitiesCommandHandler;
import org.ouracademy.srtomy.accountability.command.create_accountability.CreateAccountabilityCommand;
import org.ouracademy.srtomy.accountability.command.sync_person_positions.SyncPersonPositionsCommand;
import org.ouracademy.srtomy.accountability.command.sync_person_positions.SyncPersonPositionsCommandHandler;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.command.PersonInfo;
import org.ouracademy.srtomy.person.domain.Person;
import org.ouracademy.srtomy.person.repository.PersonRepository;

@ExtendWith(MockitoExtension.class)
class CreatePersonCommandHandlerImplTests {

  static final PersonInfo INFO =
      new PersonInfo("Ana", "Diaz", "999", 30, LocalDate.of(1995, 5, 17), true);

  @Mock PersonRepository personRepository;
  @Mock FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  @Mock SyncPersonPositionsCommandHandler syncPersonPositionsCommandHandler;
  @Mock CreateAccountabilitiesCommandHandler createAccountabilitiesCommandHandler;

  CreatePersonCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new CreatePersonCommandHandlerImpl(
            personRepository,
            findChurchOfOrganizationQueryHandler,
            syncPersonPositionsCommandHandler,
            createAccountabilitiesCommandHandler);
  }

  @Test
  void handleSavesThePersonSyncsPositionsAndMakesThemAVisitorThatIsPartOfTheOrganization() {
    var organizationId = PartyId.generate();
    var churchId = PartyId.generate();
    var pastor = AccountabilityTypeId.generate();
    when(findChurchOfOrganizationQueryHandler.handle(
            new FindChurchOfOrganizationQuery(organizationId)))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(churchId)));
    when(personRepository.save(any(Person.class))).thenAnswer(call -> call.getArgument(0));

    var result =
        handler.handle(new CreatePersonCommand("me", INFO, organizationId, Set.of(pastor)));

    var saved = ArgumentCaptor.forClass(Person.class);
    verify(personRepository).save(saved.capture());
    var person = saved.getValue();
    assertThat(person.getId()).isEqualTo(result.id());
    assertThat(person.getFirstName()).isEqualTo("Ana");
    assertThat(person.getLastName()).isEqualTo("Diaz");
    assertThat(person.getFullName()).isEqualTo("Ana Diaz");
    assertThat(person.getPhoneNumber()).isEqualTo("999");
    assertThat(person.getAge()).isEqualTo(30);
    assertThat(person.getBirthdate()).isEqualTo(LocalDate.of(1995, 5, 17));
    assertThat(person.getHasBeenBaptized()).isTrue();

    verify(syncPersonPositionsCommandHandler)
        .handle(
            new SyncPersonPositionsCommand(
                "me", result.id(), organizationId, churchId, Set.of(pastor)));
    verify(createAccountabilitiesCommandHandler)
        .handle(
            new CreateAccountabilitiesCommand(
                List.of(
                    new CreateAccountabilityCommand(result.id(), organizationId, VISITOR),
                    new CreateAccountabilityCommand(result.id(), organizationId, IS_PART_OF))));
  }

  @Test
  void handleThrowsAndCreatesNothingWhenTheOrganizationDoesNotExist() {
    var organizationId = PartyId.generate();
    when(findChurchOfOrganizationQueryHandler.handle(
            new FindChurchOfOrganizationQuery(organizationId)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> handler.handle(new CreatePersonCommand("me", INFO, organizationId, Set.of())))
        .isInstanceOf(NoSuchElementException.class);
    verifyNoInteractions(
        personRepository, syncPersonPositionsCommandHandler, createAccountabilitiesCommandHandler);
  }
}
