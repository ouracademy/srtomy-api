package org.ouracademy.ovejafiel.event.query.get_shared_event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.recurrentEvent;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.AccessLevel;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.GeneralAccess;
import org.ouracademy.ovejafiel.event.domain.LinkRole;
import org.ouracademy.ovejafiel.event.query.EventAccess;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.query.UserResult;

class GetSharedEventQueryHandlerImplTests {

  EventRepository events = mock(EventRepository.class);
  EventRegistrationRepository registrations = mock(EventRegistrationRepository.class);
  EventAccess access = mock(EventAccess.class);
  GetSharedEventQueryHandlerImpl handler =
      new GetSharedEventQueryHandlerImpl(events, registrations, access);

  Organization group = organization(PartyId.generate(), "Jóvenes", Organization.Type.GROUP);
  Event event = singleEvent(EventId.generate(), "Retiro", group, List.of());

  @BeforeEach
  void setUp() {
    when(events.findByShareSlug("slug")).thenReturn(Optional.of(event));
    when(access.levelOn(any(), any())).thenReturn(AccessLevel.NONE);
  }

  @Test
  void aRestrictedEventDoesNotOpenForAVisitorWithoutAccess() {
    assertThat(handler.handle(new GetSharedEventQuery("slug", null))).isEmpty();
    assertThat(handler.handle(new GetSharedEventQuery("slug", "me"))).isEmpty();
  }

  @Test
  void anUnknownLinkIsEmpty() {
    when(events.findByShareSlug("nope")).thenReturn(Optional.empty());

    assertThat(handler.handle(new GetSharedEventQuery("nope", "me"))).isEmpty();
  }

  @Test
  void anOpenLinkShowsTheEventToAnyone() {
    event.shareVia(GeneralAccess.ANYONE_WITH_LINK, LinkRole.VIEWER);

    var page = handler.handle(new GetSharedEventQuery("slug", null)).orElseThrow();

    assertThat(page.title()).isEqualTo("Retiro");
    assertThat(page.organizationName()).isEqualTo("Jóvenes");
    assertThat(page.address()).isEqualTo("Av. Siempre Viva 123");
    assertThat(page.isACourse()).isFalse();
    assertThat(page.daysOfWeek()).isNull();
    assertThat(page.canRegister()).isFalse();
    assertThat(page.registered()).isFalse();
  }

  @Test
  void whoCanViewTheEventOpensItEvenIfItIsRestricted() {
    when(access.levelOn("me", event)).thenReturn(AccessLevel.VIEWER);

    assertThat(handler.handle(new GetSharedEventQuery("slug", "me"))).isPresent();
    assertThat(handler.handle(new GetSharedEventQuery("slug", null))).isEmpty();
  }

  @Test
  void theVisitorSeesWhetherTheyCanSignUpAndIfTheyAlready() {
    var course = singleEvent(EventId.generate(), "Curso", group, List.of());
    course.setIsACourse(true);
    course.shareVia(GeneralAccess.ANYONE_WITH_LINK, LinkRole.REGISTRANT);
    when(events.findByShareSlug("course")).thenReturn(Optional.of(course));
    when(access.canRegister("me", course)).thenReturn(true);
    when(access.verifiedUser("me"))
        .thenReturn(new UserResult(UserId.generate(), "me", "lu@x.org", true));
    when(registrations.existsByEventIdAndEmailIgnoreCase(course.getId(), "lu@x.org"))
        .thenReturn(true);

    var page = handler.handle(new GetSharedEventQuery("course", "me")).orElseThrow();

    assertThat(page.canRegister()).isTrue();
    assertThat(page.registered()).isTrue();
  }

  @Test
  void aRecurringEventShowsItsDays() {
    var series = recurrentEvent(EventId.generate(), group);
    series.shareVia(GeneralAccess.ANYONE_WITH_LINK, LinkRole.VIEWER);
    when(events.findByShareSlug("series")).thenReturn(Optional.of(series));

    var page = handler.handle(new GetSharedEventQuery("series", null)).orElseThrow();

    assertThat(page.daysOfWeek()).containsExactly(1, 3);
    assertThat(page.endRecur()).hasToString("2026-01-14");
  }
}
