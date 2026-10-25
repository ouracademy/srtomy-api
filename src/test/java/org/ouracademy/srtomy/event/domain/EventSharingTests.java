package org.ouracademy.srtomy.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.ouracademy.srtomy.event.EventFixtures.singleEvent;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;

class EventSharingTests {

  Event event(boolean course) {
    var event =
        singleEvent(
            EventId.generate(),
            "Retiro",
            organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
            List.of());
    event.setIsACourse(course);
    return event;
  }

  @Test
  void aNewEventIsRestrictedWithAnUnguessableLink() {
    var first = event(false);
    var second = event(false);

    assertThat(first.getGeneralAccess()).isEqualTo(GeneralAccess.RESTRICTED);
    assertThat(first.getLinkRole()).isEqualTo(LinkRole.VIEWER);
    assertThat(first.getShareSlug())
        .matches("[A-Za-z0-9_-]{16}")
        .isNotEqualTo(second.getShareSlug());
  }

  @Test
  void anyEventCanBeOpenedToTheLinkAsViewer() {
    var event = event(false);

    event.shareVia(GeneralAccess.ANYONE_WITH_LINK, LinkRole.VIEWER);

    assertThat(event.getGeneralAccess()).isEqualTo(GeneralAccess.ANYONE_WITH_LINK);
  }

  @Test
  void onlyACourseCanOpenItsRegistration() {
    var course = event(true);
    course.shareVia(GeneralAccess.ANYONE_WITH_LINK, LinkRole.REGISTRANT);
    assertThat(course.getLinkRole()).isEqualTo(LinkRole.REGISTRANT);

    var plain = event(false);
    assertThatThrownBy(() -> plain.shareVia(GeneralAccess.CHURCH, LinkRole.REGISTRANT))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("curso");
    assertThat(plain.getGeneralAccess()).isEqualTo(GeneralAccess.RESTRICTED);
  }

  @Test
  void renewingTheLinkRevokesTheOldOne() {
    var event = event(false);
    var old = event.getShareSlug();

    event.renewShareLink();

    assertThat(event.getShareSlug()).isNotEqualTo(old).hasSize(16);
  }

  @Test
  void levelsAreOrderedAndCombineByTheHighest() {
    assertThat(AccessLevel.MANAGER.atLeast(AccessLevel.EDITOR)).isTrue();
    assertThat(AccessLevel.VIEWER.atLeast(AccessLevel.EDITOR)).isFalse();
    assertThat(AccessLevel.NONE.atLeast(AccessLevel.NONE)).isTrue();
    assertThat(AccessLevel.VIEWER.or(AccessLevel.EDITOR)).isEqualTo(AccessLevel.EDITOR);
    assertThat(AccessLevel.MANAGER.or(AccessLevel.NONE)).isEqualTo(AccessLevel.MANAGER);
  }

  @Test
  void aShareMatchesItsEmailIgnoringCaseAndSpaces() {
    var share = new EventShare(event(false), "ana@x.org", ShareRole.VIEWER, null);

    assertThat(share.isFor(" ANA@x.org ")).isTrue();
    assertThat(share.isFor("luis@x.org")).isFalse();
    assertThat(share.isFor(null)).isFalse();
  }
}
