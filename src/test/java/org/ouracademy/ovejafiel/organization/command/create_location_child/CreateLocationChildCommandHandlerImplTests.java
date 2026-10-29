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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.accountability.command.set_node_membership.SetNodeMembershipCommand;
import org.ouracademy.ovejafiel.accountability.command.set_node_membership.SetNodeMembershipCommandHandler;
import org.ouracademy.ovejafiel.organization.command.OrganizationInfo;
import org.ouracademy.ovejafiel.organization.command.OrganizationWriter;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class CreateLocationChildCommandHandlerImplTests {

  @Mock OrganizationWriter organizationWriter;
  @Mock SetNodeMembershipCommandHandler setNodeMembershipCommandHandler;

  CreateLocationChildCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new CreateLocationChildCommandHandlerImpl(
            organizationWriter, setNodeMembershipCommandHandler);
  }

  @Test
  void handleCreatesTheGroupAndPutsItsPeopleInItAsMembersWhoLeadIfTheyDo() {
    var info = new OrganizationInfo("Youth", null, null, null);
    var locationId = PartyId.generate();
    var groupId = PartyId.generate();
    var member = PartyId.generate();
    var leader = PartyId.generate();
    when(organizationWriter.create(info, Organization.Type.GROUP, locationId))
        .thenReturn(organization(groupId, "Youth", Organization.Type.GROUP));

    var result =
        handler.handle(
            new CreateLocationChildCommand(
                info, locationId, Organization.Type.GROUP, List.of(member), List.of(leader)));

    assertThat(result.organization().id()).isEqualTo(groupId);
    verify(setNodeMembershipCommandHandler)
        .handle(new SetNodeMembershipCommand(member, groupId, MEMBER, false));
    verify(setNodeMembershipCommandHandler)
        .handle(new SetNodeMembershipCommand(leader, groupId, MEMBER, true));
  }

  @Test
  void aPersonInBothListsIsOnlyAddedOnceAndLeads() {
    var info = new OrganizationInfo("Youth", null, null, null);
    var locationId = PartyId.generate();
    var groupId = PartyId.generate();
    var both = PartyId.generate();
    when(organizationWriter.create(info, Organization.Type.GROUP, locationId))
        .thenReturn(organization(groupId, "Youth", Organization.Type.GROUP));

    handler.handle(
        new CreateLocationChildCommand(
            info, locationId, Organization.Type.GROUP, List.of(both), List.of(both)));

    verify(setNodeMembershipCommandHandler)
        .handle(new SetNodeMembershipCommand(both, groupId, MEMBER, true));
    verify(setNodeMembershipCommandHandler, never())
        .handle(new SetNodeMembershipCommand(both, groupId, MEMBER, false));
  }
}
