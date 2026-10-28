package org.ouracademy.ovejafiel.event.command.set_event_registration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.registrationSetup;
import static org.ouracademy.ovejafiel.event.EventFixtures.singleEvent;
import static org.ouracademy.ovejafiel.event.EventFixtures.ticket;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.ovejafiel.event.command.WaitlistPromotion;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationSetup;
import org.ouracademy.ovejafiel.event.domain.TicketType;
import org.ouracademy.ovejafiel.event.domain.TicketTypeId;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationSetupRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.event.repository.TicketTypeRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

class SetEventRegistrationCommandHandlerImplTests {

  EventRepository events = mock(EventRepository.class);
  EventRegistrationSetupRepository setups = mock(EventRegistrationSetupRepository.class);
  TicketTypeRepository tickets = mock(TicketTypeRepository.class);
  EventRegistrationRepository registrations = mock(EventRegistrationRepository.class);
  WaitlistPromotion promotion = mock(WaitlistPromotion.class);
  SetEventRegistrationCommandHandlerImpl handler =
      new SetEventRegistrationCommandHandlerImpl(events, setups, tickets, registrations, promotion);

  EventId id = EventId.generate();
  Event event =
      singleEvent(
          id,
          "Curso",
          organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH),
          List.of());

  @BeforeEach
  void setUp() {
    when(events.findById(id)).thenReturn(Optional.of(event));
    when(setups.lockByEventId(id)).thenReturn(Optional.empty());
    when(setups.save(any(EventRegistrationSetup.class))).thenAnswer(call -> call.getArgument(0));
    when(tickets.findByEventIdOrderByPosition(id)).thenReturn(List.of());
  }

  SetEventRegistrationCommand.Ticket newTicket(String name, int price, Integer quantity) {
    return new SetEventRegistrationCommand.Ticket(null, name, price, "PEN", quantity, null, null);
  }

  SetEventRegistrationCommand command(SetEventRegistrationCommand.Ticket... tickets) {
    return new SetEventRegistrationCommand(id, false, List.of(tickets));
  }

  @Test
  void turnsOnTheRegistrationWithItsTicketsInOrder() {
    handler.handle(command(newTicket("General", 0, 30), newTicket("VIP", 2000, 10)));

    var setup = ArgumentCaptor.forClass(EventRegistrationSetup.class);
    verify(setups).save(setup.capture());
    assertThat(setup.getValue().isOpen()).isTrue();
    assertThat(setup.getValue().isRequiresAccount()).isFalse();
    assertThat(setup.getValue().isWaitlistEnabled()).isFalse();
    assertThat(setup.getValue().getCapacity()).isNull();
    var saved = ArgumentCaptor.forClass(TicketType.class);
    verify(tickets, org.mockito.Mockito.times(2)).save(saved.capture());
    assertThat(saved.getAllValues())
        .extracting(TicketType::getName, TicketType::getPriceCents, TicketType::getPosition)
        .containsExactly(
            org.assertj.core.api.Assertions.tuple("General", 0, 0),
            org.assertj.core.api.Assertions.tuple("VIP", 2000, 1));
    verify(promotion).promote(setup.getValue());
  }

  @Test
  void changesAnExistingSetupAndUpdatesItsTickets() {
    var setup = registrationSetup(event, false, null);
    var general = ticket(event, "General", 5);
    when(setups.lockByEventId(id)).thenReturn(Optional.of(setup));
    when(tickets.findByEventIdOrderByPosition(id)).thenReturn(List.of(general));

    handler.handle(
        command(
            new SetEventRegistrationCommand.Ticket(
                general.getId(), "Entrada", 1500, "usd", 8, null, null)));

    assertThat(setup.getCapacity()).isNull();
    assertThat(general.getName()).isEqualTo("Entrada");
    assertThat(general.getCurrency()).isEqualTo("USD");
    assertThat(general.getQuantity()).isEqualTo(8);
    verify(setups, never()).save(any());
  }

  @Test
  void removesATicketNobodyHolds() {
    var setup = registrationSetup(event, false, null);
    var kept = ticket(event, "General", 5);
    var dropped = ticket(event, "Antiguo", 5);
    when(setups.lockByEventId(id)).thenReturn(Optional.of(setup));
    when(tickets.findByEventIdOrderByPosition(id)).thenReturn(List.of(kept, dropped));
    when(registrations.existsByTicketTypeId(dropped.getId())).thenReturn(false);

    handler.handle(
        command(
            new SetEventRegistrationCommand.Ticket(
                kept.getId(), "General", 0, "PEN", 5, null, null)));

    verify(tickets).delete(dropped);
  }

  @Test
  void aTicketWithSignUpsCannotBeRemoved() {
    var setup = registrationSetup(event, false, null);
    var kept = ticket(event, "General", 5);
    var held = ticket(event, "VIP", 5);
    when(setups.lockByEventId(id)).thenReturn(Optional.of(setup));
    when(tickets.findByEventIdOrderByPosition(id)).thenReturn(List.of(kept, held));
    when(registrations.existsByTicketTypeId(held.getId())).thenReturn(true);

    assertThatThrownBy(
            () ->
                handler.handle(
                    command(
                        new SetEventRegistrationCommand.Ticket(
                            kept.getId(), "General", 0, "PEN", 5, null, null))))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("VIP");
    verify(tickets, never()).delete(held);
  }

  @Test
  void needsAtLeastOneTicket() {
    assertThatThrownBy(() -> handler.handle(command()))
        .isInstanceOf(DomainValidationException.class);
    verify(setups, never()).save(any());
  }

  @Test
  void ticketNamesCannotRepeatIgnoringCase() {
    assertThatThrownBy(
            () -> handler.handle(command(newTicket("General", 0, 1), newTicket(" general", 0, 1))))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("general");
  }

  @Test
  void aTicketThatIsNotOfThisEventIsNotFound() {
    assertThatThrownBy(
            () ->
                handler.handle(
                    command(
                        new SetEventRegistrationCommand.Ticket(
                            TicketTypeId.generate(), "Ajeno", 0, "PEN", 1, null, null))))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void anInvalidTicketIsRefused() {
    assertThatThrownBy(() -> handler.handle(command(newTicket(" ", 0, 1))))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> handler.handle(command(newTicket("General", -1, 1))))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> handler.handle(command(newTicket("General", 0, 0))))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void aClosedRegistrationPromotesNobody() {
    var setup = registrationSetup(event, false, null);
    setup.setOpen(false);
    when(setups.lockByEventId(id)).thenReturn(Optional.of(setup));

    handler.handle(command(newTicket("General", 0, null)));

    assertThat(setup.isOpen()).isFalse();
    verify(promotion, never()).promote(any());
  }

  @Test
  void savingTheSetupKeepsItOpenOrClosedAsItWas() {
    var setup = registrationSetup(event, false, null);
    when(setups.lockByEventId(id)).thenReturn(Optional.of(setup));
    handler.handle(command(newTicket("General", 0, null)));
    assertThat(setup.isOpen()).isTrue();
  }

  @Test
  void anUnknownEventIsNotFound() {
    when(events.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(command(newTicket("General", 0, 1))))
        .isInstanceOf(NoSuchElementException.class);
  }
}
