package org.ouracademy.ovejafiel.event.command.check_in_registration_by_ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.childOf;
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
import org.ouracademy.ovejafiel.event.domain.RegistrationMode;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.query.EffectiveRegistration;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

class CheckInRegistrationByTicketCommandHandlerImplTests {

  EventRegistrationRepository repository = mock(EventRegistrationRepository.class);
  EventRepository eventRepository = mock(EventRepository.class);
  CheckInRegistrationByTicketCommandHandlerImpl handler =
      new CheckInRegistrationByTicketCommandHandlerImpl(
          repository, eventRepository, new EffectiveRegistration());

  Event event =
      singleEvent(
          EventId.generate(),
          "Curso",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  @Test
  void scanningTheTicketChecksTheirSignUpIn() {
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    when(repository.findByTicketCode(registration.getTicketCode()))
        .thenReturn(Optional.of(registration));

    var result =
        handler.handle(
            new CheckInRegistrationByTicketCommand(event.getId(), registration.getTicketCode()));

    assertThat(result.alreadyCheckedIn()).isFalse();
    assertThat(result.name()).isEqualTo("Lu Mora");
    assertThat(result.ticketName()).isEqualTo("General");
    assertThat(result.checkedInAt()).isNotNull();
    assertThat(registration.getCheckInSource()).isEqualTo(CheckInSource.QR_TICKET);
  }

  @Test
  void scanningAgainSaysTheyAlreadyCameWithoutChangingTheTime() {
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    when(repository.findByTicketCode(registration.getTicketCode()))
        .thenReturn(Optional.of(registration));
    var command =
        new CheckInRegistrationByTicketCommand(event.getId(), registration.getTicketCode());
    var first = handler.handle(command);

    var second = handler.handle(command);

    assertThat(second.alreadyCheckedIn()).isTrue();
    assertThat(second.checkedInAt()).isEqualTo(first.checkedInAt());
  }

  @Test
  void aCancelledSignUpCannotCheckIn() {
    for (var status : List.of(RegistrationStatus.CANCELLED)) {
      var registration = registration(event, ticket(event, "General", 5), "lu@x.org", status);
      when(repository.findByTicketCode(registration.getTicketCode()))
          .thenReturn(Optional.of(registration));

      assertThatThrownBy(
              () ->
                  handler.handle(
                      new CheckInRegistrationByTicketCommand(
                          event.getId(), registration.getTicketCode())))
          .isInstanceOf(DomainValidationException.class);
      assertThat(registration.isCheckedIn()).isFalse();
    }
  }

  @Test
  void aTicketOfAnotherEventIsNotFound() {
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    when(repository.findByTicketCode(registration.getTicketCode()))
        .thenReturn(Optional.of(registration));

    assertThatThrownBy(
            () ->
                handler.handle(
                    new CheckInRegistrationByTicketCommand(
                        EventId.generate(), registration.getTicketCode())))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void theCancelCodeIsNotATicket() {
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    when(repository.findByTicketCode(registration.getCancelCode())).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                handler.handle(
                    new CheckInRegistrationByTicketCommand(
                        event.getId(), registration.getCancelCode())))
        .isInstanceOf(NoSuchElementException.class);
  }

  private Event subEvent(RegistrationMode mode) {
    var child = childOf(event, EventId.generate(), "Taller", event.getStart(), event.getEnd());
    child.setRegistrationMode(mode);
    when(eventRepository.findById(child.getId())).thenReturn(Optional.of(child));
    return child;
  }

  @Test
  void theContainersTicketIsValidInASubEventThatInheritsAndLeavesTheSignUpUntouched() {
    var child = subEvent(RegistrationMode.INHERIT);
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    when(repository.findByTicketCode(registration.getTicketCode()))
        .thenReturn(Optional.of(registration));

    var result =
        handler.handle(
            new CheckInRegistrationByTicketCommand(child.getId(), registration.getTicketCode()));

    assertThat(result.inherited()).isTrue();
    assertThat(result.name()).isEqualTo("Lu Mora");
    assertThat(result.alreadyCheckedIn()).isFalse();
    assertThat(registration.isCheckedIn()).isFalse();
  }

  @Test
  void aSubEventWithItsOwnRegistrationDoesNotAcceptTheContainersTicket() {
    var child = subEvent(RegistrationMode.OWN);
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    when(repository.findByTicketCode(registration.getTicketCode()))
        .thenReturn(Optional.of(registration));

    assertThatThrownBy(
            () ->
                handler.handle(
                    new CheckInRegistrationByTicketCommand(
                        child.getId(), registration.getTicketCode())))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void anOpenSubEventDoesNotNeedTheContainersTicket() {
    var child = subEvent(RegistrationMode.OPEN);
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    when(repository.findByTicketCode(registration.getTicketCode()))
        .thenReturn(Optional.of(registration));

    assertThatThrownBy(
            () ->
                handler.handle(
                    new CheckInRegistrationByTicketCommand(
                        child.getId(), registration.getTicketCode())))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void aCancelledContainerSignUpDoesNotEnterASubEvent() {
    var child = subEvent(RegistrationMode.INHERIT);
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CANCELLED);
    when(repository.findByTicketCode(registration.getTicketCode()))
        .thenReturn(Optional.of(registration));

    assertThatThrownBy(
            () ->
                handler.handle(
                    new CheckInRegistrationByTicketCommand(
                        child.getId(), registration.getTicketCode())))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void aTicketOfAnUnrelatedEventStillDoesNotWorkInASubEvent() {
    var other =
        singleEvent(
            EventId.generate(),
            "Otro",
            organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
            List.of());
    var child = subEvent(RegistrationMode.INHERIT);
    var registration =
        registration(other, ticket(other, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    when(repository.findByTicketCode(registration.getTicketCode()))
        .thenReturn(Optional.of(registration));

    assertThatThrownBy(
            () ->
                handler.handle(
                    new CheckInRegistrationByTicketCommand(
                        child.getId(), registration.getTicketCode())))
        .isInstanceOf(NoSuchElementException.class);
  }
}
