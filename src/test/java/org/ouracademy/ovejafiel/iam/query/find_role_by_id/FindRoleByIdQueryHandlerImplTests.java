package org.ouracademy.ovejafiel.iam.query.find_role_by_id;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.iam.domain.IamRole;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.repository.IamRoleRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;

class FindRoleByIdQueryHandlerImplTests {

  IamRoleRepository repository = mock(IamRoleRepository.class);
  FindRoleByIdQueryHandlerImpl handler = new FindRoleByIdQueryHandlerImpl(repository);

  @Test
  void mapsAnExistingRole() {
    var role = IamRole.custom(mock(Organization.class), "Tesorero", null, List.of("org.read"));
    when(repository.findById(role.getId())).thenReturn(Optional.of(role));

    assertThat(handler.handle(new FindRoleByIdQuery(role.getId())))
        .hasValueSatisfying(result -> assertThat(result.name()).isEqualTo("Tesorero"));
  }

  @Test
  void returnsEmptyForAnUnknownRole() {
    assertThat(handler.handle(new FindRoleByIdQuery(IamRoleId.generate()))).isEmpty();
  }
}
