package org.ouracademy.ovejafiel.event.command.remove_event_registration;

import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationSetupRepository;
import org.ouracademy.ovejafiel.event.repository.TicketTypeRepository;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class RemoveEventRegistrationCommandHandlerImpl implements RemoveEventRegistrationCommandHandler {

  private final EventRegistrationSetupRepository setupRepository;
  private final TicketTypeRepository ticketTypeRepository;
  private final EventRegistrationRepository eventRegistrationRepository;

  @Override
  @Transactional
  public RemoveEventRegistrationResult handle(RemoveEventRegistrationCommand command) {
    setupRepository
        .lockByEventId(command.eventId())
        .ifPresent(
            setup -> {
              if (eventRegistrationRepository.existsByEventIdAndStatusNot(
                  command.eventId(), RegistrationStatus.CANCELLED)) {
                throw new DomainValidationException(
                    "Hay inscritos o personas en espera: cierre la inscripción en lugar de quitarla");
              }
              // What is left are the cancelled sign-ups, kept only as history.
              eventRegistrationRepository.deleteByEventId(command.eventId());
              eventRegistrationRepository.flush();
              ticketTypeRepository.deleteAll(
                  ticketTypeRepository.findByEventIdOrderByPosition(command.eventId()));
              ticketTypeRepository.flush();
              setupRepository.delete(setup);
            });
    return new RemoveEventRegistrationResult();
  }
}
