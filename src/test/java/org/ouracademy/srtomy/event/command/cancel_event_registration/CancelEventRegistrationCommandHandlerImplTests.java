package org.ouracademy.srtomy.event.command.cancel_event_registration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.event.EventFixtures.singleEvent;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.domain.EventRegistration;
import org.ouracademy.srtomy.event.domain.EventRegistrationId;
import org.ouracademy.srtomy.event.repository.EventRegistrationRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.party.domain.PartyId;

class CancelEventRegistrationCommandHandlerImplTests {

  EventRegistrationRepository repository = mock(EventRegistrationRepository.class);
  CancelEventRegistrationCommandHandlerImpl handler =
      new CancelEventRegistrationCommandHandlerImpl(repository);

  EventId eventId = EventId.generate();
  EventRegistrationId registrationId = EventRegistrationId.generate();

  EventRegistration registrationOf(EventId owner) {
    var event =
        singleEvent(
            owner,
            "Curso",
            organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
            List.of());
    return new EventRegistration(event, null, "Lu", "Mora", "lu@x.org", null);
  }

  @Test
  void removesTheSignUpOfThatEvent() {
    var registration = registrationOf(eventId);
    when(repository.findById(registrationId)).thenReturn(Optional.of(registration));

    handler.handle(new CancelEventRegistrationCommand(eventId, registrationId));

    verify(repository).delete(registration);
  }

  @Test
  void aSignUpOfAnotherEventIsNotFoundAndNotRemoved() {
    var registration = registrationOf(EventId.generate());
    when(repository.findById(registrationId)).thenReturn(Optional.of(registration));

    assertThatThrownBy(
            () -> handler.handle(new CancelEventRegistrationCommand(eventId, registrationId)))
        .isInstanceOf(NoSuchElementException.class);
    verify(repository, never()).delete(registration);
  }

  @Test
  void anUnknownSignUpIsNotFound() {
    when(repository.findById(registrationId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> handler.handle(new CancelEventRegistrationCommand(eventId, registrationId)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
