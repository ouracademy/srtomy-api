package org.ouracademy.srtomy.iam.command.revoke_access;

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
class RevokeAccessCommandHandlerImplTests {

  @Mock RoleBindingRepository roleBindingRepository;
  @Mock GrantRules grantRules;
  @Mock FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  @InjectMocks RevokeAccessCommandHandlerImpl handler;

  PartyId node = PartyId.generate();
  RoleBindingId bindingId = RoleBindingId.generate();
  RoleBinding binding = mock(RoleBinding.class);

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
  void deletesADirectBinding() {
    bindingOf(PredefinedRoles.LEADER, node);

    handler.handle(new RevokeAccessCommand("me", bindingId, node));

    verify(roleBindingRepository).delete(binding);
  }

  @Test
  void aBindingMadeElsewhereIsNotFound() {
    bindingOf(PredefinedRoles.LEADER, PartyId.generate());

    assertThatThrownBy(() -> handler.handle(new RevokeAccessCommand("me", bindingId, node)))
        .isInstanceOf(NoSuchElementException.class);
    verify(roleBindingRepository, never()).delete(any());
  }

  @Test
  void theLastOwnerBindingIsKept() {
    var church = PartyId.generate();
    bindingOf(PredefinedRoles.OWNER, node);
    when(findChurchOfOrganizationQueryHandler.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    when(roleBindingRepository.countByRoleInChurch(PredefinedRoles.OWNER.value(), church.value()))
        .thenReturn(1L);

    assertThatThrownBy(() -> handler.handle(new RevokeAccessCommand("me", bindingId, node)))
        .isInstanceOf(DomainValidationException.class);
    verify(roleBindingRepository, never()).delete(any());
  }

  @Test
  void anOwnerBindingIsDeletedWhenAnotherOneRemains() {
    var church = PartyId.generate();
    bindingOf(PredefinedRoles.OWNER, node);
    when(findChurchOfOrganizationQueryHandler.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    when(roleBindingRepository.countByRoleInChurch(PredefinedRoles.OWNER.value(), church.value()))
        .thenReturn(2L);

    handler.handle(new RevokeAccessCommand("me", bindingId, node));

    verify(roleBindingRepository).delete(binding);
  }
}
