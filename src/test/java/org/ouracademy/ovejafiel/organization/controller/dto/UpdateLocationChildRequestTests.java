package org.ouracademy.ovejafiel.organization.controller.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.organization.domain.Organization;

class UpdateLocationChildRequestTests {

  @Test
  void isChildOnlyForGroupsAndMinistries() {
    assertThat(requestOf(Organization.Type.GROUP).isChild()).isTrue();
    assertThat(requestOf(Organization.Type.MINISTRY).isChild()).isTrue();
    assertThat(requestOf(Organization.Type.CHURCH).isChild()).isFalse();
    assertThat(requestOf(Organization.Type.LOCATION).isChild()).isFalse();
  }

  private static UpdateLocationChildRequest requestOf(Organization.Type type) {
    var request = new UpdateLocationChildRequest();
    request.setType(type);
    return request;
  }
}
