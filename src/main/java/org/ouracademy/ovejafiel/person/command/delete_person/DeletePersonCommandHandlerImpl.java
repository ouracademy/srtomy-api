package org.ouracademy.ovejafiel.person.command.delete_person;

import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.command.end_accountabilities_of_child.EndAccountabilitiesOfChildCommand;
import org.ouracademy.ovejafiel.accountability.command.end_accountabilities_of_child.EndAccountabilitiesOfChildCommandHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.ovejafiel.person.command.delete_person.DeletePersonResult.Outcome;
import org.ouracademy.ovejafiel.person.repository.PersonRepository;
import org.ouracademy.ovejafiel.user.command.unlink_user_from_person.UnlinkUserFromPersonCommand;
import org.ouracademy.ovejafiel.user.command.unlink_user_from_person.UnlinkUserFromPersonCommandHandler;
import org.ouracademy.ovejafiel.user.query.is_person_linked.IsPersonLinkedQuery;
import org.ouracademy.ovejafiel.user.query.is_person_linked.IsPersonLinkedQueryHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class DeletePersonCommandHandlerImpl implements DeletePersonCommandHandler {

  private final PersonRepository personRepository;
  private final FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  private final FindOrganizationSubtreeQueryHandler findOrganizationSubtreeQueryHandler;
  private final IsPersonLinkedQueryHandler isPersonLinkedQueryHandler;
  private final UnlinkUserFromPersonCommandHandler unlinkUserFromPersonCommandHandler;
  private final EndAccountabilitiesOfChildCommandHandler endAccountabilitiesOfChildCommandHandler;

  @Override
  @Transactional
  public DeletePersonResult handle(DeletePersonCommand command) {
    var person =
        personRepository
            .findById(command.personId())
            .filter(found -> found.getDeletedTime() == null);
    if (person.isEmpty()) {
      return new DeletePersonResult(Outcome.NOT_FOUND);
    }
    var churchId =
        findChurchOfOrganizationQueryHandler
            .handle(new FindChurchOfOrganizationQuery(command.organizationId()))
            .orElseThrow(
                () ->
                    new NoSuchElementException(
                        "Organization not found: " + command.organizationId().value()))
            .churchId();
    var wholeChurch = churchId.equals(command.organizationId());

    if (wholeChurch && isLinked(command)) {
      if (!command.confirmUnlinkUser()) {
        return new DeletePersonResult(Outcome.REQUIRES_USER_UNLINK);
      }
      unlinkUserFromPersonCommandHandler.handle(
          new UnlinkUserFromPersonCommand(command.personId()));
    }

    if (wholeChurch) {
      person.get().delete();
    }
    var organizationIds =
        findOrganizationSubtreeQueryHandler
            .handle(new FindOrganizationSubtreeQuery(command.organizationId()))
            .ids();
    endAccountabilitiesOfChildCommandHandler.handle(
        new EndAccountabilitiesOfChildCommand(command.personId(), organizationIds));

    return new DeletePersonResult(wholeChurch ? Outcome.DELETED : Outcome.REMOVED_FROM_NODE);
  }

  private boolean isLinked(DeletePersonCommand command) {
    return isPersonLinkedQueryHandler.handle(new IsPersonLinkedQuery(command.personId())).linked();
  }
}
