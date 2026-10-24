package org.ouracademy.srtomy.iam.query.find_accessible_organizations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.iam.repository.RoleBindingRepository;
import org.ouracademy.srtomy.iam.repository.RoleBindingRepository.BoundRole;
import org.ouracademy.srtomy.iam.repository.RoleBindingRepository.VisibleOrganization;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class FindAccessibleOrganizationsQueryHandlerImplTests {

  @Mock RoleBindingRepository roleBindingRepository;
  @InjectMocks FindAccessibleOrganizationsQueryHandlerImpl handler;

  @Test
  void attachesTheRolesBoundOnEachNode() {
    var church = UUID.randomUUID();
    var group = UUID.randomUUID();
    var nodes =
        List.of(
            node(church, "Iglesia", "CHURCH", null, false),
            node(group, "Coro", "GROUP", church, true));
    when(roleBindingRepository.findVisibleOrganizations("u")).thenReturn(nodes);
    var bound = mock(BoundRole.class);
    when(bound.getOrganizationId()).thenReturn(group);
    when(bound.getRoleName()).thenReturn("Líder");
    when(roleBindingRepository.findBoundRoles("u")).thenReturn(List.of(bound));

    var result = handler.handle(new FindAccessibleOrganizationsQuery("u"));

    assertThat(result)
        .containsExactly(
            new AccessibleOrganizationResult(
                new PartyId(church), "Iglesia", Organization.Type.CHURCH, null, false, List.of()),
            new AccessibleOrganizationResult(
                new PartyId(group),
                "Coro",
                Organization.Type.GROUP,
                new PartyId(church),
                true,
                List.of("Líder")));
  }

  static VisibleOrganization node(
      UUID id, String name, String type, UUID parentId, boolean accessible) {
    var node = mock(VisibleOrganization.class);
    when(node.getId()).thenReturn(id);
    when(node.getName()).thenReturn(name);
    when(node.getType()).thenReturn(type);
    when(node.getParentId()).thenReturn(parentId);
    when(node.getAccessible()).thenReturn(accessible);
    return node;
  }
}
