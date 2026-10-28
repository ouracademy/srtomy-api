package org.ouracademy.ovejafiel.event.command.create_event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;
import static org.ouracademy.ovejafiel.person.PersonFixtures.person;
import static org.ouracademy.ovejafiel.person.PersonFixtures.personResult;

import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.event.EventFixtures;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventSeries;
import org.ouracademy.ovejafiel.event.domain.Recurrence;
import org.ouracademy.ovejafiel.event.domain.RegistrationMode;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.event.repository.EventSeriesRepository;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.organization.query.FindOrganizationResult;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_subtree.FindOrganizationSubtreeResult;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.person.query.find_people_by_ids.FindPeopleByIdsQuery;
import org.ouracademy.ovejafiel.person.query.find_people_by_ids.FindPeopleByIdsQueryHandler;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;

@ExtendWith(MockitoExtension.class)
class CreateEventCommandHandlerImplTests {

  static final OffsetDateTime START = OffsetDateTime.parse("2026-01-05T10:00:00-05:00");
  static final OffsetDateTime END = OffsetDateTime.parse("2026-01-05T12:00:00-05:00");

  @Mock EventRepository eventRepository;
  @Mock EventSeriesRepository eventSeriesRepository;
  @Mock FindOrganizationByIdQueryHandler findOrganizationByIdQueryHandler;
  @Mock FindPeopleByIdsQueryHandler findPeopleByIdsQueryHandler;
  @Mock FindOrganizationSubtreeQueryHandler findOrganizationSubtreeQueryHandler;
  @Mock EntityManager entityManager;

  CreateEventCommandHandlerImpl handler;

  PartyId organizationId = PartyId.generate();
  PartyId speakerId = PartyId.generate();
  Organization church = organization(organizationId, "Iglesia", Organization.Type.CHURCH);
  Person speaker = person(speakerId, "Jose", "Mena");

  @BeforeEach
  void setUp() {
    handler =
        new CreateEventCommandHandlerImpl(
            eventRepository,
            eventSeriesRepository,
            findOrganizationByIdQueryHandler,
            findPeopleByIdsQueryHandler,
            findOrganizationSubtreeQueryHandler,
            entityManager);
  }

  private CreateEventCommand command(
      List<PartyId> speakerIds, Recurrence recurrence, ZoneId timeZone) {
    return new CreateEventCommand(
        "Servicio",
        START,
        END,
        "Av. 1",
        "Descripcion",
        true,
        organizationId,
        speakerIds,
        recurrence,
        timeZone,
        null,
        null);
  }

  private void organizationAndSpeakerExist() {
    when(findOrganizationByIdQueryHandler.handle(new FindOrganizationByIdQuery(organizationId)))
        .thenReturn(Optional.of(FindOrganizationResult.from(church)));
    when(findPeopleByIdsQueryHandler.handle(new FindPeopleByIdsQuery(List.of(speakerId))))
        .thenReturn(List.of(personResult(speakerId, "Jose", "Mena")));
    when(entityManager.getReference(Organization.class, organizationId)).thenReturn(church);
    when(entityManager.getReference(Person.class, speakerId)).thenReturn(speaker);
  }

  @Test
  void handleSavesASingleEventInUtcReferencingTheOrganizationAndSpeakers() {
    organizationAndSpeakerExist();
    when(eventRepository.save(any(Event.class))).thenAnswer(call -> call.getArgument(0));

    var result = handler.handle(command(List.of(speakerId), null, ZoneId.of("UTC")));

    var saved = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(saved.capture());
    assertThat(saved.getValue().getSeries()).isNull();
    var event = saved.getValue();
    assertThat(event.getId()).isEqualTo(result.id());
    assertThat(event.getTitle()).isEqualTo("Servicio");
    assertThat(event.getStart()).isEqualTo(ZonedDateTime.parse("2026-01-05T15:00:00Z[UTC]"));
    assertThat(event.getEnd()).isEqualTo(ZonedDateTime.parse("2026-01-05T17:00:00Z[UTC]"));
    assertThat(event.getOrganization()).isSameAs(church);
    assertThat(event.getSpeakers()).containsExactly(speaker);
    assertThat(event.getAddress()).isEqualTo("Av. 1");
    assertThat(event.getDescription()).isEqualTo("Descripcion");
    assertThat(event.getIsACourse()).isTrue();
    verify(eventRepository, never()).saveAll(any());
    verifyNoInteractions(eventSeriesRepository);
  }

