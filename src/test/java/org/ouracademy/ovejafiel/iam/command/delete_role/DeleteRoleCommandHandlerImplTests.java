package org.ouracademy.ovejafiel.iam.command.delete_role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.iam.domain.IamRole;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.repository.IamRoleRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

class DeleteRoleCommandHandlerImplTests {

  IamRoleRepository repository = mock(IamRoleRepository.class);
  DeleteRoleCommandHandlerImpl handler = new DeleteRoleCommandHandlerImpl(repository);

  @Test
  void marksACustomRoleDeleted() {
    var role = IamRole.custom(mock(Organization.class), "Tesorero", null, List.of("org.read"));
    when(repository.findById(role.getId())).thenReturn(Optional.of(role));

    handler.handle(new DeleteRoleCommand(role.getId()));

    assertThat(role.getDeletedTime()).isNotNull();
  }

  @Test
  void rejectsPredefinedRolesAndUnknownIds() {
    var predefined = mock(IamRole.class);
    when(predefined.isPredefined()).thenReturn(true);
    var id = IamRoleId.generate();
    when(repository.findById(id)).thenReturn(Optional.of(predefined));

    assertThatThrownBy(() -> handler.handle(new DeleteRoleCommand(id)))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> handler.handle(new DeleteRoleCommand(IamRoleId.generate())))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void aLevelUsedByPositionsCannotBeDeleted() {
    var role = IamRole.custom(mock(Organization.class), "Tesorero", null, List.of("org.read"));
    when(repository.findById(role.getId())).thenReturn(Optional.of(role));
    when(repository.countPositionsUsing(role.getId().value())).thenReturn(2L);

    assertThatThrownBy(() -> handler.handle(new DeleteRoleCommand(role.getId())))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("2 cargo");
    assertThat(role.getDeletedTime()).isNull();
  }
}
