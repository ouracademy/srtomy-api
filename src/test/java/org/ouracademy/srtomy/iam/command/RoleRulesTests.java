package org.ouracademy.srtomy.iam.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.iam.repository.IamRoleRepository;
import org.ouracademy.srtomy.iam.repository.RoleBindingRepository;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class RoleRulesTests {

  @Mock IamRoleRepository iamRoleRepository;
  @Mock RoleBindingRepository roleBindingRepository;
  @InjectMocks RoleRules rules;

  PartyId church = PartyId.generate();

  @Test
  void permissionsAreDeduplicated() {
    when(roleBindingRepository.findPermissions(any(), any()))
        .thenReturn(List.of("org.read", "events.read"));

    assertThat(rules.requirePermissions("me", church, List.of("org.read", "org.read")))
        .containsExactly("org.read");
  }

  @Test
  void emptyOrUnknownPermissionsAreRejected() {
    assertThatThrownBy(() -> rules.requirePermissions("me", church, List.of()))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> rules.requirePermissions("me", church, null))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> rules.requirePermissions("me", church, List.of("org.fly")))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("org.fly");
  }

  @Test
  void permissionsTheEditorLacksAreDenied() {
    when(roleBindingRepository.findPermissions(any(), any())).thenReturn(List.of("org.read"));

    assertThatThrownBy(() -> rules.requirePermissions("me", church, List.of("events.write")))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void theNameIsTrimmedAndMustBeFree() {
    var id = IamRoleId.generate();
    when(iamRoleRepository.nameTaken(church, "Tesorero", id)).thenReturn(false);
    when(iamRoleRepository.nameTaken(church, "Líder", id)).thenReturn(true);

    assertThat(rules.requireAvailableName(church, "  Tesorero ", id)).isEqualTo("Tesorero");
    assertThatThrownBy(() -> rules.requireAvailableName(church, "Líder", id))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> rules.requireAvailableName(church, "  ", id))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> rules.requireAvailableName(church, null, id))
        .isInstanceOf(DomainValidationException.class);
  }
}
