package org.ouracademy.ovejafiel.iam.query.expand_permissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.iam.domain.IamRole;
import org.ouracademy.ovejafiel.iam.domain.PredefinedRoles;
import org.ouracademy.ovejafiel.iam.repository.IamRoleRepository;

class ExpandPermissionsQueryHandlerImplTests {

  IamRoleRepository repository = mock(IamRoleRepository.class);
  ExpandPermissionsQueryHandlerImpl handler = new ExpandPermissionsQueryHandlerImpl(repository);

  IamRole role(String... permissions) {
    var role = mock(IamRole.class);
    when(role.getPermissions()).thenReturn(List.of(permissions));
    return role;
  }

  @Test
  void unitesTheRolesAndTheLoosePermissionsWithoutRepeating() {
    var viewPeople = role("org.read", "members.read");
    var events = role("org.read", "events.read", "events.write");
    var roles = Set.of(PredefinedRoles.VIEW_PEOPLE, PredefinedRoles.EVENTS_MANAGER);
    when(repository.findAllById(roles)).thenReturn(List.of(viewPeople, events));

    var result = handler.handle(new ExpandPermissionsQuery(roles, Set.of("members.write")));

    assertThat(result.permissions())
        .containsExactly(
            "events.read", "events.write", "members.read", "members.write", "org.read");
  }

  @Test
  void withoutRolesItReturnsTheLoosePermissionsSorted() {
    var result =
        handler.handle(new ExpandPermissionsQuery(Set.of(), Set.of("org.read", "iam.read")));

    assertThat(result.permissions()).containsExactly("iam.read", "org.read");
    verifyNoInteractions(repository);
  }

  @Test
  void aLabelGivesNothing() {
    assertThat(handler.handle(new ExpandPermissionsQuery(Set.of(), null)).permissions()).isEmpty();
  }
}
