package org.ouracademy.srtomy.organization.command.update_location_child;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.LEADER;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.command.create_accountabilities.CreateAccountabilitiesCommand;
import org.ouracademy.srtomy.accountability.command.create_accountabilities.CreateAccountabilitiesCommandHandler;
import org.ouracademy.srtomy.accountability.command.create_accountability.CreateAccountabilityCommand;
import org.ouracademy.srtomy.accountability.command.delete_accountabilities_by_parent_and_types.DeleteAccountabilitiesByParentAndTypesCommand;
import org.ouracademy.srtomy.accountability.command.delete_accountabilities_by_parent_and_types.DeleteAccountabilitiesByParentAndTypesCommandHandler;
import org.ouracademy.srtomy.accountability.command.delete_accountabilities_by_parent_and_types.DeleteAccountabilitiesByParentAndTypesResult;
import org.ouracademy.srtomy.organization.command.OrganizationInfo;
import org.ouracademy.srtomy.organization.command.OrganizationWriter;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class UpdateLocationChildCommandHandlerImplTests {

  @Mock OrganizationWriter organizationWriter;
  @Mock DeleteAccountabilitiesByParentAndTypesCommandHandler deleteAccountabilitiesHandler;
  @Mock CreateAccountabilitiesCommandHandler createAccountabilitiesCommandHandler;

  UpdateLocationChildCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new UpdateLocationChildCommandHandlerImpl(
            organizationWriter,
            deleteAccountabilitiesHandler,
            createAccountabilitiesCommandHandler);
  }

  @Test
  void handleReplacesMembersAndLeadersAndUpdatesTheType() {
    var info = new OrganizationInfo("Jóvenes", "Nuevo", null, null);
    var groupId = PartyId.generate();
    var member1 = PartyId.generate();
    var leader = PartyId.generate();
    when(organizationWriter.editChild(groupId, info, Organization.Type.MINISTRY))
        .thenReturn(Optional.of(organization(groupId, "Jóvenes", Organization.Type.MINISTRY)));
    when(deleteAccountabilitiesHandler.handle(any()))
        .thenReturn(new DeleteAccountabilitiesByParentAndTypesResult());

    var result =
        handler.handle(
            new UpdateLocationChildCommand(
                groupId, info, Organization.Type.MINISTRY, List.of(member1), List.of(leader)));

    assertThat(result).isPresent();
    assertThat(result.get().organization().id()).isEqualTo(groupId);
    verify(deleteAccountabilitiesHandler)
        .handle(
            new DeleteAccountabilitiesByParentAndTypesCommand(
                groupId, List.of(IS_PART_OF, LEADER)));
    verify(createAccountabilitiesCommandHandler)
        .handle(
            new CreateAccountabilitiesCommand(
                List.of(
                    new CreateAccountabilityCommand(member1, groupId, IS_PART_OF),
                    new CreateAccountabilityCommand(leader, groupId, LEADER))));
  }

  @Test
  void handleReturnsEmptyAndWritesNothingWhenTheOrganizationIsNotAGroupOrMinistry() {
    var info = new OrganizationInfo("Sede", null, null, null);
    var id = PartyId.generate();
    when(organizationWriter.editChild(id, info, Organization.Type.GROUP))
        .thenReturn(Optional.empty());

    var result =
        handler.handle(
            new UpdateLocationChildCommand(
                id,
                info,
                Organization.Type.GROUP,
                List.of(PartyId.generate()),
                List.of(PartyId.generate())));

    assertThat(result).isEmpty();
    verify(deleteAccountabilitiesHandler, never()).handle(any());
    verify(createAccountabilitiesCommandHandler, never()).handle(any());
  }
}
