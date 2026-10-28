package org.ouracademy.ovejafiel.event.query.get_shared_event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.eventSeries;
import static org.ouracademy.ovejafiel.event.EventFixtures.occurrenceOf;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.AccessLevel;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistration;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationSetup;
import org.ouracademy.ovejafiel.event.domain.GeneralAccess;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.domain.TicketType;
import org.ouracademy.ovejafiel.event.query.EventAccess;
import org.ouracademy.ovejafiel.event.query.Seats;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationSetupRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.event.repository.TicketTypeRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.query.UserResult;

class GetSharedEventQueryHandlerImplTests {

  EventRepository events = mock(EventRepository.class);
  EventRegistrationSetupRepository setups = mock(EventRegistrationSetupRepository.class);
  TicketTypeRepository tickets = mock(TicketTypeRepository.class);
  EventRegistrationRepository registrations = mock(EventRegistrationRepository.class);
  EventAccess access = mock(EventAccess.class);
  Seats seats = mock(Seats.class);
  GetSharedEventQueryHandlerImpl handler =
      new GetSharedEventQueryHandlerImpl(events, setups, tickets, registrations, access, seats);

  Organization group = organization(PartyId.generate(), "Jóvenes", Organization.Type.GROUP);
  Event event = singleEvent(EventId.generate(), "Retiro", group, List.of());

  @BeforeEach
  void setUp() {
    when(events.findByShareSlug("slug")).thenReturn(Optional.of(event));
    when(access.canOpen(any(), any()))
        .thenAnswer(
            call -> {
              Event opened = call.getArgument(1);
              return opened.getGeneralAccess() == GeneralAccess.ANYONE_WITH_LINK
                  || access.levelOn(call.getArgument(0), opened).atLeast(AccessLevel.VIEWER);
            });
    when(access.levelOn(any(), any())).thenReturn(AccessLevel.NONE);
    when(setups.findByEventId(any())).thenReturn(Optional.empty());
    when(seats.confirmedByTicket(any())).thenReturn(new HashMap<>());
    when(registrations.findFirstByEventIdAndUserIdAndStatusNot(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(registrations.findFirstByEventIdAndEmailIgnoreCaseAndStatusNot(any(), any(), any()))
        .thenReturn(Optional.empty());
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
    event.shareVia(GeneralAccess.ANYONE_WITH_LINK);

    var page = handler.handle(new GetSharedEventQuery("slug", null)).orElseThrow();

    assertThat(page.title()).isEqualTo("Retiro");
    assertThat(page.organizationName()).isEqualTo("Jóvenes");
    assertThat(page.address()).isEqualTo("Av. Siempre Viva 123");
    assertThat(page.isACourse()).isFalse();
    assertThat(page.recurrence()).isNull();
    assertThat(page.registration()).isNull();
    assertThat(page.canRegister()).isFalse();
    assertThat(page.registrationStatus()).isNull();
  }

  @Test
  void whoCanViewTheEventOpensItEvenIfItIsRestricted() {
    when(access.levelOn("me", event)).thenReturn(AccessLevel.VIEWER);

    assertThat(handler.handle(new GetSharedEventQuery("slug", "me"))).isPresent();
    assertThat(handler.handle(new GetSharedEventQuery("slug", null))).isEmpty();
  }

  Event courseWithRegistration(boolean open, boolean requiresAccount) {
    var course = singleEvent(EventId.generate(), "Curso", group, List.of());
    course.setIsACourse(true);
    course.shareVia(GeneralAccess.ANYONE_WITH_LINK);
    when(events.findByShareSlug("course")).thenReturn(Optional.of(course));
    var setup = new EventRegistrationSetup(course);
    setup.configure(requiresAccount);
    setup.setOpen(open);
    when(setups.findByEventId(course.getId())).thenReturn(Optional.of(setup));
    var ticket = new TicketType(course, 0);
    ticket.describe("General", 0, "PEN", 2, null, null);
    when(tickets.findByEventIdOrderByPosition(course.getId())).thenReturn(List.of(ticket));
    when(seats.remaining(any(), any(), any())).thenReturn(2);
    return course;
  }

  @Test
  void anOpenRegistrationShowsItsTicketsAndLetsTheVisitorSignUp() {
    courseWithRegistration(true, false);

    var page = handler.handle(new GetSharedEventQuery("course", null)).orElseThrow();

    assertThat(page.canRegister()).isTrue();
    assertThat(page.registration().soldOut()).isFalse();
    assertThat(page.registration().tickets())
        .extracting(GetSharedEventResult.Ticket::name, GetSharedEventResult.Ticket::remaining)
        .containsExactly(tuple("General", 2));
  }

  @Test
  void aClosedRegistrationCannotBeSignedUpFor() {
    courseWithRegistration(false, false);

    var page = handler.handle(new GetSharedEventQuery("course", null)).orElseThrow();

    assertThat(page.registration().open()).isFalse();
    assertThat(page.canRegister()).isFalse();
  }

  @Test
  void aRegistrationThatNeedsAnAccountAsksForOne() {
    courseWithRegistration(true, true);

    assertThat(handler.handle(new GetSharedEventQuery("course", null)).orElseThrow().canRegister())
        .isFalse();

    when(access.verifiedUser("me"))
        .thenReturn(new UserResult(UserId.generate(), "me", "lu@x.org", true));
    assertThat(handler.handle(new GetSharedEventQuery("course", "me")).orElseThrow().canRegister())
        .isTrue();
  }

  @Test
  void whenNoTicketHasSeatsLeftTheEventIsSoldOut() {
    courseWithRegistration(true, false);
    when(seats.remaining(any(), any(), any())).thenReturn(0);

    var page = handler.handle(new GetSharedEventQuery("course", null)).orElseThrow();

    assertThat(page.registration().soldOut()).isTrue();
  }

  @Test
  void theVisitorSeesTheirPlaceIfTheyAlreadyHaveOne() {
    var course = courseWithRegistration(true, false);
    var user = new UserResult(UserId.generate(), "me", "lu@x.org", true);
    when(access.verifiedUser("me")).thenReturn(user);
    var mine =
        new EventRegistration(
            course,
            new TicketType(course, 0),
            null,
            "Lu",
            "Mora",
            "lu@x.org",
            null,
            RegistrationStatus.CONFIRMED);
    when(registrations.findFirstByEventIdAndEmailIgnoreCaseAndStatusNot(
            course.getId(), "lu@x.org", RegistrationStatus.CANCELLED))
        .thenReturn(Optional.of(mine));

    var page = handler.handle(new GetSharedEventQuery("course", "me")).orElseThrow();

    assertThat(page.registrationStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
    assertThat(page.canRegister()).isFalse();
  }

  @Test
  void aRepeatingEventShowsHowItRepeats() {
    var series = occurrenceOf(eventSeries(group), EventId.generate());
    series.shareVia(GeneralAccess.ANYONE_WITH_LINK);
    when(events.findByShareSlug("series")).thenReturn(Optional.of(series));

    var page = handler.handle(new GetSharedEventQuery("series", null)).orElseThrow();

    assertThat(page.recurrence().daysOfWeek()).containsExactly(1, 3);
    assertThat(page.recurrence().until()).hasToString("2026-01-14");
  }
}
