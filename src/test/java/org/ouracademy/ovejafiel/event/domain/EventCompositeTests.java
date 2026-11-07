package org.ouracademy.ovejafiel.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.EventFixtures;
import org.ouracademy.ovejafiel.organization.OrganizationFixtures;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.test.util.ReflectionTestUtils;

class EventCompositeTests {

  private static final ZonedDateTime FRIDAY = ZonedDateTime.parse("2026-08-28T00:00:00Z[UTC]");
  private static final ZonedDateTime SUNDAY_END = ZonedDateTime.parse("2026-08-30T23:59:00Z[UTC]");

  private Organization church;
  private Event conference;

  @BeforeEach
  void setUp() {
    church =
        OrganizationFixtures.organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH);
    conference =
        EventFixtures.eventBetween(EventId.generate(), "Mega evento", church, FRIDAY, SUNDAY_END);
  }

  private Event child(ZonedDateTime start, ZonedDateTime end) {
    return EventFixtures.eventBetween(EventId.generate(), "Taller", church, start, end);
  }

  @Test
  void aChildInsideTheContainerBecomesItsChildAndInheritsItsAccessAtCreation() {
    conference.shareVia(GeneralAccess.CHURCH);
    var workshop = child(FRIDAY.plusDays(1).plusHours(9), FRIDAY.plusDays(1).plusHours(11));

    workshop.attachTo(conference);

    assertThat(workshop.getParent()).isSameAs(conference);
    assertThat(workshop.isChild()).isTrue();
    assertThat(workshop.getGeneralAccess()).isEqualTo(GeneralAccess.CHURCH);
    assertThat(workshop.getRegistrationMode()).isEqualTo(RegistrationMode.INHERIT);
    assertThat(conference.isChild()).isFalse();
  }

  @Test
  void aChildCannotHaveChildren() {
    var workshop = child(FRIDAY.plusHours(9), FRIDAY.plusHours(11));
    workshop.attachTo(conference);
    var session = child(FRIDAY.plusHours(9), FRIDAY.plusHours(10));

    assertThatThrownBy(() -> session.attachTo(workshop))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void aChildMustFitInsideTheContainerDates() {
    var before = child(FRIDAY.minusHours(1), FRIDAY.plusHours(1));
    var after = child(SUNDAY_END.minusHours(1), SUNDAY_END.plusHours(1));

    assertThatThrownBy(() -> before.attachTo(conference))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> after.attachTo(conference))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void compositeEventsCannotBeOccurrencesOfASeries() {
    var occurrence = child(FRIDAY.plusHours(9), FRIDAY.plusHours(11));
    ReflectionTestUtils.setField(occurrence, "series", EventFixtures.eventSeries(church));
    var workshop = child(FRIDAY.plusHours(9), FRIDAY.plusHours(11));
    ReflectionTestUtils.setField(conference, "series", EventFixtures.eventSeries(church));

    assertThatThrownBy(() -> occurrence.attachTo(conference))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> workshop.attachTo(conference))
        .isInstanceOf(DomainValidationException.class);
  }
}
