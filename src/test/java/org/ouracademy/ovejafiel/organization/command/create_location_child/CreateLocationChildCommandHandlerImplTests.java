package org.ouracademy.ovejafiel.organization.command.create_location_child;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.MEMBER;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.accountability.command.set_node_membership.SetNodeMembershipCommand;
import org.ouracademy.ovejafiel.accountability.command.set_node_membership.SetNodeMembershipCommandHandler;
import org.ouracademy.ovejafiel.organization.command.OrganizationInfo;
import org.ouracademy.ovejafiel.organization.command.OrganizationWriter;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.organization.repository.OrganizationRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class CreateLocationChildCommandHandlerImplTests {

  @Mock OrganizationWriter organizationWriter;
  @Mock OrganizationRepository organizationRepository;
  @Mock SetNodeMembershipCommandHandler setNodeMembershipCommandHandler;

  CreateLocationChildCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new CreateLocationChildCommandHandlerImpl(
            organizationWriter, organizationRepository, setNodeMembershipCommandHandler);
  }

  @Test
  void handleCreatesTheGroupAndPutsItsPeopleInItAndInTheNodesAboveIt() {
    var info = new OrganizationInfo("Youth", null, null, null);
    var churchId = PartyId.generate();
    var locationId = PartyId.generate();
    var groupId = PartyId.generate();
    var ana = PartyId.generate();
    var beto = PartyId.generate();
    when(organizationWriter.create(info, Organization.Type.GROUP, locationId))
        .thenReturn(organization(groupId, "Youth", Organization.Type.GROUP));
    when(organizationRepository.findAncestorIds(groupId.value()))
        .thenReturn(List.of(churchId.value(), locationId.value()));

    var result =
        handler.handle(
            new CreateLocationChildCommand(
                info, locationId, Organization.Type.GROUP, List.of(ana, beto, ana)));

    assertThat(result.organization().id()).isEqualTo(groupId);
    var above = List.of(churchId, locationId);
    verify(setNodeMembershipCommandHandler)
        .handle(new SetNodeMembershipCommand(ana, groupId, above, MEMBER));
    verify(setNodeMembershipCommandHandler)
        .handle(new SetNodeMembershipCommand(beto, groupId, above, MEMBER));
  }

  @Test
  void aGroupWithoutPeopleAddsNobody() {
    var info = new OrganizationInfo("Youth", null, null, null);
    var locationId = PartyId.generate();
    when(organizationWriter.create(info, Organization.Type.GROUP, locationId))
        .thenReturn(organization(PartyId.generate(), "Youth", Organization.Type.GROUP));

    handler.handle(
        new CreateLocationChildCommand(info, locationId, Organization.Type.GROUP, List.of()));

    verify(setNodeMembershipCommandHandler, never()).handle(ArgumentMatchers.any());
  }
}
