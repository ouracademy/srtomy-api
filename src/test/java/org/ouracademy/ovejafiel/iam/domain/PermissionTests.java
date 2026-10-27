package org.ouracademy.ovejafiel.iam.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PermissionTests {

  @Test
  void fromValueFindsTheCatalogEntryOrNothing() {
    assertThat(Permission.fromValue("members.write")).contains(Permission.MEMBERS_WRITE);
    assertThat(Permission.fromValue("nope")).isEmpty();
  }

  @Test
  void wireValuesAreResourceDotAction() {
    for (var permission : Permission.values()) {
      assertThat(permission.value()).matches("[a-z]+\\.[a-z_]+");
    }
  }
}
