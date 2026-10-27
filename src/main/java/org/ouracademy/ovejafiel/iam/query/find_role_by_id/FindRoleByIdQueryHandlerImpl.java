package org.ouracademy.ovejafiel.iam.query.find_role_by_id;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.iam.query.RoleResult;
import org.ouracademy.ovejafiel.iam.repository.IamRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class FindRoleByIdQueryHandlerImpl implements FindRoleByIdQueryHandler {

  private final IamRoleRepository iamRoleRepository;

  @Override
  @Transactional(readOnly = true)
  public Optional<RoleResult> handle(FindRoleByIdQuery query) {
    return iamRoleRepository.findById(query.roleId()).map(RoleResult::from);
  }
}
