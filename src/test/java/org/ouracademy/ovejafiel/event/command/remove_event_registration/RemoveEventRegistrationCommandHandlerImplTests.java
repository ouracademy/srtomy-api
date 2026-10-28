package org.ouracademy.ovejafiel.event.command.remove_event_registration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.registrationSetup;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.event.EventFixtures.ticket;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationSetupRepository;
import org.ouracademy.ovejafiel.event.repository.TicketTypeRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

class RemoveEventRegistrationCommandHandlerImplTests {

  EventRegistrationSetupRepository setups = mock(EventRegistrationSetupRepository.class);
  TicketTypeRepository tickets = mock(TicketTypeRepository.class);
  EventRegistrationRepository registrations = mock(EventRegistrationRepository.class);
  RemoveEventRegistrationCommandHandlerImpl handler =
      new RemoveEventRegistrationCommandHandlerImpl(setups, tickets, registrations);

  EventId id = EventId.generate();
  Event event =
      singleEvent(
          id,
          "Curso",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  @Test
  void removesTheSetupItsTicketsAndTheCancelledHistory() {
    var setup = registrationSetup(event);
    var general = ticket(event, "General", 5);
    when(setups.lockByEventId(id)).thenReturn(Optional.of(setup));
    when(registrations.existsByEventIdAndStatusNot(id, RegistrationStatus.CANCELLED))
        .thenReturn(false);
    when(tickets.findByEventIdOrderByPosition(id)).thenReturn(List.of(general));

    handler.handle(new RemoveEventRegistrationCommand(id));

    verify(registrations).deleteByEventId(id);
    verify(tickets).deleteAll(List.of(general));
    verify(setups).delete(setup);
  }

  @Test
  void whileSomeoneHoldsOrAwaitsASeatItCannotBeRemoved() {
    when(setups.lockByEventId(id)).thenReturn(Optional.of(registrationSetup(event)));
    when(registrations.existsByEventIdAndStatusNot(id, RegistrationStatus.CANCELLED))
        .thenReturn(true);

    assertThatThrownBy(() -> handler.handle(new RemoveEventRegistrationCommand(id)))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("cierre");
    verify(setups, never()).delete(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void removingWhatDoesNotExistIsNotAnError() {
    when(setups.lockByEventId(id)).thenReturn(Optional.empty());

    handler.handle(new RemoveEventRegistrationCommand(id));

    verify(setups, never()).delete(org.mockito.ArgumentMatchers.any());
  }
}
