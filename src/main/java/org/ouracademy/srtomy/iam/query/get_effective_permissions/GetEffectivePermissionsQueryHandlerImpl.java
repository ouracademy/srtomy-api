package org.ouracademy.srtomy.iam.query.get_effective_permissions;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.repository.RoleBindingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetEffectivePermissionsQueryHandlerImpl implements GetEffectivePermissionsQueryHandler {

  private final RoleBindingRepository roleBindingRepository;

  @Override
  @Transactional(readOnly = true)
  public EffectivePermissionsResult handle(GetEffectivePermissionsQuery query) {
    // Values no longer in the catalog (e.g. a permission retired from code) are ignored.
    var permissions =
        roleBindingRepository
            .findPermissions(query.subject(), List.of(query.organizationId().value()))
            .stream()
            .map(Permission::fromValue)
            .flatMap(Optional::stream)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(Permission.class)));
    return new EffectivePermissionsResult(permissions);
  }
}
