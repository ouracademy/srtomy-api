package org.ouracademy.ovejafiel.iam.query.get_permission_catalog;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.springframework.stereotype.Service;

@Service
class GetPermissionCatalogQueryHandlerImpl implements GetPermissionCatalogQueryHandler {

  @Override
  public PermissionCatalogResult handle(GetPermissionCatalogQuery query) {
    var byResource =
        Arrays.stream(Permission.values())
            .map(Permission::value)
            .collect(
                Collectors.groupingBy(
                    value -> value.substring(0, value.indexOf('.')),
                    LinkedHashMap::new,
                    Collectors.toList()));
    return new PermissionCatalogResult(
        byResource.entrySet().stream()
            .map(entry -> new PermissionCatalogResult.Group(entry.getKey(), entry.getValue()))
            .toList());
  }
}
