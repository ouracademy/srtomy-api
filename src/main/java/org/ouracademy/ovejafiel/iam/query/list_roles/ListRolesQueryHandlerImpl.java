package org.ouracademy.ovejafiel.iam.query.list_roles;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.iam.query.RoleResult;
import org.ouracademy.ovejafiel.iam.repository.IamRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ListRolesQueryHandlerImpl implements ListRolesQueryHandler {

  private final IamRoleRepository iamRoleRepository;

  @Override
  @Transactional(readOnly = true)
  public List<RoleResult> handle(ListRolesQuery query) {
    return iamRoleRepository.findAvailableTo(query.churchId()).stream()
        .map(RoleResult::from)
        .toList();
  }
}
