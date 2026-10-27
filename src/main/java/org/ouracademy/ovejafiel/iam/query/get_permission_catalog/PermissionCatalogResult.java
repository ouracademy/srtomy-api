package org.ouracademy.ovejafiel.iam.query.get_permission_catalog;

import java.util.List;

/** Every permission a role can include, grouped by resource (the part before the dot). */
public record PermissionCatalogResult(List<Group> groups) {

  public record Group(String resource, List<String> permissions) {}
}
