package org.ouracademy.ovejafiel.organization.command.create_location_child;

import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER;

import java.util.LinkedHashSet;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.command.set_node_membership.SetNodeMembershipCommand;
import org.ouracademy.ovejafiel.accountability.command.set_node_membership.SetNodeMembershipCommandHandler;
import org.ouracademy.ovejafiel.organization.command.OrganizationWriter;
import org.ouracademy.ovejafiel.organization.query.FindOrganizationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CreateLocationChildCommandHandlerImpl implements CreateLocationChildCommandHandler {

  private final OrganizationWriter organizationWriter;
  private final SetNodeMembershipCommandHandler setNodeMembershipCommandHandler;

  @Override
  @Transactional
  public CreateLocationChildResult handle(CreateLocationChildCommand command) {
    var locationChild =
        organizationWriter.create(command.info(), command.type(), command.parentId());
    var childId = locationChild.getId();
    var people = new LinkedHashSet<>(command.memberIds());
    people.addAll(command.leaderIds());
    for (var personId : people) {
      setNodeMembershipCommandHandler.handle(
          new SetNodeMembershipCommand(
              personId, childId, MEMBER, command.leaderIds().contains(personId)));
    }
    return new CreateLocationChildResult(FindOrganizationResult.from(locationChild));
  }
}