  @Test
  @SuppressWarnings("unchecked")
  void handleSavesASeriesWithItsOccurrencesForAFiniteRecurrence() {
    organizationAndSpeakerExist();
    when(eventRepository.saveAll(any())).thenAnswer(call -> call.getArgument(0));

    var recurrence =
        Recurrence.of(
            Recurrence.Frequency.WEEKLY,
            1,
            List.of(1, 3),
            null,
            Recurrence.EndType.UNTIL,
            LocalDate.of(2026, 1, 14),
            null);
    var result = handler.handle(command(List.of(speakerId), recurrence, ZoneId.of("America/Lima")));

    var series = ArgumentCaptor.forClass(EventSeries.class);
    verify(eventSeriesRepository).save(series.capture());
    assertThat(series.getValue().getTimeZone()).isEqualTo("America/Lima");
    assertThat(series.getValue().getStartLocal()).isEqualTo(LocalDateTime.of(2026, 1, 5, 10, 0));
    assertThat(series.getValue().duration()).isEqualTo(Duration.ofHours(2));
    assertThat(series.getValue().getSpeakers()).containsExactly(speaker);

    ArgumentCaptor<Iterable<Event>> saved = ArgumentCaptor.forClass(Iterable.class);
    verify(eventRepository).saveAll(saved.capture());
    var occurrences = (List<Event>) saved.getValue();
    // Mondays and Wednesdays from Jan 5 to Jan 14, 2026, at 10am in Lima.
    assertThat(occurrences)
        .extracting(Event::getStart)
        .containsExactly(
            ZonedDateTime.parse("2026-01-05T15:00:00Z[UTC]"),
            ZonedDateTime.parse("2026-01-07T15:00:00Z[UTC]"),
            ZonedDateTime.parse("2026-01-12T15:00:00Z[UTC]"),
            ZonedDateTime.parse("2026-01-14T15:00:00Z[UTC]"));
    assertThat(occurrences)
        .allSatisfy(
            occurrence -> {
              assertThat(occurrence.getSeries()).isSameAs(series.getValue());
              assertThat(occurrence.getOrganization()).isSameAs(church);
              assertThat(occurrence.getSpeakers()).containsExactly(speaker);
              assertThat(occurrence.duration()).isEqualTo(Duration.ofHours(2));
            });
    assertThat(result.id()).isEqualTo(occurrences.getFirst().getId());
    verify(eventRepository, never()).save(any());
  }

  @Test
  void aSeriesThatNeverEndsGetsAYearOfOccurrences() {
    organizationAndSpeakerExist();
    when(eventRepository.saveAll(any())).thenAnswer(call -> call.getArgument(0));
    var daily =
        Recurrence.of(
            Recurrence.Frequency.DAILY, 1, null, null, Recurrence.EndType.NEVER, null, null);
    var tomorrow = OffsetDateTime.now().plusDays(1);

    handler.handle(
        new CreateEventCommand(
            "Oración",
            tomorrow,
            tomorrow.plusHours(1),
            null,
            null,
            false,
            organizationId,
            List.of(speakerId),
            daily,
            ZoneId.of("UTC"),
            null,
            null));

    var series = ArgumentCaptor.forClass(EventSeries.class);
    verify(eventSeriesRepository).save(series.capture());
    assertThat(series.getValue().isFullyMaterialized()).isFalse();
    assertThat(series.getValue().getMaterializedUntil())
        .isEqualTo(series.getValue().getStartLocal().toLocalDate().plusMonths(12));
  }

