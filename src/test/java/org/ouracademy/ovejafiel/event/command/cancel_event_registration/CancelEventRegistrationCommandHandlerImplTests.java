package org.ouracademy.ovejafiel.event.command.cancel_event_registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.registration;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.event.EventFixtures.ticket;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistration;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class CancelEventRegistrationCommandHandlerImplTests {

  EventRegistrationRepository repository = mock(EventRegistrationRepository.class);
  CancelEventRegistrationCommandHandlerImpl handler =
      new CancelEventRegistrationCommandHandlerImpl(repository);

  EventId eventId = EventId.generate();
  EventRegistrationId registrationId = EventRegistrationId.generate();

  Event eventOf(EventId id) {
    return singleEvent(
        id,
        "Curso",
        organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
        List.of());
  }

  EventRegistration registrationOf(Event event, RegistrationStatus status) {
    return registration(event, ticket(event, "General", 5), "lu@x.org", status);
  }

  @Test
  void cancelsTheSignUpAndKeepsItAsHistory() {
    var registration = registrationOf(eventOf(eventId), RegistrationStatus.CONFIRMED);
    when(repository.findById(registrationId)).thenReturn(Optional.of(registration));

    handler.handle(new CancelEventRegistrationCommand(eventId, registrationId));

    assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
  }

  @Test
  void cancellingACheckedInSignUpTakesTheCheckInBack() {
    var registration = registrationOf(eventOf(eventId), RegistrationStatus.CONFIRMED);
    registration.checkIn(org.ouracademy.ovejafiel.event.domain.CheckInSource.MANUAL);
    when(repository.findById(registrationId)).thenReturn(Optional.of(registration));

    handler.handle(new CancelEventRegistrationCommand(eventId, registrationId));

    assertThat(registration.isCheckedIn()).isFalse();
  }

  @Test
  void cancellingTwiceChangesNothing() {
    var registration = registrationOf(eventOf(eventId), RegistrationStatus.CANCELLED);
    when(repository.findById(registrationId)).thenReturn(Optional.of(registration));

    handler.handle(new CancelEventRegistrationCommand(eventId, registrationId));

    assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
  }

  @Test
  void aSignUpOfAnotherEventIsNotFoundAndNotCancelled() {
    var registration = registrationOf(eventOf(EventId.generate()), RegistrationStatus.CONFIRMED);
    when(repository.findById(registrationId)).thenReturn(Optional.of(registration));

    assertThatThrownBy(
            () -> handler.handle(new CancelEventRegistrationCommand(eventId, registrationId)))
        .isInstanceOf(NoSuchElementException.class);
    assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
  }

  @Test
  void anUnknownSignUpIsNotFound() {
    when(repository.findById(registrationId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> handler.handle(new CancelEventRegistrationCommand(eventId, registrationId)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
