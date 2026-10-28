package org.ouracademy.ovejafiel.event.command.cancel_event_registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.registration;
import static org.ouracademy.ovejafiel.event.EventFixtures.registrationSetup;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.event.EventFixtures.ticket;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.command.WaitlistPromotion;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistration;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationSetupRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class CancelEventRegistrationCommandHandlerImplTests {

  EventRegistrationRepository repository = mock(EventRegistrationRepository.class);
  EventRegistrationSetupRepository setups = mock(EventRegistrationSetupRepository.class);
  WaitlistPromotion promotion = mock(WaitlistPromotion.class);
  CancelEventRegistrationCommandHandlerImpl handler =
      new CancelEventRegistrationCommandHandlerImpl(repository, setups, promotion);

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
  void cancelsTheSignUpAndGivesItsSeatToTheWaitlist() {
    var event = eventOf(eventId);
    var setup = registrationSetup(event, true, null);
    var registration = registrationOf(event, RegistrationStatus.CONFIRMED);
    when(setups.lockByEventId(eventId)).thenReturn(Optional.of(setup));
    when(repository.findById(registrationId)).thenReturn(Optional.of(registration));

    handler.handle(new CancelEventRegistrationCommand(eventId, registrationId));

    assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
    verify(promotion).promote(setup);
  }

  @Test
  void cancellingTwiceChangesNothingAndPromotesNobody() {
    var event = eventOf(eventId);
    var registration = registrationOf(event, RegistrationStatus.CANCELLED);
    when(setups.lockByEventId(eventId))
        .thenReturn(Optional.of(registrationSetup(event, true, null)));
    when(repository.findById(registrationId)).thenReturn(Optional.of(registration));

    handler.handle(new CancelEventRegistrationCommand(eventId, registrationId));

    verify(promotion, never()).promote(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void aSignUpOfAnotherEventIsNotFoundAndNotCancelled() {
    var registration = registrationOf(eventOf(EventId.generate()), RegistrationStatus.CONFIRMED);
    when(setups.lockByEventId(eventId)).thenReturn(Optional.empty());
    when(repository.findById(registrationId)).thenReturn(Optional.of(registration));

    assertThatThrownBy(
            () -> handler.handle(new CancelEventRegistrationCommand(eventId, registrationId)))
        .isInstanceOf(NoSuchElementException.class);
    assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
  }

  @Test
  void anUnknownSignUpIsNotFound() {
    when(setups.lockByEventId(eventId)).thenReturn(Optional.empty());
    when(repository.findById(registrationId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> handler.handle(new CancelEventRegistrationCommand(eventId, registrationId)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
