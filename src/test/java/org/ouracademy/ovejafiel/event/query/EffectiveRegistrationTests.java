package org.ouracademy.ovejafiel.event.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ouracademy.ovejafiel.event.EventFixtures.childOf;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.RegistrationMode;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class EffectiveRegistrationTests {

  EffectiveRegistration effective = new EffectiveRegistration();

  Event conference =
      singleEvent(
          EventId.generate(),
          "Mega evento",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  private Event child(RegistrationMode mode) {
    var child =
        childOf(
            conference, EventId.generate(), "Taller", conference.getStart(), conference.getEnd());
    child.setRegistrationMode(mode);
    return child;
  }

  @Test
  void anEventOnItsOwnAnswersToItself() {
    assertThat(effective.governing(conference)).containsSame(conference);
    assertThat(effective.isOwn(conference)).isTrue();
  }

  @Test
  void aSubEventThatInheritsAnswersToItsContainer() {
    var child = child(RegistrationMode.INHERIT);

    assertThat(effective.governing(child)).containsSame(conference);
    assertThat(effective.isOwn(child)).isFalse();
  }

  @Test
  void aSubEventWithItsOwnRegistrationAnswersToItself() {
    var child = child(RegistrationMode.OWN);

    assertThat(effective.governing(child)).containsSame(child);
    assertThat(effective.isOwn(child)).isTrue();
  }

  @Test
  void anOpenSubEventAnswersToNobody() {
    var child = child(RegistrationMode.OPEN);

    assertThat(effective.governing(child)).isEmpty();
    assertThat(effective.isOwn(child)).isFalse();
  }
}
