package org.ouracademy.ovejafiel.person.command.create_person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBERSHIP_IN_PROGRESS;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.VISITOR;

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
import org.ouracademy.ovejafiel.accountability.command.set_node_membership.SetNodeMembershipCommand;
import org.ouracademy.ovejafiel.accountability.command.set_node_membership.SetNodeMembershipCommandHandler;
import org.ouracademy.ovejafiel.accountability.command.sync_person_positions.SyncPersonPositionsCommand;
import org.ouracademy.ovejafiel.accountability.command.sync_person_positions.SyncPersonPositionsCommandHandler;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.ovejafiel.organization.query.find_organization_ancestors.FindOrganizationAncestorsQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_ancestors.FindOrganizationAncestorsResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.command.PersonInfo;
import org.ouracademy.ovejafiel.person.domain.MemberState;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;

@ExtendWith(MockitoExtension.class)
class CreatePersonCommandHandlerImplTests {

  static final PersonInfo INFO =
      new PersonInfo("Ana", "Diaz", "999", null, 30, LocalDate.of(1995, 5, 17), true);

  @Mock PersonRepository personRepository;
  @Mock FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  @Mock SyncPersonPositionsCommandHandler syncPersonPositionsCommandHandler;
  @Mock SetNodeMembershipCommandHandler setNodeMembershipCommandHandler;
  @Mock FindOrganizationAncestorsQueryHandler findOrganizationAncestorsQueryHandler;

  CreatePersonCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    org.mockito.Mockito.lenient()
        .when(findOrganizationAncestorsQueryHandler.handle(any()))
        .thenReturn(new FindOrganizationAncestorsResult(List.of()));
    handler =
        new CreatePersonCommandHandlerImpl(
            personRepository,
            findChurchOfOrganizationQueryHandler,
            syncPersonPositionsCommandHandler,
            setNodeMembershipCommandHandler,
            findOrganizationAncestorsQueryHandler);
  }

  @Test
  void handleSavesThePersonSyncsPositionsAndMakesThemAMemberThatIsPartOfTheOrganization() {
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
    verify(setNodeMembershipCommandHandler)
        .handle(new SetNodeMembershipCommand(result.id(), organizationId, List.of(), MEMBER));
  }

  private void churchOf(PartyId organizationId) {
    when(findChurchOfOrganizationQueryHandler.handle(
            new FindChurchOfOrganizationQuery(organizationId)))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(PartyId.generate())));
    when(personRepository.save(any(Person.class))).thenAnswer(call -> call.getArgument(0));
  }

  @Test
  void aNewPersonWithoutPositionsOrStateIsAVisitor() {
    var organizationId = PartyId.generate();
    churchOf(organizationId);

    var result = handler.handle(new CreatePersonCommand("me", INFO, organizationId, Set.of()));

    verify(setNodeMembershipCommandHandler)
        .handle(new SetNodeMembershipCommand(result.id(), organizationId, List.of(), VISITOR));
  }

  @Test
  void theStateAskedForIsTheOneTheyStartWith() {
    var organizationId = PartyId.generate();
    churchOf(organizationId);

    var result =
        handler.handle(
            new CreatePersonCommand(
                "me", INFO, organizationId, Set.of(), MemberState.MEMBERSHIP_IN_PROGRESS));

    verify(setNodeMembershipCommandHandler)
        .handle(
            new SetNodeMembershipCommand(
                result.id(), organizationId, List.of(), MEMBERSHIP_IN_PROGRESS));
  }

  @Test
  void aPositionMakesThemAMemberWhateverTheStateAsked() {
    var organizationId = PartyId.generate();
    churchOf(organizationId);

    var result =
        handler.handle(
            new CreatePersonCommand(
                "me",
                INFO,
                organizationId,
                Set.of(AccountabilityTypeId.generate()),
                MemberState.VISITOR));

    verify(setNodeMembershipCommandHandler)
        .handle(new SetNodeMembershipCommand(result.id(), organizationId, List.of(), MEMBER));
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
        personRepository, syncPersonPositionsCommandHandler, setNodeMembershipCommandHandler);
  }

  @Test
  void aPersonIsAlsoKnownByTheNodesAboveTheOneTheyAreRegisteredIn() {
    var organizationId = PartyId.generate();
    var churchId = PartyId.generate();
    when(findChurchOfOrganizationQueryHandler.handle(
            new FindChurchOfOrganizationQuery(organizationId)))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(churchId)));
    when(personRepository.save(any(Person.class))).thenAnswer(call -> call.getArgument(0));
    when(findOrganizationAncestorsQueryHandler.handle(any()))
        .thenReturn(new FindOrganizationAncestorsResult(List.of(churchId)));

    var result = handler.handle(new CreatePersonCommand("me", INFO, organizationId, Set.of()));

    verify(setNodeMembershipCommandHandler)
        .handle(
            new SetNodeMembershipCommand(result.id(), organizationId, List.of(churchId), VISITOR));
  }
}
