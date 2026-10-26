package org.ouracademy.ovejafiel.iam.query.list_access;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.domain.RoleBindingId;
import org.ouracademy.ovejafiel.iam.repository.RoleBindingRepository;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ListAccessQueryHandlerImpl implements ListAccessQueryHandler {

  private final RoleBindingRepository roleBindingRepository;

  @Override
  @Transactional(readOnly = true)
  public List<AccessEntryResult> handle(ListAccessQuery query) {
    return roleBindingRepository.findAccess(query.organizationId().value()).stream()
        .map(
            entry ->
                new AccessEntryResult(
                    entry.getBindingId() == null ? null : new RoleBindingId(entry.getBindingId()),
                    new UserId(entry.getUserId()),
                    entry.getEmail(),
                    entry.getRoleId() == null ? null : new IamRoleId(entry.getRoleId()),
                    entry.getRoleName(),
                    new PartyId(entry.getOrganizationId()),
                    entry.getOrganizationName(),
                    !entry.getOrganizationId().equals(query.organizationId().value()),
                    AccessEntryResult.Source.valueOf(entry.getSource()),
                    entry.getPositionName(),
                    entry.getPermissionCount()))
        .toList();
  }
}
