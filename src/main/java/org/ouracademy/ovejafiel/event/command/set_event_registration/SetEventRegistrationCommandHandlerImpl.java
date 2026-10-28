package org.ouracademy.ovejafiel.event.command.set_event_registration;

import java.util.HashSet;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.command.WaitlistPromotion;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationSetup;
import org.ouracademy.ovejafiel.event.domain.TicketType;
import org.ouracademy.ovejafiel.event.domain.TicketTypeId;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationSetupRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.event.repository.TicketTypeRepository;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class SetEventRegistrationCommandHandlerImpl implements SetEventRegistrationCommandHandler {

  private final EventRepository eventRepository;
  private final EventRegistrationSetupRepository setupRepository;
  private final TicketTypeRepository ticketTypeRepository;
  private final EventRegistrationRepository eventRegistrationRepository;
  private final WaitlistPromotion waitlistPromotion;

  @Override
  @Transactional
  public SetEventRegistrationResult handle(SetEventRegistrationCommand command) {
    var event =
        eventRepository
            .findById(command.eventId())
            .orElseThrow(() -> new NoSuchElementException("Event not found"));
    requireValidTickets(command);

    var setup =
        setupRepository
            .lockByEventId(event.getId())
            .orElseGet(() -> setupRepository.save(new EventRegistrationSetup(event)));
    setup.configure(command.requiresAccount());

    var existing =
        ticketTypeRepository.findByEventIdOrderByPosition(event.getId()).stream()
            .collect(Collectors.toMap(TicketType::getId, Function.identity()));
    var kept = new HashSet<TicketTypeId>();
    var position = 0;
    for (var input : command.tickets()) {
      TicketType ticket;
      if (input.id() == null) {
        ticket = new TicketType(event, position);
      } else {
        ticket = existing.get(input.id());
        if (ticket == null) {
          throw new NoSuchElementException("Ticket not found");
        }
        kept.add(ticket.getId());
      }
      ticket.describe(
          input.name(),
          input.priceCents(),
          input.currency(),
          input.quantity(),
          input.salesStart(),
          input.salesEnd());
      ticket.moveTo(position++);
      ticketTypeRepository.save(ticket);
    }
    existing.values().stream()
        .filter(ticket -> !kept.contains(ticket.getId()))
        .forEach(
            ticket -> {
              if (eventRegistrationRepository.existsByTicketTypeId(ticket.getId())) {
                throw new DomainValidationException(
                    "El ticket «"
                        + ticket.getName()
                        + "» ya tiene inscripciones: ciérrelo en lugar de quitarlo");
              }
              ticketTypeRepository.delete(ticket);
            });

    // A larger limit may free seats for whoever is waiting.
    ticketTypeRepository.flush();
    if (setup.isOpen()) {
      waitlistPromotion.promote(setup);
    }
    return new SetEventRegistrationResult();
  }

  private static void requireValidTickets(SetEventRegistrationCommand command) {
    if (command.tickets() == null || command.tickets().isEmpty()) {
      throw new DomainValidationException("Agregue al menos un tipo de ticket");
    }
    var names = new HashSet<String>();
    for (var ticket : command.tickets()) {
      var name = ticket.name() == null ? "" : ticket.name().trim().toLowerCase(Locale.ROOT);
      if (!name.isEmpty() && !names.add(name)) {
        throw new DomainValidationException(
            "Hay dos tickets con el nombre «" + ticket.name() + "»");
      }
    }
  }
}
