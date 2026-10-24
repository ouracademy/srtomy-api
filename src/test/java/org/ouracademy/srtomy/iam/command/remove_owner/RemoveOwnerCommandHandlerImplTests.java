package org.ouracademy.srtomy.iam.command.remove_owner;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.srtomy.iam.command.GrantRules;
import org.ouracademy.srtomy.iam.domain.IamRole;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.iam.domain.RoleBinding;
import org.ouracademy.srtomy.iam.domain.RoleBindingId;
import org.ouracademy.srtomy.iam.repository.RoleBindingRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;

@ExtendWith(MockitoExtension.class)
class RemoveOwnerCommandHandlerImplTests {

  @Mock RoleBindingRepository roleBindingRepository;
  @Mock GrantRules grantRules;
  @Mock FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  @InjectMocks RemoveOwnerCommandHandlerImpl handler;

  PartyId node = PartyId.generate();
  RoleBindingId bindingId = RoleBindingId.generate();
  RoleBinding binding = mock(RoleBinding.class);

  void churchIs(PartyId church) {
    lenient()
        .when(findChurchOfOrganizationQueryHandler.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
  }

  void bindingOf(IamRoleId roleId, PartyId organizationId) {
    var role = mock(IamRole.class);
    var organization = mock(Organization.class);
    lenient().when(role.getId()).thenReturn(roleId);
    lenient().when(organization.getId()).thenReturn(organizationId);
    lenient().when(binding.getRole()).thenReturn(role);
    lenient().when(binding.getOrganization()).thenReturn(organization);
    when(roleBindingRepository.findById(bindingId)).thenReturn(Optional.of(binding));
  }

  @Test
  void aBindingThatIsNotOwnerIsNotFound() {
    bindingOf(PredefinedRoles.LEADER, node);
    churchIs(node);

    assertThatThrownBy(() -> handler.handle(new RemoveOwnerCommand("me", bindingId, node)))
        .isInstanceOf(NoSuchElementException.class);
    verify(roleBindingRepository, never()).delete(any());
  }

  @Test
  void anOwnerBindingOfAnotherChurchIsNotFound() {
    bindingOf(PredefinedRoles.OWNER, PartyId.generate());
    churchIs(node);

    assertThatThrownBy(() -> handler.handle(new RemoveOwnerCommand("me", bindingId, node)))
        .isInstanceOf(NoSuchElementException.class);
    verify(roleBindingRepository, never()).delete(any());
  }

  @Test
  void theLastOwnerBindingIsKept() {
    var church = node;
    bindingOf(PredefinedRoles.OWNER, node);
    churchIs(node);
    when(roleBindingRepository.countByRoleInChurch(PredefinedRoles.OWNER.value(), church.value()))
        .thenReturn(1L);

    assertThatThrownBy(() -> handler.handle(new RemoveOwnerCommand("me", bindingId, node)))
        .isInstanceOf(DomainValidationException.class);
    verify(roleBindingRepository, never()).delete(any());
  }

  @Test
  void anOwnerBindingIsDeletedWhenAnotherOneRemains() {
    var church = node;
    bindingOf(PredefinedRoles.OWNER, node);
    churchIs(node);
    when(roleBindingRepository.countByRoleInChurch(PredefinedRoles.OWNER.value(), church.value()))
        .thenReturn(2L);

    handler.handle(new RemoveOwnerCommand("me", bindingId, node));

    verify(roleBindingRepository).delete(binding);
  }
}
