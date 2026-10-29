package org.ouracademy.ovejafiel.person.command.restore_person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.ovejafiel.person.PersonFixtures.person;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.accountability.command.create_accountabilities.CreateAccountabilitiesCommand;
import org.ouracademy.ovejafiel.accountability.command.create_accountabilities.CreateAccountabilitiesCommandHandler;
import org.ouracademy.ovejafiel.accountability.command.create_accountability.CreateAccountabilityCommand;
import org.ouracademy.ovejafiel.accountability.query.find_organizations_of_member.FindOrganizationsOfMemberQueryHandler;
import org.ouracademy.ovejafiel.accountability.query.find_organizations_of_member.OrganizationsOfMemberResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.MemberState;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;
import org.ouracademy.ovejafiel.shared.domain.ConflictException;

@ExtendWith(MockitoExtension.class)
class RestorePersonCommandHandlerImplTests {

  @Mock PersonRepository personRepository;
  @Mock FindOrganizationsOfMemberQueryHandler findOrganizationsOfMemberQueryHandler;
  @Mock CreateAccountabilitiesCommandHandler createAccountabilitiesCommandHandler;

  RestorePersonCommandHandlerImpl handler;

  PartyId personId = PartyId.generate();
  PartyId nodeId = PartyId.generate();

  @BeforeEach
  void setUp() {
    handler =
        new RestorePersonCommandHandlerImpl(
            personRepository,
            findOrganizationsOfMemberQueryHandler,
            createAccountabilitiesCommandHandler);
  }

  @Test
  void handleClearsTheRemovalAndGivesANewVisitorMembership() {
    var ana = person(personId, "Ana", "Diaz");
    ana.delete();
    when(personRepository.findById(personId)).thenReturn(Optional.of(ana));
    when(findOrganizationsOfMemberQueryHandler.handle(any()))
        .thenReturn(new OrganizationsOfMemberResult(List.of()));

    var result = handler.handle(new RestorePersonCommand(personId, nodeId));

    assertThat(result.id()).isEqualTo(personId);
    assertThat(ana.getDeletedTime()).isNull();
    verify(createAccountabilitiesCommandHandler)
        .handle(
            new CreateAccountabilitiesCommand(
                List.of(
                    new CreateAccountabilityCommand(personId, nodeId, MemberState.VISITOR.typeId()),
                    new CreateAccountabilityCommand(personId, nodeId, IS_PART_OF))));
  }

  @Test
  void handleOfAPersonAlreadyInTheNodeIsAConflictAndChangesNothing() {
    var ana = person(personId, "Ana", "Diaz");
    when(personRepository.findById(personId)).thenReturn(Optional.of(ana));
    when(findOrganizationsOfMemberQueryHandler.handle(any()))
        .thenReturn(new OrganizationsOfMemberResult(List.of(nodeId)));

    assertThatThrownBy(() -> handler.handle(new RestorePersonCommand(personId, nodeId)))
        .isInstanceOf(ConflictException.class)
        .extracting("code")
        .isEqualTo("PERSON_ALREADY_ACTIVE");
    verifyNoInteractions(createAccountabilitiesCommandHandler);
  }

  @Test
  void handleOfAMissingPersonThrowsNotFound() {
    when(personRepository.findById(personId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(new RestorePersonCommand(personId, nodeId)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
