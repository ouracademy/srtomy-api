package org.ouracademy.srtomy.organization.command.create_church;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.auth.command.update_user_app_metadata.UpdateUserAppMetadataCommand;
import org.ouracademy.srtomy.auth.command.update_user_app_metadata.UpdateUserAppMetadataCommandHandler;
import org.ouracademy.srtomy.auth.command.update_user_app_metadata.UpdateUserAppMetadataResult;
import org.ouracademy.srtomy.organization.command.OrganizationInfo;
import org.ouracademy.srtomy.organization.command.OrganizationWriter;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class CreateChurchCommandHandlerImplTests {

  static final OrganizationInfo INFO = new OrganizationInfo("Church", "desc", "addr", "123");

  @Mock OrganizationWriter organizationWriter;
  @Mock UpdateUserAppMetadataCommandHandler updateUserAppMetadataCommandHandler;

  CreateChurchCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler =
        new CreateChurchCommandHandlerImpl(organizationWriter, updateUserAppMetadataCommandHandler);
  }

  @Test
  void handleCreatesTheChurchAndStoresItsIdInTheUsersAppMetadata() {
    var churchId = PartyId.generate();
    var church = organization(churchId, "Church", Organization.Type.CHURCH);
    when(organizationWriter.create(INFO, Organization.Type.CHURCH, null)).thenReturn(church);
    when(updateUserAppMetadataCommandHandler.handle(
            new UpdateUserAppMetadataCommand("auth0|123", churchId)))
        .thenReturn(new UpdateUserAppMetadataResult(true));

    var result = handler.handle(new CreateChurchCommand(INFO, null, "auth0|123"));

    assertThat(result.organization().id()).isEqualTo(churchId);
    assertThat(result.organization().type()).isEqualTo(Organization.Type.CHURCH);
    verify(updateUserAppMetadataCommandHandler)
        .handle(new UpdateUserAppMetadataCommand("auth0|123", churchId));
  }

  @Test
  void handleThrowsWhenAuth0RejectsTheAppMetadataUpdate() {
    var churchId = PartyId.generate();
    var church = organization(churchId, "Church", Organization.Type.CHURCH);
    when(organizationWriter.create(INFO, Organization.Type.CHURCH, null)).thenReturn(church);
    when(updateUserAppMetadataCommandHandler.handle(
            new UpdateUserAppMetadataCommand("auth0|123", churchId)))
        .thenReturn(new UpdateUserAppMetadataResult(false));

    assertThatThrownBy(() -> handler.handle(new CreateChurchCommand(INFO, null, "auth0|123")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Couldn't update user metadata:auth0|123,{orgId=" + churchId.value() + "}");
  }
}
