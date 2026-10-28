package org.ouracademy.ovejafiel.iam.query.get_permission_catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.iam.domain.Permission;

class GetPermissionCatalogQueryHandlerImplTests {

  @Test
  void listsEveryPermissionOnceGroupedByResource() {
    var groups =
        new GetPermissionCatalogQueryHandlerImpl().handle(new GetPermissionCatalogQuery()).groups();

    assertThat(groups)
        .extracting(PermissionCatalogResult.Group::resource)
        .containsExactly(
            "org",
            "members",
            "events",
            "attendance",
            "registrations",
            "positions",
            "iam",
            "invitations");
    assertThat(groups.stream().flatMap(g -> g.permissions().stream()))
        .containsExactlyInAnyOrderElementsOf(
            Arrays.stream(Permission.values()).map(Permission::value).toList());
  }
}
