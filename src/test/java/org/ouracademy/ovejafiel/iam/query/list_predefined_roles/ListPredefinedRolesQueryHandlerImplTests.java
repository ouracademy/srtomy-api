package org.ouracademy.ovejafiel.iam.query.list_predefined_roles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.iam.domain.IamRole;
import org.ouracademy.ovejafiel.iam.domain.PredefinedRoles;
import org.ouracademy.ovejafiel.iam.query.RoleResult;
import org.ouracademy.ovejafiel.iam.repository.IamRoleRepository;

class ListPredefinedRolesQueryHandlerImplTests {

  IamRoleRepository repository = mock(IamRoleRepository.class);

  IamRole role(org.ouracademy.ovejafiel.iam.domain.IamRoleId id, String name) {
    var role = mock(IamRole.class);
    when(role.getId()).thenReturn(id);
    when(role.getName()).thenReturn(name);
    when(role.getPermissions()).thenReturn(List.of("org.read"));
    when(role.isPredefined()).thenReturn(true);
    return role;
  }

  @Test
  void listsThePredefinedLevelsAndOnlyOwnerIsNotAssignable() {
    var owner = role(PredefinedRoles.OWNER, "Propietario");
    var leader = role(PredefinedRoles.LEADER, "Líder");
    when(repository.findPredefined()).thenReturn(List.of(owner, leader));

    var result =
        new ListPredefinedRolesQueryHandlerImpl(repository).handle(new ListPredefinedRolesQuery());

    assertThat(result).extracting(RoleResult::name).containsExactly("Propietario", "Líder");
    assertThat(result).extracting(RoleResult::assignable).containsExactly(false, true);
    assertThat(result).extracting(RoleResult::predefined).containsOnly(true);
  }
}
