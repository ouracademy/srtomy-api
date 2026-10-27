package org.ouracademy.ovejafiel.iam.query.get_effective_permissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.repository.RoleBindingRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;

@ExtendWith(MockitoExtension.class)
class GetEffectivePermissionsQueryHandlerImplTests {

  @Mock RoleBindingRepository roleBindingRepository;
  @InjectMocks GetEffectivePermissionsQueryHandlerImpl handler;

  @Test
  void mapsStoredValuesToTheCatalogAndIgnoresUnknownOnes() {
    var node = PartyId.generate();
    when(roleBindingRepository.findPermissions("u", List.of(node.value())))
        .thenReturn(List.of("events.read", "retired.permission"));

    var result = handler.handle(new GetEffectivePermissionsQuery("u", node));

    assertThat(result.permissions()).containsExactly(Permission.EVENTS_READ);
  }
}
