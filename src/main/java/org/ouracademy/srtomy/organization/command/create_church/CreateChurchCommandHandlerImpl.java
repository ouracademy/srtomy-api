package org.ouracademy.srtomy.organization.command.create_church;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.auth.command.update_user_app_metadata.UpdateUserAppMetadataCommand;
import org.ouracademy.srtomy.auth.command.update_user_app_metadata.UpdateUserAppMetadataCommandHandler;
import org.ouracademy.srtomy.organization.command.OrganizationWriter;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CreateChurchCommandHandlerImpl implements CreateChurchCommandHandler {

  private final OrganizationWriter organizationWriter;
  private final UpdateUserAppMetadataCommandHandler updateUserAppMetadataCommandHandler;

  /**
   * Fails with {@link IllegalStateException} (rolling the church back) when Auth0 does not accept
   * the app_metadata update.
   */
  // TODO: use saga to update user metadata (to make the db transaction more short)
  @Override
  @Transactional
  public CreateChurchResult handle(CreateChurchCommand command) {
    var church =
        organizationWriter.create(command.info(), Organization.Type.CHURCH, command.parentId());
    var result =
        updateUserAppMetadataCommandHandler.handle(
            new UpdateUserAppMetadataCommand(command.userId(), church.getId()));
    if (!result.success()) {
      var userMetadata = Map.of("orgId", church.getId().value());
      throw new IllegalStateException(
          "Couldn't update user metadata:" + command.userId() + "," + userMetadata);
    }
    return new CreateChurchResult(FindOrganizationResult.from(church));
  }
}
