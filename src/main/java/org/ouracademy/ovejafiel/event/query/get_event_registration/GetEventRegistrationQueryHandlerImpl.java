package org.ouracademy.ovejafiel.event.query.get_event_registration;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.query.Seats;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationSetupRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.event.repository.TicketTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class GetEventRegistrationQueryHandlerImpl implements GetEventRegistrationQueryHandler {

  private final EventRepository eventRepository;
  private final EventRegistrationSetupRepository setupRepository;
  private final TicketTypeRepository ticketTypeRepository;
  private final EventRegistrationRepository eventRegistrationRepository;
  private final Seats seats;

  @Override
  @Transactional(readOnly = true)
  public Optional<GetEventRegistrationResult> handle(GetEventRegistrationQuery query) {
    return eventRepository
        .findById(query.eventId())
        .map(
            event ->
                setupRepository
                    .findByEventId(event.getId())
                    .map(
                        setup -> {
                          var sold = seats.confirmedByTicket(event.getId());
                          var tickets =
                              ticketTypeRepository
                                  .findByEventIdOrderByPosition(event.getId())
                                  .stream()
                                  .map(
                                      ticket ->
                                          new GetEventRegistrationResult.Ticket(
                                              ticket.getId(),
                                              ticket.getName(),
                                              ticket.getPriceCents(),
                                              ticket.getCurrency(),
                                              ticket.getQuantity(),
                                              ticket.getSalesStart(),
                                              ticket.getSalesEnd(),
                                              sold.getOrDefault(ticket.getId(), 0L)))
                                  .toList();
                          return new GetEventRegistrationResult(
                              true,
                              setup.isOpen(),
                              setup.isRequiresAccount(),
                              eventRegistrationRepository.countByEventIdAndStatus(
                                  event.getId(), RegistrationStatus.CONFIRMED),
                              eventRegistrationRepository.countByEventIdAndStatus(
                                  event.getId(), RegistrationStatus.WAITLISTED),
                              eventRegistrationRepository.countCheckedIn(event.getId()),
                              tickets);
                        })
                    .orElseGet(GetEventRegistrationResult::disabled));
  }
}
