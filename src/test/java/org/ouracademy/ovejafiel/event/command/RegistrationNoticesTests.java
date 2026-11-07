package org.ouracademy.ovejafiel.event.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.childOf;
import static org.ouracademy.ovejafiel.event.EventFixtures.registration;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.event.EventFixtures.ticket;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.RegistrationMode;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.query.EffectiveRegistration;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailCommand;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailCommandHandler;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class RegistrationNoticesTests {

  EventRegistrationRepository registrations = mock(EventRegistrationRepository.class);
  SendEmailCommandHandler email = mock(SendEmailCommandHandler.class);
  EventNotices eventNotices = mock(EventNotices.class);
  RegistrationNotices notices =
      new RegistrationNotices(registrations, new EffectiveRegistration(), email, eventNotices);

  Event congress =
      singleEvent(
          EventId.generate(),
          "Congreso",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  @Test
  void aDeletedMegaEventAndItsSubEventsTellEachPersonOnce() {
    congress.moveTo(
        java.time.ZonedDateTime.now().plusDays(10), java.time.ZonedDateTime.now().plusDays(12));
    var workshop =
        childOf(congress, EventId.generate(), "Taller", congress.getStart(), congress.getEnd());
    var lu =
        registration(
            congress, ticket(congress, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    when(registrations.findByEventIdInAndStatusNot(any(), any())).thenReturn(List.of(lu));

    var told = notices.cancelled(List.of(congress, workshop));

    assertThat(told).isEqualTo(1);
    var sent = ArgumentCaptor.forClass(SendEmailCommand.class);
    verify(email, times(1)).handle(sent.capture());
    assertThat(sent.getValue().to()).isEqualTo("lu@x.org");
    assertThat(sent.getValue().type()).isEqualTo("EVENT_CANCELLED");
    assertThat(sent.getValue().subject()).isEqualTo("Se canceló «Congreso»");
    assertThat(sent.getValue().text()).contains("Congreso").contains("Taller");
  }

  @Test
  void anEventWithNobodySignedUpOrAlreadyOverTellsNobody() {
    congress.moveTo(
        java.time.ZonedDateTime.now().plusDays(10), java.time.ZonedDateTime.now().plusDays(12));
    when(registrations.findByEventIdInAndStatusNot(any(), any())).thenReturn(List.of());

    assertThat(notices.cancelled(List.of(congress))).isZero();
    verify(email, never()).handle(any());
  }

  @Test
  void anEventAlreadyOverIsNotAnnounced() {
    congress.moveTo(
        java.time.ZonedDateTime.now().minusDays(12), java.time.ZonedDateTime.now().minusDays(10));

    assertThat(notices.cancelled(List.of(congress))).isZero();
    verify(email, never()).handle(any());
  }

  @Test
  void aSubEventThatStopsUsingItsOwnSignUpCancelsThemAndTellsTheirHolders() {
    var dinner =
        childOf(congress, EventId.generate(), "Cena", congress.getStart(), congress.getEnd());
    dinner.setRegistrationMode(RegistrationMode.OWN);
    var tomas =
        registration(
            dinner, ticket(dinner, "General", 5), "tomas@x.org", RegistrationStatus.CONFIRMED);
    when(registrations.findByEventIdAndStatusNotOrderByCreateDate(
            dinner.getId(), RegistrationStatus.CANCELLED))
        .thenReturn(List.of(tomas));

    var told = notices.ownCancelled(dinner, "Ahora la entrada es libre.");

    assertThat(told).isEqualTo(1);
    assertThat(tomas.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
    var sent = ArgumentCaptor.forClass(SendEmailCommand.class);
    verify(email).handle(sent.capture());
    assertThat(sent.getValue().type()).isEqualTo("REGISTRATION_CHANGED");
    assertThat(sent.getValue().text()).contains("Ahora la entrada es libre.");
  }

  @Test
  void aSubEventCancelledInsideAnEventThatStaysIsToldOnThePageOfThatEvent() {
    congress.moveTo(
        java.time.ZonedDateTime.now().plusDays(10), java.time.ZonedDateTime.now().plusDays(12));
    var workshop =
        childOf(congress, EventId.generate(), "Taller", congress.getStart(), congress.getEnd());
    when(registrations.findByEventIdInAndStatusNot(any(), any())).thenReturn(List.of());

    notices.cancelled(List.of(workshop));

    verify(eventNotices)
        .post(
            congress,
            org.ouracademy.ovejafiel.event.domain.NoticeKind.CANCELLED,
            "Se canceló «Taller» del programa.");
  }

  @Test
  void aWholeMegaEventCancelledPostsNoNoticeOnAPageThatNoLongerExists() {
    congress.moveTo(
        java.time.ZonedDateTime.now().plusDays(10), java.time.ZonedDateTime.now().plusDays(12));
    var workshop =
        childOf(congress, EventId.generate(), "Taller", congress.getStart(), congress.getEnd());
    when(registrations.findByEventIdInAndStatusNot(any(), any())).thenReturn(List.of());

    notices.cancelled(List.of(congress, workshop));

    verify(eventNotices, never()).post(any(), any(), any());
  }

  @Test
  void aSubEventThatStopsUsingItsOwnSignUpSaysSoOnItsPage() {
    var dinner =
        childOf(congress, EventId.generate(), "Cena", congress.getStart(), congress.getEnd());
    when(registrations.findByEventIdAndStatusNotOrderByCreateDate(any(), any()))
        .thenReturn(List.of());

    notices.ownCancelled(dinner, "Ahora la entrada es libre.");

    verify(eventNotices)
        .post(
            dinner,
            org.ouracademy.ovejafiel.event.domain.NoticeKind.REGISTRATION_CHANGED,
            "La inscripción propia de este evento ya no se usa. Ahora la entrada es libre.");
  }
}
