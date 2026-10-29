package org.ouracademy.ovejafiel.person.command.delete_person;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.person.PersonFixtures.person;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.accountability.command.end_accountabilities_of_child.EndAccountabilitiesOfChildCommand;
import org.ouracademy.ovejafiel.accountability.command.end_accountabilities_of_child.EndAccountabilitiesOfChildCommandHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.command.delete_person.DeletePersonResult.Outcome;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;
import org.ouracademy.ovejafiel.user.command.unlink_user_from_person.UnlinkUserFromPersonCommand;
import org.ouracademy.ovejafiel.user.command.unlink_user_from_person.UnlinkUserFromPersonCommandHandler;
import org.ouracademy.ovejafiel.user.query.is_person_linked.IsPersonLinkedQueryHandler;
import org.ouracademy.ovejafiel.user.query.is_person_linked.IsPersonLinkedResult;

@ExtendWith(MockitoExtension.class)
class DeletePersonCommandHandlerImplTests {

  @Mock PersonRepository personRepository;
  @Mock FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  @Mock FindOrganizationSubtreeQueryHandler findOrganizationSubtreeQueryHandler;
  @Mock IsPersonLinkedQueryHandler isPersonLinkedQueryHandler;
  @Mock UnlinkUserFromPersonCommandHandler unlinkUserFromPersonCommandHandler;
  @Mock EndAccountabilitiesOfChildCommandHandler endAccountabilitiesOfChildCommandHandler;

  DeletePersonCommandHandlerImpl handler;

  PartyId personId = PartyId.generate();
  PartyId churchId = PartyId.generate();
  PartyId groupId = PartyId.generate();

  @BeforeEach
  void setUp() {
    handler =
        new DeletePersonCommandHandlerImpl(
            personRepository,
            findChurchOfOrganizationQueryHandler,
            findOrganizationSubtreeQueryHandler,
            isPersonLinkedQueryHandler,
            unlinkUserFromPersonCommandHandler,
            endAccountabilitiesOfChildCommandHandler);
  }

  @Test
  void handleOfAMissingPersonIsNotFoundAndChangesNothing() {
    when(personRepository.findById(personId)).thenReturn(Optional.empty());

    var result = handler.handle(new DeletePersonCommand(personId, churchId, false));

    assertThat(result.outcome()).isEqualTo(Outcome.NOT_FOUND);
    verifyNoInteractions(
        endAccountabilitiesOfChildCommandHandler, unlinkUserFromPersonCommandHandler);
  }

  @Test
  void handleOfAPersonAlreadyRemovedFromTheChurchIsNotFound() {
    var removed = person(personId, "Ana", "Diaz");
    removed.delete();
    when(personRepository.findById(personId)).thenReturn(Optional.of(removed));

    var result = handler.handle(new DeletePersonCommand(personId, churchId, false));

    assertThat(result.outcome()).isEqualTo(Outcome.NOT_FOUND);
  }

  @Test
  void handleFromAGroupClosesOnlyTheMembershipsUnderThatGroupAndKeepsThePerson() {
    var ana = person(personId, "Ana", "Diaz");
    var child = PartyId.generate();
    when(personRepository.findById(personId)).thenReturn(Optional.of(ana));
    churchIs(churchId);
    when(findOrganizationSubtreeQueryHandler.handle(any()))
        .thenReturn(new FindOrganizationSubtreeResult(List.of(groupId, child)));

    var result = handler.handle(new DeletePersonCommand(personId, groupId, false));

    assertThat(result.outcome()).isEqualTo(Outcome.REMOVED_FROM_NODE);
    assertThat(ana.getDeletedTime()).isNull();
    verify(endAccountabilitiesOfChildCommandHandler)
        .handle(new EndAccountabilitiesOfChildCommand(personId, List.of(groupId, child)));
    verifyNoInteractions(isPersonLinkedQueryHandler, unlinkUserFromPersonCommandHandler);
  }

  @Test
  void handleFromTheChurchClosesEverythingAndMarksThePersonRemoved() {
    var ana = person(personId, "Ana", "Diaz");
    when(personRepository.findById(personId)).thenReturn(Optional.of(ana));
    churchIs(churchId);
    when(findOrganizationSubtreeQueryHandler.handle(any()))
        .thenReturn(new FindOrganizationSubtreeResult(List.of(churchId, groupId)));
    when(isPersonLinkedQueryHandler.handle(any())).thenReturn(new IsPersonLinkedResult(false));

    var result = handler.handle(new DeletePersonCommand(personId, churchId, false));

    assertThat(result.outcome()).isEqualTo(Outcome.DELETED);
    assertThat(ana.getDeletedTime()).isNotNull();
    verify(endAccountabilitiesOfChildCommandHandler)
        .handle(new EndAccountabilitiesOfChildCommand(personId, List.of(churchId, groupId)));
    verifyNoInteractions(unlinkUserFromPersonCommandHandler);
  }

  @Test
  void handleFromTheChurchOfAPersonWithAnAccountAsksForConfirmationAndChangesNothing() {
    var ana = person(personId, "Ana", "Diaz");
    when(personRepository.findById(personId)).thenReturn(Optional.of(ana));
    churchIs(churchId);
    when(isPersonLinkedQueryHandler.handle(any())).thenReturn(new IsPersonLinkedResult(true));

    var result = handler.handle(new DeletePersonCommand(personId, churchId, false));

    assertThat(result.outcome()).isEqualTo(Outcome.REQUIRES_USER_UNLINK);
    assertThat(ana.getDeletedTime()).isNull();
    verifyNoInteractions(
        unlinkUserFromPersonCommandHandler, endAccountabilitiesOfChildCommandHandler);
  }

  @Test
  void handleFromTheChurchOfAPersonWithAnAccountUnlinksTheUserWhenConfirmed() {
    var ana = person(personId, "Ana", "Diaz");
    when(personRepository.findById(personId)).thenReturn(Optional.of(ana));
    churchIs(churchId);
    when(findOrganizationSubtreeQueryHandler.handle(any()))
        .thenReturn(new FindOrganizationSubtreeResult(List.of(churchId)));
    when(isPersonLinkedQueryHandler.handle(any())).thenReturn(new IsPersonLinkedResult(true));

    var result = handler.handle(new DeletePersonCommand(personId, churchId, true));

    assertThat(result.outcome()).isEqualTo(Outcome.DELETED);
    verify(unlinkUserFromPersonCommandHandler).handle(new UnlinkUserFromPersonCommand(personId));
    verify(endAccountabilitiesOfChildCommandHandler).handle(any());
  }

  @Test
  void handleOfAnUnknownOrganizationThrowsNotFound() {
    when(personRepository.findById(personId))
        .thenReturn(Optional.of(person(personId, "Ana", "Diaz")));
    when(findChurchOfOrganizationQueryHandler.handle(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(new DeletePersonCommand(personId, groupId, false)))
        .isInstanceOf(NoSuchElementException.class);
  }

  private void churchIs(PartyId id) {
    when(findChurchOfOrganizationQueryHandler.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(id)));
  }
}
