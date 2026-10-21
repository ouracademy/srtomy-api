package org.ouracademy.srtomy.organization.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.LEADER;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.command.CreateAccountabilitiesCommand;
import org.ouracademy.srtomy.accountability.command.CreateAccountabilitiesCommandHandler;
import org.ouracademy.srtomy.accountability.command.CreateAccountabilityCommand;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class CreateLocationChildCommandHandlerImplTests {

  @Mock OrganizationWriter organizationWriter;
  @Mock CreateAccountabilitiesCommandHandler createAccountabilitiesCommandHandler;

  CreateLocationChildCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new CreateLocationChildCommandHandlerImpl(
            organizationWriter, createAccountabilitiesCommandHandler);
  }

  @Test
  void handleCreatesTheGroupAndItsMembersAndLeadersInASingleBatch() {
    var info = new OrganizationInfo("Youth", null, null, null);
    var locationId = PartyId.generate();
    var groupId = PartyId.generate();
    var member1 = PartyId.generate();
    var member2 = PartyId.generate();
    var leader = PartyId.generate();
    when(organizationWriter.create(info, Organization.Type.GROUP, locationId))
        .thenReturn(organization(groupId, "Youth", Organization.Type.GROUP));

    var result =
        handler.handle(
            new CreateLocationChildCommand(
                info,
                locationId,
                Organization.Type.GROUP,
                List.of(member1, member2),
                List.of(leader)));

    assertThat(result.organization().id()).isEqualTo(groupId);
    verify(createAccountabilitiesCommandHandler)
        .handle(
            new CreateAccountabilitiesCommand(
                List.of(
                    new CreateAccountabilityCommand(member1, groupId, IS_PART_OF),
                    new CreateAccountabilityCommand(member2, groupId, IS_PART_OF),
                    new CreateAccountabilityCommand(leader, groupId, LEADER))));
  }
}
