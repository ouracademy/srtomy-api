package org.ouracademy.srtomy.person.command.create_person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.PASTOR;
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
import org.ouracademy.srtomy.accountability.command.sync_person_roles.SyncPersonRolesCommand;
import org.ouracademy.srtomy.accountability.command.sync_person_roles.SyncPersonRolesCommandHandler;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.ouracademy.srtomy.organization.query.find_organization_by_id.FindOrganizationByIdQuery;
import org.ouracademy.srtomy.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.command.PersonInfo;
import org.ouracademy.srtomy.person.domain.Person;
import org.ouracademy.srtomy.person.repository.PersonRepository;

@ExtendWith(MockitoExtension.class)
class CreatePersonCommandHandlerImplTests {

  static final PersonInfo INFO =
      new PersonInfo("Ana", "Diaz", "999", 30, LocalDate.of(1995, 5, 17), true);

  @Mock PersonRepository personRepository;
  @Mock FindOrganizationByIdQueryHandler findOrganizationByIdQueryHandler;
  @Mock SyncPersonRolesCommandHandler syncPersonRolesCommandHandler;
  @Mock CreateAccountabilitiesCommandHandler createAccountabilitiesCommandHandler;

  CreatePersonCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new CreatePersonCommandHandlerImpl(
            personRepository,
            findOrganizationByIdQueryHandler,
            syncPersonRolesCommandHandler,
            createAccountabilitiesCommandHandler);
  }

  @Test
  void handleSavesThePersonSyncsRolesAndMakesThemAVisitorThatIsPartOfTheOrganization() {
    var organizationId = PartyId.generate();
    when(findOrganizationByIdQueryHandler.handle(new FindOrganizationByIdQuery(organizationId)))
        .thenReturn(Optional.of(church(organizationId)));
    when(personRepository.save(any(Person.class))).thenAnswer(call -> call.getArgument(0));

    var result = handler.handle(new CreatePersonCommand(INFO, organizationId, Set.of(PASTOR)));

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

    verify(syncPersonRolesCommandHandler)
        .handle(new SyncPersonRolesCommand(result.id(), organizationId, Set.of(PASTOR)));
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
    when(findOrganizationByIdQueryHandler.handle(new FindOrganizationByIdQuery(organizationId)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> handler.handle(new CreatePersonCommand(INFO, organizationId, Set.of())))
        .isInstanceOf(NoSuchElementException.class);
    verifyNoInteractions(
        personRepository, syncPersonRolesCommandHandler, createAccountabilitiesCommandHandler);
  }

  static FindOrganizationResult church(PartyId id) {
    return new FindOrganizationResult(
        id, "Church", null, null, null, Organization.Type.CHURCH, null, null);
  }
}
