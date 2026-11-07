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
  org.ouracademy.ovejafiel.event.repository.EventRepository eventRepository =
      mock(org.ouracademy.ovejafiel.event.repository.EventRepository.class);
  org.ouracademy.ovejafiel.event.repository.SubEventCheckInRepository subEventCheckIns =
      mock(org.ouracademy.ovejafiel.event.repository.SubEventCheckInRepository.class);
  org.ouracademy.ovejafiel.event.query.InheritedCheckIns inheritedCheckIns =
      new org.ouracademy.ovejafiel.event.query.InheritedCheckIns(subEventCheckIns);
  SetRegistrationCheckInCommandHandlerImpl handler =
      new SetRegistrationCheckInCommandHandlerImpl(repository, eventRepository, inheritedCheckIns);

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
  void aCancelledSignUpCannotBeMarked() {
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CANCELLED);
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

  private org.ouracademy.ovejafiel.event.domain.Event inheritingSubEvent() {
    var child =
        org.ouracademy.ovejafiel.event.EventFixtures.childOf(
            event, EventId.generate(), "Taller", event.getStart(), event.getEnd());
    when(eventRepository.findById(child.getId())).thenReturn(Optional.of(child));
    when(subEventCheckIns.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(call -> call.getArgument(0));
    return child;
  }

  @Test
  void marksASignUpOfTheMegaEventAsInTheSubEventWithoutTouchingTheSignUp() {
    var child = inheritingSubEvent();
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    when(repository.findById(registration.getId())).thenReturn(Optional.of(registration));

    var result =
        handler.handle(
            new SetRegistrationCheckInCommand(child.getId(), registration.getId(), true));

    assertThat(result.checkedInAt()).isNotNull();
    assertThat(result.alreadyCheckedIn()).isFalse();
    assertThat(registration.isCheckedIn()).isFalse();
    org.mockito.Mockito.verify(subEventCheckIns).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void takesTheMarkBackInTheSubEvent() {
    var child = inheritingSubEvent();
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    when(repository.findById(registration.getId())).thenReturn(Optional.of(registration));
    var mark =
        new org.ouracademy.ovejafiel.event.domain.SubEventCheckIn(
            registration, child, CheckInSource.MANUAL);
    when(subEventCheckIns.findByRegistrationIdAndEventId(registration.getId(), child.getId()))
        .thenReturn(Optional.of(mark));

    var result =
        handler.handle(
            new SetRegistrationCheckInCommand(child.getId(), registration.getId(), false));

    assertThat(result.checkedInAt()).isNull();
    org.mockito.Mockito.verify(subEventCheckIns).delete(mark);
  }

  @Test
  void aSignUpOfAnotherEventIsNotFoundInASubEvent() {
    var child = inheritingSubEvent();
    var other =
        singleEvent(
            EventId.generate(),
            "Otro",
            organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
            List.of());
    var registration =
        registration(other, ticket(other, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    when(repository.findById(registration.getId())).thenReturn(Optional.of(registration));

    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                handler.handle(
                    new SetRegistrationCheckInCommand(child.getId(), registration.getId(), true)))
        .isInstanceOf(java.util.NoSuchElementException.class);
  }
}
