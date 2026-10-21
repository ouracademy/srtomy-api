package org.ouracademy.srtomy.auth.command.update_user_app_metadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.auth.gateway.AccessToken;
import org.ouracademy.srtomy.auth.gateway.Auth0Gateway;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class UpdateUserAppMetadataCommandHandlerImplTests {

  @Mock Auth0Gateway auth0Gateway;

  UpdateUserAppMetadataCommandHandlerImpl handler;

  @BeforeEach
  void setUp() {
    handler = new UpdateUserAppMetadataCommandHandlerImpl(auth0Gateway);
  }

  @Test
  void handleSendsTheOrganizationIdAsAppMetadataAndReportsSuccess() {
    var organizationId = PartyId.generate();
    var accessToken = new AccessToken();
    when(auth0Gateway.getAccessToken()).thenReturn(accessToken);
    when(auth0Gateway.updateAppMetadata(
            accessToken, "auth0|123", Map.of("orgId", organizationId.value())))
        .thenReturn(ResponseEntity.ok("{}"));

    var result = handler.handle(new UpdateUserAppMetadataCommand("auth0|123", organizationId));

    assertThat(result).isEqualTo(new UpdateUserAppMetadataResult(true));
    verify(auth0Gateway)
        .updateAppMetadata(accessToken, "auth0|123", Map.of("orgId", organizationId.value()));
  }

  @Test
  void handleReportsFailureWithoutThrowingWhenAuth0RespondsWithANon2xxStatus() {
    var organizationId = PartyId.generate();
    var accessToken = new AccessToken();
    when(auth0Gateway.getAccessToken()).thenReturn(accessToken);
    when(auth0Gateway.updateAppMetadata(
            accessToken, "auth0|123", Map.of("orgId", organizationId.value())))
        .thenReturn(ResponseEntity.status(HttpStatus.MULTIPLE_CHOICES).body(""));

    var result = handler.handle(new UpdateUserAppMetadataCommand("auth0|123", organizationId));

    assertThat(result).isEqualTo(new UpdateUserAppMetadataResult(false));
  }
}
