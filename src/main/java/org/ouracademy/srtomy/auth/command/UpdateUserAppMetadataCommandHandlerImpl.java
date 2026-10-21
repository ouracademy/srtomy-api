package org.ouracademy.srtomy.auth.command;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.auth.gateway.Auth0Gateway;
import org.springframework.stereotype.Service;

/**
 * Stores the user's organization id in their Auth0 app_metadata. Reports the outcome without
 * throwing: the caller decides what a failed update means for its own flow.
 */
@Service
@RequiredArgsConstructor
class UpdateUserAppMetadataCommandHandlerImpl implements UpdateUserAppMetadataCommandHandler {

  private final Auth0Gateway auth0Gateway;

  // TODO: use saga to update user metadata (to make the db transaction more short). Callers
  // currently invoke this inside their own DB transaction, keeping it open across this external
  // HTTP call to Auth0.
  @Override
  public UpdateUserAppMetadataResult handle(UpdateUserAppMetadataCommand command) {
    var accessToken = auth0Gateway.getAccessToken();
    var userMetadata = Map.of("orgId", command.organizationId().value());
    var response = auth0Gateway.updateAppMetadata(accessToken, command.userId(), userMetadata);
    return new UpdateUserAppMetadataResult(response.getStatusCode().is2xxSuccessful());
  }
}
