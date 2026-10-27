package org.ouracademy.ovejafiel.iam.query.list_access;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.iam.repository.RoleBindingRepository;
import org.ouracademy.ovejafiel.iam.repository.RoleBindingRepository.AccessEntry;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class ListAccessQueryHandlerImplTests {

  RoleBindingRepository repository = mock(RoleBindingRepository.class);

  AccessEntry entry(UUID organizationId, String email) {
    var entry = mock(AccessEntry.class);
    when(entry.getBindingId()).thenReturn(UUID.randomUUID());
    when(entry.getUserId()).thenReturn(UUID.randomUUID());
    when(entry.getEmail()).thenReturn(email);
    when(entry.getRoleId()).thenReturn(UUID.randomUUID());
    when(entry.getRoleName()).thenReturn("Líder");
    when(entry.getOrganizationId()).thenReturn(organizationId);
    when(entry.getOrganizationName()).thenReturn("Nodo");
    when(entry.getSource()).thenReturn("DIRECT");
    return entry;
  }

  @Test
  void bindingsMadeOnAnAncestorAreInherited() {
    var node = PartyId.generate();
    var ancestor = UUID.randomUUID();
    var direct = entry(node.value(), "a@x.org");
    var inherited = entry(ancestor, "b@x.org");
    when(repository.findAccess(node.value())).thenReturn(List.of(direct, inherited));

    var access = new ListAccessQueryHandlerImpl(repository).handle(new ListAccessQuery(node));

    assertThat(access).extracting(AccessEntryResult::inherited).containsExactly(false, true);
    assertThat(access.get(1).organizationId().value()).isEqualTo(ancestor);
  }
}
