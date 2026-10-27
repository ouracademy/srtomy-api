package org.ouracademy.ovejafiel.event.command.create_event;

import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventSeries;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.event.repository.EventSeriesRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.person.query.FindPersonResult;
import org.ouracademy.ovejafiel.person.query.find_people_by_ids.FindPeopleByIdsQuery;
import org.ouracademy.ovejafiel.person.query.find_people_by_ids.FindPeopleByIdsQueryHandler;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
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
  private final EventSeriesRepository eventSeriesRepository;
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
    if (command.recurrence() == null) {
      var event = new Event(command.name(), start, end);
      mapToModel(command, event);
      return new CreateEventResult(eventRepository.save(event).getId());
    }
    return createSeries(command);
  }

  private CreateEventResult createSeries(CreateEventCommand command) {
    var zone = command.timeZone();
    var series =
        new EventSeries(
            command.name(),
            command.startTime().atZoneSameInstant(zone).toLocalDateTime(),
            Duration.between(command.startTime(), command.endTime()),
            zone,
            command.recurrence());
    series.setOrganization(
        entityManager.getReference(Organization.class, command.organizationId()));
    series.setAddress(command.address());
    series.setDescription(command.description());
    series.setIsACourse(command.isACourse());
    series.setSpeakers(speakersOf(command));
    eventSeriesRepository.save(series);
    var occurrences = series.materialize(series.horizonFrom(LocalDate.now(zone)));
    if (occurrences.isEmpty()) {
      throw new DomainValidationException("La repetición no genera ningún evento");
    }
    return new CreateEventResult(eventRepository.saveAll(occurrences).getFirst().getId());
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
    event.setSpeakers(speakersOf(command));
    event.setDescription(command.description());
    event.setIsACourse(command.isACourse());
  }

  private List<Person> speakersOf(CreateEventCommand command) {
    return command.speakerIds().stream()
        .map(id -> entityManager.getReference(Person.class, id))
        .toList();
  }
}
