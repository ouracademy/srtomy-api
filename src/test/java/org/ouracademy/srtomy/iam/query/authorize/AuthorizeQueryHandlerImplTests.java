package org.ouracademy.srtomy.iam.query.authorize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.repository.RoleBindingRepository;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class AuthorizeQueryHandlerImplTests {

  @Mock RoleBindingRepository roleBindingRepository;
  @InjectMocks AuthorizeQueryHandlerImpl handler;

  @Test
  void grantsWhenAnyBindingOnTheNodeOrItsAncestorsHasThePermission() {
    var node = PartyId.generate();
    when(roleBindingRepository.findPermissions("u", List.of(node.value())))
        .thenReturn(List.of("org.read", "members.read"));

    var result = handler.handle(new AuthorizeQuery("u", node, Permission.MEMBERS_READ));

    assertThat(result.permission()).isEqualTo(Permission.MEMBERS_READ);
  }

  @Test
  void deniesWhenThePermissionIsMissing() {
    var node = PartyId.generate();
    when(roleBindingRepository.findPermissions("u", List.of(node.value())))
        .thenReturn(List.of("org.read"));

    assertThatThrownBy(() -> handler.handle(new AuthorizeQuery("u", node, Permission.ORG_UPDATE)))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void deniesWithoutQueryingWhenThereIsNoNode() {
    assertThatThrownBy(
            () -> handler.handle(new AuthorizeQuery("u", List.of(), Permission.MEMBERS_READ)))
        .isInstanceOf(AccessDeniedException.class);
    verifyNoInteractions(roleBindingRepository);
  }
}
