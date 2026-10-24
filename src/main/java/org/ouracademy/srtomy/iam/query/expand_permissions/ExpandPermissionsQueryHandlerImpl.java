package org.ouracademy.srtomy.iam.query.expand_permissions;

import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.iam.repository.IamRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class ExpandPermissionsQueryHandlerImpl implements ExpandPermissionsQueryHandler {

  private final IamRoleRepository iamRoleRepository;

  @Override
  @Transactional(readOnly = true)
  public ExpandPermissionsResult handle(ExpandPermissionsQuery query) {
    var all = new TreeSet<String>();
    if (!query.roleIds().isEmpty()) {
      iamRoleRepository
          .findAllById(query.roleIds())
          .forEach(role -> all.addAll(role.getPermissions()));
    }
    all.addAll(orEmpty(query.permissions()));
    return new ExpandPermissionsResult(List.copyOf(all));
  }

  private static Collection<String> orEmpty(Collection<String> values) {
    return values == null ? List.of() : values;
  }
}
