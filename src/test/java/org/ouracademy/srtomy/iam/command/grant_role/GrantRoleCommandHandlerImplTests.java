package org.ouracademy.srtomy.iam.command.grant_role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.iam.domain.IamRole;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.iam.domain.RoleBinding;
import org.ouracademy.srtomy.iam.repository.IamRoleRepository;
import org.ouracademy.srtomy.iam.repository.RoleBindingRepository;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.user.domain.UserId;

@ExtendWith(MockitoExtension.class)
class GrantRoleCommandHandlerImplTests {

  @Mock RoleBindingRepository roleBindingRepository;
  @Mock IamRoleRepository iamRoleRepository;
  @Mock EntityManager entityManager;
  @InjectMocks GrantRoleCommandHandlerImpl handler;

  @Test
  void savesANewBinding() {
    var command =
        new GrantRoleCommand(UserId.generate(), PredefinedRoles.OWNER, PartyId.generate(), null);
    when(iamRoleRepository.findById(PredefinedRoles.OWNER))
        .thenReturn(Optional.of(mock(IamRole.class)));

    assertThat(handler.handle(command).created()).isTrue();
    verify(roleBindingRepository).save(any(RoleBinding.class));
  }

  @Test
  void grantingTheSameBindingTwiceIsANoOp() {
    var command =
        new GrantRoleCommand(UserId.generate(), PredefinedRoles.LEADER, PartyId.generate(), null);
    when(roleBindingRepository.existsByUserIdAndRoleIdAndOrganizationId(
            command.userId(), command.roleId(), command.organizationId()))
        .thenReturn(true);

    assertThat(handler.handle(command).created()).isFalse();
    verify(roleBindingRepository, never()).save(any());
  }
}
