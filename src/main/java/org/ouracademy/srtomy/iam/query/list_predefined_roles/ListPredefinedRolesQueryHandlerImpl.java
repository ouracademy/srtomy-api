package org.ouracademy.srtomy.iam.query.list_predefined_roles;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.iam.query.RoleResult;
import org.ouracademy.srtomy.iam.repository.IamRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ListPredefinedRolesQueryHandlerImpl implements ListPredefinedRolesQueryHandler {

  private final IamRoleRepository iamRoleRepository;

  @Override
  @Transactional(readOnly = true)
  public List<RoleResult> handle(ListPredefinedRolesQuery query) {
    return iamRoleRepository.findPredefined().stream().map(RoleResult::from).toList();
  }
}
