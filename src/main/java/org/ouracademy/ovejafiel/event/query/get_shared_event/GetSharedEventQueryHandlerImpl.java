package org.ouracademy.ovejafiel.event.query.get_shared_event;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventRegistration;
import org.ouracademy.ovejafiel.event.domain.RegistrationMode;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.query.EffectiveRegistration;
import org.ouracademy.ovejafiel.event.query.EventAccess;
import org.ouracademy.ovejafiel.event.query.RecurrenceResult;
import org.ouracademy.ovejafiel.event.query.Seats;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationSetupRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.event.repository.TicketTypeRepository;
import org.ouracademy.ovejafiel.user.query.UserResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetSharedEventQueryHandlerImpl implements GetSharedEventQueryHandler {

  private final EventRepository eventRepository;
  private final EventRegistrationSetupRepository setupRepository;
  private final TicketTypeRepository ticketTypeRepository;
  private final EventRegistrationRepository eventRegistrationRepository;
  private final EventAccess eventAccess;
  private final Seats seats;
  private final EffectiveRegistration effectiveRegistration;

  @Override
  @Transactional(readOnly = true)
  public Optional<GetSharedEventResult> handle(GetSharedEventQuery query) {
    return eventRepository
        .findByShareSlug(query.slug())
        .filter(event -> eventAccess.canOpen(query.subject(), event))
        .map(event -> result(query.subject(), event));
  }

  private GetSharedEventResult result(String subject, Event event) {
    var user = eventAccess.verifiedUser(subject);
    var governing = effectiveRegistration.governing(event);
    var registration = governing.map(this::registrationOf).orElse(null);
    var mine =
        user == null || governing.isEmpty()
            ? Optional.<EventRegistration>empty()
            : registrationOf(governing.get(), user);
    return new GetSharedEventResult(
        event.getTitle(),
        event.getDescription(),
        event.getAddress(),
        event.getStart(),
        event.getEnd(),
        Boolean.TRUE.equals(event.getIsACourse()),
        event.getOrganization().getName(),
        RecurrenceResult.from(event.getSeries()),
        registration,
        registration != null
            && registration.open()
            && !registration.tickets().isEmpty()
            && (!registration.requiresAccount() || user != null)
            && mine.isEmpty()
            && effectiveRegistration.isOwn(event),
        mine.map(EventRegistration::getStatus).orElse(null),
        programOf(subject, event),
        containerOf(subject, event));
  }

  private List<GetSharedEventResult.ProgramItem> programOf(String subject, Event event) {
    if (event.isChild()) {
      return List.of();
    }
    return eventRepository.findByParentOrderByStart(event).stream()
        .filter(child -> eventAccess.canOpen(subject, child))
        .map(
            child ->
                new GetSharedEventResult.ProgramItem(
                    child.getShareSlug(),
                    child.getTitle(),
                    child.getStart(),
                    child.getEnd(),
                    Boolean.TRUE.equals(child.getIsACourse()),
                    child.getRegistrationMode(),
                    child.getRegistrationMode() == RegistrationMode.INHERIT))
        .toList();
  }

  private GetSharedEventResult.Container containerOf(String subject, Event event) {
    var parent = event.getParent();
    if (parent == null || !eventAccess.canOpen(subject, parent)) {
      return null;
    }
    return new GetSharedEventResult.Container(parent.getShareSlug(), parent.getTitle());
  }

  private Optional<EventRegistration> registrationOf(Event event, UserResult user) {
    return eventRegistrationRepository
        .findFirstByEventIdAndUserIdAndStatusNot(
            event.getId(), user.id(), RegistrationStatus.CANCELLED)
        .or(
            () ->
                eventRegistrationRepository.findFirstByEventIdAndEmailIgnoreCaseAndStatusNot(
                    event.getId(), user.email(), RegistrationStatus.CANCELLED));
  }

  private GetSharedEventResult.Registration registrationOf(Event event) {
    return setupRepository
        .findByEventId(event.getId())
        .map(
            setup -> {
              var sold = seats.confirmedByTicket(event.getId());
              var now = ZonedDateTime.now();
              var tickets =
                  ticketTypeRepository.findByEventIdOrderByPosition(event.getId()).stream()
                      .map(
                          ticket ->
                              new GetSharedEventResult.Ticket(
                                  ticket.getId(),
                                  ticket.getName(),
                                  ticket.getPriceCents(),
                                  ticket.getCurrency(),
                                  ticket.isOnSale(now),
                                  seats.remaining(setup, ticket, sold)))
                      .toList();
              var soldOut =
                  tickets.stream()
                      .filter(GetSharedEventResult.Ticket::onSale)
                      .allMatch(ticket -> ticket.remaining() != null && ticket.remaining() == 0);
              return new GetSharedEventResult.Registration(
                  setup.isOpen(), setup.isRequiresAccount(), soldOut, tickets);
            })
        .orElse(null);
  }
}
