package org.ouracademy.srtomy.organization.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.accountability.command.DeleteAccountabilitiesForPartyCommand;
import org.ouracademy.srtomy.accountability.command.DeleteAccountabilitiesForPartyCommandHandler;
import org.ouracademy.srtomy.organization.command.DeleteOrganizationResult.Outcome;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.repository.OrganizationRepository;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class DeleteOrganizationCommandHandlerImplTests {

  @Mock OrganizationRepository organizationRepository;
  @Mock DeleteAccountabilitiesForPartyCommandHandler deleteAccountabilitiesForPartyCommandHandler;

  DeleteOrganizationCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new DeleteOrganizationCommandHandlerImpl(
            organizationRepository, deleteAccountabilitiesForPartyCommandHandler);
  }

  @Test
  void handleDeletesAGroupAndItsAccountabilities() {
    var id = PartyId.generate();
    when(organizationRepository.findById(id))
        .thenReturn(Optional.of(organization(id, "Youth", Organization.Type.GROUP)));

    var result = handler.handle(new DeleteOrganizationCommand(id));

    assertThat(result.outcome()).isEqualTo(Outcome.DELETED);
    verify(deleteAccountabilitiesForPartyCommandHandler)
        .handle(new DeleteAccountabilitiesForPartyCommand(id));
    verify(organizationRepository).deleteById(id);
  }

  @Test
  void handleRefusesToDeleteALocation() {
    var id = PartyId.generate();
    when(organizationRepository.findById(id))
        .thenReturn(Optional.of(organization(id, "Downtown", Organization.Type.LOCATION)));

    var result = handler.handle(new DeleteOrganizationCommand(id));

    assertThat(result.outcome()).isEqualTo(Outcome.NOT_DELETABLE);
    verifyNoInteractions(deleteAccountabilitiesForPartyCommandHandler);
    verify(organizationRepository, never()).deleteById(id);
  }

  @Test
  void handleReportsNotFoundForAnUnknownId() {
    var id = PartyId.generate();
    when(organizationRepository.findById(id)).thenReturn(Optional.empty());

    assertThat(handler.handle(new DeleteOrganizationCommand(id)).outcome())
        .isEqualTo(Outcome.NOT_FOUND);
  }
}
