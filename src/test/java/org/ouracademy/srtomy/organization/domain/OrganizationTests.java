package org.ouracademy.srtomy.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrganizationTests {

  @Test
  void onlyGroupsAndMinistriesAreGroupOrMinistry() {
    assertThat(new Organization("Youth", Organization.Type.GROUP).isGroupOrMinistry()).isTrue();
    assertThat(new Organization("Youth", Organization.Type.MINISTRY).isGroupOrMinistry()).isTrue();
    assertThat(new Organization("Church", Organization.Type.CHURCH).isGroupOrMinistry()).isFalse();
    assertThat(new Organization("Sede", Organization.Type.LOCATION).isGroupOrMinistry()).isFalse();
  }

  @Test
  void isDeletableMatchesGroupOrMinistry() {
    assertThat(new Organization("Youth", Organization.Type.GROUP).isDeletable()).isTrue();
    assertThat(new Organization("Church", Organization.Type.CHURCH).isDeletable()).isFalse();
  }

  @Test
  void changeTypeReplacesTheType() {
    var group = new Organization("Youth", Organization.Type.GROUP);

    group.changeType(Organization.Type.MINISTRY);

    assertThat(group.getType()).isEqualTo(Organization.Type.MINISTRY);
  }
}
