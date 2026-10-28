package org.ouracademy.ovejafiel.event.command.set_registration_check_in;

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
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

class SetRegistrationCheckInCommandHandlerImplTests {

  EventRegistrationRepository repository = mock(EventRegistrationRepository.class);
  SetRegistrationCheckInCommandHandlerImpl handler =
      new SetRegistrationCheckInCommandHandlerImpl(repository);

  Event event =
      singleEvent(
          EventId.generate(),
          "Curso",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  @Test
  void marksThemAsPresentByHand() {
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    when(repository.findById(registration.getId())).thenReturn(Optional.of(registration));

    var result =
        handler.handle(
            new SetRegistrationCheckInCommand(event.getId(), registration.getId(), true));

    assertThat(result.checkedInAt()).isNotNull();
    assertThat(registration.getCheckInSource()).isEqualTo(CheckInSource.MANUAL);
  }

  @Test
  void takesTheMarkBack() {
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    registration.checkIn(CheckInSource.QR_TICKET);
    when(repository.findById(registration.getId())).thenReturn(Optional.of(registration));

    var result =
        handler.handle(
            new SetRegistrationCheckInCommand(event.getId(), registration.getId(), false));

    assertThat(result.checkedInAt()).isNull();
    assertThat(registration.isCheckedIn()).isFalse();
  }

  @Test
  void whoIsWaitingCannotBeMarked() {
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.WAITLISTED);
    when(repository.findById(registration.getId())).thenReturn(Optional.of(registration));

    assertThatThrownBy(
            () ->
                handler.handle(
                    new SetRegistrationCheckInCommand(event.getId(), registration.getId(), true)))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void aSignUpOfAnotherEventOrAnUnknownOneIsNotFound() {
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    when(repository.findById(registration.getId())).thenReturn(Optional.of(registration));
    var unknown = EventRegistrationId.generate();
    when(repository.findById(unknown)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                handler.handle(
                    new SetRegistrationCheckInCommand(
                        EventId.generate(), registration.getId(), true)))
        .isInstanceOf(NoSuchElementException.class);
    assertThatThrownBy(
            () -> handler.handle(new SetRegistrationCheckInCommand(event.getId(), unknown, true)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
