package org.ouracademy.srtomy.iam.command.create_role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.iam.command.RoleRules;
import org.ouracademy.srtomy.iam.domain.IamRole;
import org.ouracademy.srtomy.iam.repository.IamRoleRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

class CreateRoleCommandHandlerImplTests {

  IamRoleRepository repository = mock(IamRoleRepository.class);
  RoleRules rules = mock(RoleRules.class);
  EntityManager entityManager = mock(EntityManager.class);
  PartyId church = PartyId.generate();

  @Test
  void savesACustomRoleWithTheValidatedValues() {
    when(rules.requireAvailableName(any(), any(), any())).thenReturn("Tesorero");
    when(rules.requirePermissions("me", church, List.of("org.read", "org.read")))
        .thenReturn(List.of("org.read"));
    when(entityManager.getReference(Organization.class, church))
        .thenReturn(mock(Organization.class));
    when(repository.save(any(IamRole.class))).thenAnswer(call -> call.getArgument(0));

    var created =
        new CreateRoleCommandHandlerImpl(repository, rules, entityManager)
            .handle(
                new CreateRoleCommand(
                    "me", church, " Tesorero ", "d", List.of("org.read", "org.read")))
            .role();

    assertThat(created.name()).isEqualTo("Tesorero");
    assertThat(created.permissions()).containsExactly("org.read");
    assertThat(created.predefined()).isFalse();
  }
}
