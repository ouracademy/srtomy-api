package org.ouracademy.ovejafiel.iam.command.update_role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.iam.command.RoleRules;
import org.ouracademy.ovejafiel.iam.domain.IamRole;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.repository.IamRoleRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

class UpdateRoleCommandHandlerImplTests {

  IamRoleRepository repository = mock(IamRoleRepository.class);
  RoleRules rules = mock(RoleRules.class);
  UpdateRoleCommandHandlerImpl handler = new UpdateRoleCommandHandlerImpl(repository, rules);

  @Test
  void changesACustomRole() {
    var church = mock(Organization.class);
    when(church.getId()).thenReturn(PartyId.generate());
    var role = IamRole.custom(church, "Tesorero", null, List.of("org.read"));
    when(repository.findById(role.getId())).thenReturn(Optional.of(role));
    when(rules.requireAvailableName(any(), any(), any())).thenReturn("Nuevo");
    when(rules.requirePermissions(any(), any(), any())).thenReturn(List.of("events.read"));

    var result =
        handler.handle(
            new UpdateRoleCommand("me", role.getId(), "Nuevo", null, List.of("events.read")));

    assertThat(result.role().name()).isEqualTo("Nuevo");
    assertThat(role.getPermissions()).containsExactly("events.read");
  }

  @Test
  void rejectsPredefinedRolesAndUnknownIds() {
    var predefined = mock(IamRole.class);
    when(predefined.isPredefined()).thenReturn(true);
    var id = IamRoleId.generate();
    when(repository.findById(id)).thenReturn(Optional.of(predefined));
    var missing = IamRoleId.generate();
    when(repository.findById(missing)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(new UpdateRoleCommand("me", id, "X", null, List.of())))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(
            () -> handler.handle(new UpdateRoleCommand("me", missing, "X", null, List.of())))
        .isInstanceOf(NoSuchElementException.class);
  }
}
