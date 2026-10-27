package org.ouracademy.ovejafiel.attendance.command.mark_person_present;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.attendance.repository.EventAttendanceRepository;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQuery;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQueryHandler;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.person.query.find_people_by_ids.FindPeopleByIdsQuery;
import org.ouracademy.ovejafiel.person.query.find_people_by_ids.FindPeopleByIdsQueryHandler;
import org.ouracademy.ovejafiel.user.domain.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class MarkPersonPresentCommandHandlerImpl implements MarkPersonPresentCommandHandler {

  private final EventAttendanceRepository eventAttendanceRepository;
  private final FindEventByIdQueryHandler findEventByIdQueryHandler;
  private final FindPeopleByIdsQueryHandler findPeopleByIdsQueryHandler;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public MarkPersonPresentResult handle(MarkPersonPresentCommand command) {
    findEventByIdQueryHandler
        .handle(new FindEventByIdQuery(command.eventId()))
        .orElseThrow(
            () -> new NoSuchElementException("Event not found: " + command.eventId().value()));
    if (findPeopleByIdsQueryHandler
        .handle(new FindPeopleByIdsQuery(List.of(command.personId())))
        .isEmpty()) {
      throw new NoSuchElementException("Person not found: " + command.personId().value());
    }

    eventAttendanceRepository.createIfMissing(command.eventId().value());
    var attendance =
        eventAttendanceRepository
            .findByEventId(command.eventId())
            .orElseThrow(
                () -> new NoSuchElementException("Event not found: " + command.eventId().value()));
    var added =
        attendance.markPresent(
            entityManager.getReference(Person.class, command.personId()),
            command.source(),
            command.markedBy() == null
                ? null
                : entityManager.getReference(AppUser.class, command.markedBy()));
    eventAttendanceRepository.save(attendance);
    return new MarkPersonPresentResult(!added);
  }
}
