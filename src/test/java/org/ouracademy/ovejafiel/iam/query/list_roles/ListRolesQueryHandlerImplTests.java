package org.ouracademy.ovejafiel.iam.query.list_roles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.iam.domain.IamRole;
import org.ouracademy.ovejafiel.iam.repository.IamRoleRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class ListRolesQueryHandlerImplTests {

  IamRoleRepository repository = mock(IamRoleRepository.class);

  @Test
  void returnsTheRolesAvailableToTheChurchAsResults() {
    var church = PartyId.generate();
    var organization = mock(Organization.class);
    when(organization.getId()).thenReturn(church);
    var role = IamRole.custom(organization, "Tesorero", "d", List.of("org.read"));
    when(repository.findAvailableTo(church)).thenReturn(List.of(role));

    var roles = new ListRolesQueryHandlerImpl(repository).handle(new ListRolesQuery(church));

    assertThat(roles).hasSize(1);
    assertThat(roles.get(0).churchId()).isEqualTo(church);
    assertThat(roles.get(0).predefined()).isFalse();
  }
}
