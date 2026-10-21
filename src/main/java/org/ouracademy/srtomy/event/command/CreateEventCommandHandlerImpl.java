package org.ouracademy.srtomy.event.command;

import jakarta.persistence.EntityManager;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.event.domain.Event;
import org.ouracademy.srtomy.event.domain.RecurrentEvent;
import org.ouracademy.srtomy.event.domain.SingleEvent;
import org.ouracademy.srtomy.event.repository.EventRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.FindOrganizationByIdQuery;
import org.ouracademy.srtomy.organization.query.FindOrganizationByIdQueryHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.domain.Person;
import org.ouracademy.srtomy.person.query.FindPeopleByIdsQuery;
import org.ouracademy.srtomy.person.query.FindPeopleByIdsQueryHandler;
import org.ouracademy.srtomy.person.query.FindPersonResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The organization and the speakers are validated through their modules' query handlers; the JPA
 * references for {@code Event.organization}/{@code Event.speakers} are then plain {@link
 * EntityManager#getReference} calls so this module never touches their repositories.
 */
@Service
@RequiredArgsConstructor
class CreateEventCommandHandlerImpl implements CreateEventCommandHandler {

  private final EventRepository eventRepository;
  private final FindOrganizationByIdQueryHandler findOrganizationByIdQueryHandler;
  private final FindPeopleByIdsQueryHandler findPeopleByIdsQueryHandler;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public CreateEventResult handle(CreateEventCommand command) {
    validateOrganizationExists(command.organizationId());
    validateSpeakersExist(command);

    var start = command.startTime().atZoneSameInstant(ZoneId.of("UTC"));
    var end = command.endTime().atZoneSameInstant(ZoneId.of("UTC"));

    var event =
        command.recurrent()
            ? new RecurrentEvent(
                command.name(), start, end, command.daysOfWeek(), command.endRecur())
            : new SingleEvent(command.name(), start, end);
    mapToModel(command, event);
    var id = eventRepository.save(event).getId();

    if (event instanceof RecurrentEvent recurrentEvent) {
      var events = recurrentEvent.expand(command.startTime(), command.endTime());
      eventRepository.saveAll(events);
    }

    return new CreateEventResult(id);
  }

  private void validateOrganizationExists(PartyId organizationId) {
    findOrganizationByIdQueryHandler
        .handle(new FindOrganizationByIdQuery(organizationId))
        .orElseThrow(
            () -> new NoSuchElementException("Organization not found: " + organizationId.value()));
  }

  private void validateSpeakersExist(CreateEventCommand command) {
    var found =
        findPeopleByIdsQueryHandler.handle(new FindPeopleByIdsQuery(command.speakerIds())).stream()
            .map(FindPersonResult::id)
            .toList();
    var missing = new HashSet<>(command.speakerIds());
    found.forEach(missing::remove);
    if (!missing.isEmpty()) {
      throw new NoSuchElementException(
          "Speakers not found: " + missing.stream().map(PartyId::value).toList());
    }
  }

  private void mapToModel(CreateEventCommand command, Event event) {
    event.setOrganization(entityManager.getReference(Organization.class, command.organizationId()));
    event.setAddress(command.address());
    event.setSpeakers(
        command.speakerIds().stream()
            .map(id -> entityManager.getReference(Person.class, id))
            .toList());
    event.setDescription(command.description());
    event.setIsACourse(command.isACourse());
  }
}
