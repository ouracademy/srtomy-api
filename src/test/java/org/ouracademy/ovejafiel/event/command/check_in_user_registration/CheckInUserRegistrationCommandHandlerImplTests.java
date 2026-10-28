package org.ouracademy.ovejafiel.event.command.check_in_user_registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.registration;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.event.EventFixtures.ticket;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.CheckInSource;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.user.domain.UserId;

class CheckInUserRegistrationCommandHandlerImplTests {

  EventRegistrationRepository repository = mock(EventRegistrationRepository.class);
  CheckInUserRegistrationCommandHandlerImpl handler =
      new CheckInUserRegistrationCommandHandlerImpl(repository);

  Event event =
      singleEvent(
          EventId.generate(),
          "Curso",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());
  UserId userId = UserId.generate();

  @Test
  void scanningTheEventQrChecksInTheSeatOfThatAccount() {
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    when(repository.findFirstByEventIdAndUserIdAndStatusNot(
            event.getId(), userId, RegistrationStatus.CANCELLED))
        .thenReturn(Optional.of(registration));

    var result =
        handler.handle(new CheckInUserRegistrationCommand(event.getId(), userId, "lu@x.org"));

    assertThat(result).isPresent();
    assertThat(registration.getCheckInSource()).isEqualTo(CheckInSource.QR_EVENT);
  }

  @Test
  void fallsBackToTheVerifiedEmailWhenTheSignUpHadNoAccount() {
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    when(repository.findFirstByEventIdAndUserIdAndStatusNot(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(repository.findFirstByEventIdAndEmailIgnoreCaseAndStatusNot(
            event.getId(), "lu@x.org", RegistrationStatus.CANCELLED))
        .thenReturn(Optional.of(registration));

    assertThat(
            handler.handle(new CheckInUserRegistrationCommand(event.getId(), userId, "lu@x.org")))
        .isPresent();
    assertThat(registration.isCheckedIn()).isTrue();
  }

  @Test
  void whoHasNoSeatIsNotCheckedIn() {
    when(repository.findFirstByEventIdAndUserIdAndStatusNot(any(), any(), any()))
        .thenReturn(Optional.empty());
    when(repository.findFirstByEventIdAndEmailIgnoreCaseAndStatusNot(any(), any(), any()))
        .thenReturn(Optional.empty());

    assertThat(handler.handle(new CheckInUserRegistrationCommand(event.getId(), userId, "x@x.org")))
        .isEmpty();
    assertThat(handler.handle(new CheckInUserRegistrationCommand(event.getId(), userId, null)))
        .isEmpty();
  }
}
