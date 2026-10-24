package org.ouracademy.srtomy.iam.command;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.iam.repository.RoleBindingRepository;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class GrantRulesTests {

  @Mock RoleBindingRepository roleBindingRepository;
  @InjectMocks GrantRules rules;

  PartyId node = PartyId.generate();

  @Test
  void aRoleTheCallerHoldsCanBeGranted() {
    when(roleBindingRepository.holdsRole(any(), any(), any())).thenReturn(true);

    assertThatCode(
            () ->
                rules.requireCanGrant(
                    "me", PredefinedRoles.LEADER, List.of("org.read", "iam.manage"), node))
        .doesNotThrowAnyException();
  }

  @Test
  void aRoleWhosePermissionsTheCallerAlreadyHasCanBeGranted() {
    when(roleBindingRepository.findPermissions(any(), any()))
        .thenReturn(List.of("org.read", "events.read", "iam.manage"));

    assertThatCode(
            () ->
                rules.requireCanGrant(
                    "me", PredefinedRoles.MEMBER, List.of("org.read", "events.read"), node))
        .doesNotThrowAnyException();
  }

  @Test
  void aRoleWithPermissionsTheCallerLacksIsDenied() {
    when(roleBindingRepository.findPermissions(any(), any())).thenReturn(List.of("org.read"));

    assertThatThrownBy(
            () ->
                rules.requireCanGrant(
                    "me", PredefinedRoles.MEMBER, List.of("org.read", "events.read"), node))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void ownerCanOnlyBeGrantedOrRevokedByAnOwner() {
    when(roleBindingRepository.holdsRole(any(), any(), any())).thenReturn(false);

    assertThatThrownBy(() -> rules.requireCanGrant("me", PredefinedRoles.OWNER, List.of(), node))
        .isInstanceOf(AccessDeniedException.class);
    assertThatThrownBy(() -> rules.requireCanRevoke("me", PredefinedRoles.OWNER, node))
        .isInstanceOf(AccessDeniedException.class);

    when(roleBindingRepository.holdsRole(any(), any(), any())).thenReturn(true);
    assertThatCode(() -> rules.requireCanGrant("me", PredefinedRoles.OWNER, List.of(), node))
        .doesNotThrowAnyException();
    assertThatCode(() -> rules.requireCanRevoke("me", PredefinedRoles.OWNER, node))
        .doesNotThrowAnyException();
  }

  @Test
  void revokingAnyOtherRoleNeedsNothingExtra() {
    assertThatCode(() -> rules.requireCanRevoke("me", PredefinedRoles.LEADER, node))
        .doesNotThrowAnyException();
  }
}
