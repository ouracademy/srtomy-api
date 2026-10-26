package org.ouracademy.ovejafiel.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

class OrganizationTests {

  @Test
  void onlyGroupsAndMinistriesAreGroupOrMinistry() {
    assertThat(new Organization("Youth", Organization.Type.GROUP).isGroupOrMinistry()).isTrue();
    assertThat(new Organization("Youth", Organization.Type.MINISTRY).isGroupOrMinistry()).isTrue();
    assertThat(new Organization("Church", Organization.Type.CHURCH).isGroupOrMinistry()).isFalse();
    assertThat(new Organization("Sede", Organization.Type.LOCATION).isGroupOrMinistry()).isFalse();
  }

  @Test
  void aNewNodeIsPublicWithItsOrgChartAndAnUnguessableSlug() {
    var first = new Organization("Youth", Organization.Type.GROUP);
    var second = new Organization("Youth", Organization.Type.GROUP);

    assertThat(first.getMemberVisibility()).isEqualTo(MemberVisibility.PUBLIC);
    assertThat(first.isShowOrgChart()).isTrue();
    assertThat(first.getSlug()).matches("[A-Za-z0-9_-]{16}").isNotEqualTo(second.getSlug());
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

  @Test
  void changeTypeOnlySwitchesBetweenGroupAndMinistry() {
    var group = new Organization("Youth", Organization.Type.GROUP);
    var location = new Organization("Sede", Organization.Type.LOCATION);

    assertThatThrownBy(() -> group.changeType(Organization.Type.LOCATION))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> location.changeType(Organization.Type.GROUP))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void theTreeIsChurchThenLocationsThenNestedGroupsAndMinistries() {
    var church = Organization.createUnder("Iglesia", Organization.Type.CHURCH, null);
    var location = Organization.createUnder("Sede", Organization.Type.LOCATION, church);
    var ministry = Organization.createUnder("Alabanza", Organization.Type.MINISTRY, location);
    var group = Organization.createUnder("Coro", Organization.Type.GROUP, ministry);
    var subgroup = Organization.createUnder("Sopranos", Organization.Type.GROUP, group);

    assertThat(subgroup.getParent()).isSameAs(group);
    assertThat(group.getParent().getParent()).isSameAs(location);
  }

  @Test
  void createUnderRejectsNodesOutOfPlace() {
    var church = new Organization("Iglesia", Organization.Type.CHURCH);
    var location = new Organization("Sede", Organization.Type.LOCATION);

    assertThatThrownBy(() -> Organization.createUnder("X", Organization.Type.CHURCH, church))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> Organization.createUnder("X", Organization.Type.LOCATION, null))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> Organization.createUnder("X", Organization.Type.LOCATION, location))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> Organization.createUnder("X", Organization.Type.GROUP, church))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void socialLinksAreNeverNull() {
    var church = new Organization("Iglesia", Organization.Type.CHURCH);

    assertThat(church.getSocialLinks()).isEqualTo(SocialLinks.NONE);
  }
}