  @Test
  void aRecurrenceWithoutOccurrencesIsRejected() {
    organizationAndSpeakerExist();
    var neverHappens =
        Recurrence.of(
            Recurrence.Frequency.DAILY,
            1,
            null,
            null,
            Recurrence.EndType.UNTIL,
            LocalDate.of(2025, 12, 31),
            null);

    assertThatThrownBy(
            () -> handler.handle(command(List.of(speakerId), neverHappens, ZoneId.of("UTC"))))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void handleFailsWhenTheOrganizationDoesNotExist() {
    when(findOrganizationByIdQueryHandler.handle(new FindOrganizationByIdQuery(organizationId)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(command(List.of(speakerId), null, ZoneId.of("UTC"))))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining(organizationId.value().toString());

    verifyNoInteractions(eventRepository, entityManager, findPeopleByIdsQueryHandler);
  }

  @Test
  void handleFailsWhenASpeakerDoesNotExist() {
    var missingSpeakerId = PartyId.generate();
    when(findOrganizationByIdQueryHandler.handle(new FindOrganizationByIdQuery(organizationId)))
        .thenReturn(Optional.of(FindOrganizationResult.from(church)));
    when(findPeopleByIdsQueryHandler.handle(
            new FindPeopleByIdsQuery(List.of(speakerId, missingSpeakerId))))
        .thenReturn(List.of(personResult(speakerId, "Jose", "Mena")));

    assertThatThrownBy(
            () ->
                handler.handle(
                    command(List.of(speakerId, missingSpeakerId), null, ZoneId.of("UTC"))))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining(missingSpeakerId.value().toString())
        .hasMessageNotContaining(speakerId.value().toString());

    verifyNoInteractions(eventRepository, entityManager);
  }

  private Event conference() {
    return EventFixtures.eventBetween(
        EventId.generate(),
        "Mega evento",
        church,
        START.minusDays(1).atZoneSameInstant(ZoneId.of("UTC")),
        END.plusDays(1).atZoneSameInstant(ZoneId.of("UTC")));
  }

  private CreateEventCommand childCommand(
      EventId parentId, Recurrence recurrence, RegistrationMode mode) {
    return new CreateEventCommand(
        "Taller",
        START,
        END,
        null,
        null,
        false,
        organizationId,
        List.of(speakerId),
        recurrence,
        ZoneId.of("UTC"),
        parentId,
        mode);
  }

  @Test
  void aSubEventIsAttachedToItsContainerWithTheRequestedRegistrationMode() {
    organizationAndSpeakerExist();
    var container = conference();
    when(eventRepository.findById(container.getId())).thenReturn(Optional.of(container));
    when(findOrganizationSubtreeQueryHandler.handle(
            new FindOrganizationSubtreeQuery(organizationId)))
        .thenReturn(new FindOrganizationSubtreeResult(List.of(organizationId)));
    when(eventRepository.save(any())).thenAnswer(call -> call.getArgument(0));

    handler.handle(childCommand(container.getId(), null, RegistrationMode.OWN));

    var saved = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(saved.capture());
    assertThat(saved.getValue().getParent()).isSameAs(container);
    assertThat(saved.getValue().getRegistrationMode()).isEqualTo(RegistrationMode.OWN);
  }

  @Test
  void aSubEventInheritsTheContainersRegistrationWhenNoModeIsGiven() {
    organizationAndSpeakerExist();
    var container = conference();
    when(eventRepository.findById(container.getId())).thenReturn(Optional.of(container));
    when(findOrganizationSubtreeQueryHandler.handle(
            new FindOrganizationSubtreeQuery(organizationId)))
        .thenReturn(new FindOrganizationSubtreeResult(List.of(organizationId)));
    when(eventRepository.save(any())).thenAnswer(call -> call.getArgument(0));

    handler.handle(childCommand(container.getId(), null, null));

    var saved = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(saved.capture());
    assertThat(saved.getValue().getRegistrationMode()).isEqualTo(RegistrationMode.INHERIT);
  }

  @Test
  void aSubEventOutsideTheContainersNodeIsRejected() {
    organizationAndSpeakerExist();
    var container = conference();
    when(eventRepository.findById(container.getId())).thenReturn(Optional.of(container));
    when(findOrganizationSubtreeQueryHandler.handle(
            new FindOrganizationSubtreeQuery(organizationId)))
        .thenReturn(new FindOrganizationSubtreeResult(List.of(PartyId.generate())));

    assertThatThrownBy(() -> handler.handle(childCommand(container.getId(), null, null)))
        .isInstanceOf(DomainValidationException.class);
    verify(eventRepository, never()).save(any());
  }

  @Test
  void aSubEventCannotRepeat() {
    when(findOrganizationByIdQueryHandler.handle(new FindOrganizationByIdQuery(organizationId)))
        .thenReturn(Optional.of(FindOrganizationResult.from(church)));
    when(findPeopleByIdsQueryHandler.handle(new FindPeopleByIdsQuery(List.of(speakerId))))
        .thenReturn(List.of(personResult(speakerId, "Jose", "Mena")));
    var daily =
        Recurrence.of(Recurrence.Frequency.DAILY, 1, null, null, Recurrence.EndType.COUNT, null, 3);

    assertThatThrownBy(() -> handler.handle(childCommand(EventId.generate(), daily, null)))
        .isInstanceOf(DomainValidationException.class);
    verify(eventRepository, never()).save(any());
  }

  @Test
  void aMissingContainerIsNotFound() {
    organizationAndSpeakerExist();
    var missing = EventId.generate();
    when(eventRepository.findById(missing)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(childCommand(missing, null, null)))
        .isInstanceOf(NoSuchElementException.class);
  }
}
