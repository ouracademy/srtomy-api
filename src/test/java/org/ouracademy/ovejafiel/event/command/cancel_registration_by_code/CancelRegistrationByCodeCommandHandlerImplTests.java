package org.ouracademy.ovejafiel.event.command.cancel_registration_by_code;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationSetupRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;

class CancelRegistrationByCodeCommandHandlerImplTests {

  EventRegistrationRepository repository = mock(EventRegistrationRepository.class);
  EventRegistrationSetupRepository setups = mock(EventRegistrationSetupRepository.class);
  WaitlistPromotion promotion = mock(WaitlistPromotion.class);
  CancelRegistrationByCodeCommandHandlerImpl handler =
      new CancelRegistrationByCodeCommandHandlerImpl(repository, setups, promotion);

  Event event =
      singleEvent(
          EventId.generate(),
          "Curso",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  @Test
  void theCodeCancelsTheSignUpAndFreesItsSeat() {
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    var setup = registrationSetup(event, true, null);
    when(repository.findByCancelCode(registration.getCancelCode()))
        .thenReturn(Optional.of(registration));
    when(setups.lockByEventId(event.getId())).thenReturn(Optional.of(setup));

    handler.handle(new CancelRegistrationByCodeCommand(registration.getCancelCode()));

    assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
    verify(promotion).promote(setup);
  }

  @Test
  void theTicketCodeCannotCancel() {
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CONFIRMED);
    when(repository.findByCancelCode(registration.getTicketCode())).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> handler.handle(new CancelRegistrationByCodeCommand(registration.getTicketCode())))
        .isInstanceOf(NoSuchElementException.class);
    assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CONFIRMED);
  }

  @Test
  void cancellingAnAlreadyCancelledSignUpIsNotAnError() {
    var registration =
        registration(event, ticket(event, "General", 5), "lu@x.org", RegistrationStatus.CANCELLED);
    when(repository.findByCancelCode(registration.getCancelCode()))
        .thenReturn(Optional.of(registration));
    when(setups.lockByEventId(event.getId())).thenReturn(Optional.empty());

    handler.handle(new CancelRegistrationByCodeCommand(registration.getCancelCode()));

    verify(promotion, never()).promote(any());
  }

  @Test
  void aMissingCodeIsNotFound() {
    assertThatThrownBy(() -> handler.handle(new CancelRegistrationByCodeCommand(null)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
