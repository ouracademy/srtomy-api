package org.ouracademy.srtomy.iam.query.list_access;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.iam.domain.RoleBindingId;
import org.ouracademy.srtomy.iam.repository.RoleBindingRepository;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.user.domain.UserId;
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
                    new RoleBindingId(entry.getBindingId()),
                    new UserId(entry.getUserId()),
                    entry.getEmail(),
                    new IamRoleId(entry.getRoleId()),
                    entry.getRoleName(),
                    new PartyId(entry.getOrganizationId()),
                    entry.getOrganizationName(),
                    !entry.getOrganizationId().equals(query.organizationId().value())))
        .toList();
  }
}
