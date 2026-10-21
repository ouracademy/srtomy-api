package org.ouracademy.srtomy.event.command.create_event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.organization.OrganizationFixtures.organization;
import static org.ouracademy.srtomy.person.PersonFixtures.person;
import static org.ouracademy.srtomy.person.PersonFixtures.personResult;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
import org.ouracademy.srtomy.event.domain.Event;
import org.ouracademy.srtomy.event.domain.RecurrentEvent;
import org.ouracademy.srtomy.event.domain.SingleEvent;
import org.ouracademy.srtomy.event.repository.EventRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.ouracademy.srtomy.organization.query.find_organization_by_id.FindOrganizationByIdQuery;
import org.ouracademy.srtomy.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.domain.Person;
import org.ouracademy.srtomy.person.query.find_people_by_ids.FindPeopleByIdsQuery;
import org.ouracademy.srtomy.person.query.find_people_by_ids.FindPeopleByIdsQueryHandler;

@ExtendWith(MockitoExtension.class)
class CreateEventCommandHandlerImplTests {

  static final OffsetDateTime START = OffsetDateTime.parse("2026-01-05T10:00:00-05:00");
  static final OffsetDateTime END = OffsetDateTime.parse("2026-01-05T12:00:00-05:00");

  @Mock EventRepository eventRepository;
  @Mock FindOrganizationByIdQueryHandler findOrganizationByIdQueryHandler;
  @Mock FindPeopleByIdsQueryHandler findPeopleByIdsQueryHandler;
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
            findOrganizationByIdQueryHandler,
            findPeopleByIdsQueryHandler,
            entityManager);
  }

  private CreateEventCommand command(
      List<PartyId> speakerIds, List<Integer> daysOfWeek, LocalDate endRecur) {
    return new CreateEventCommand(
        "Servicio",
        START,
        END,
        "Av. 1",
        "Descripcion",
        true,
        organizationId,
        speakerIds,
        daysOfWeek,
        endRecur,
        endRecur != null);
  }

  private void organizationAndSpeakerExist() {
    when(findOrganizationByIdQueryHandler.handle(new FindOrganizationByIdQuery(organizationId)))
        .thenReturn(Optional.of(FindOrganizationResult.from(church)));
    when(findPeopleByIdsQueryHandler.handle(new FindPeopleByIdsQuery(List.of(speakerId))))
        .thenReturn(List.of(personResult(speakerId, "Jose", "Mena")));
    when(entityManager.getReference(Organization.class, organizationId)).thenReturn(church);
    when(entityManager.getReference(Person.class, speakerId)).thenReturn(speaker);
    when(eventRepository.save(any(Event.class))).thenAnswer(call -> call.getArgument(0));
  }

  @Test
  void handleSavesASingleEventInUtcReferencingTheOrganizationAndSpeakers() {
    organizationAndSpeakerExist();

    var result = handler.handle(command(List.of(speakerId), List.of(), null));

    var saved = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(saved.capture());
    assertThat(saved.getValue()).isInstanceOf(SingleEvent.class);
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
  }

  @Test
  @SuppressWarnings("unchecked")
  void handleSavesARecurrentEventAndItsExpandedSingleEvents() {
    organizationAndSpeakerExist();

    var result =
        handler.handle(command(List.of(speakerId), List.of(1, 3), LocalDate.of(2026, 1, 14)));

    var saved = ArgumentCaptor.forClass(Event.class);
    verify(eventRepository).save(saved.capture());
    var recurrent = (RecurrentEvent) saved.getValue();
    assertThat(recurrent.getId()).isEqualTo(result.id());
    assertThat(recurrent.getDaysOfWeek()).containsExactly(1, 3);
    assertThat(recurrent.getEndRecur()).isEqualTo(LocalDate.of(2026, 1, 14));

    ArgumentCaptor<Iterable<SingleEvent>> expanded = ArgumentCaptor.forClass(Iterable.class);
    verify(eventRepository).saveAll(expanded.capture());
    var occurrences = (List<SingleEvent>) expanded.getValue();
    // Mondays and Wednesdays from Jan 5 to Jan 14, 2026.
    assertThat(occurrences)
        .extracting(SingleEvent::getStart)
        .containsExactly(
            ZonedDateTime.parse("2026-01-05T15:00:00Z[UTC]"),
            ZonedDateTime.parse("2026-01-07T15:00:00Z[UTC]"),
            ZonedDateTime.parse("2026-01-12T15:00:00Z[UTC]"),
            ZonedDateTime.parse("2026-01-14T15:00:00Z[UTC]"));
    assertThat(occurrences)
        .allSatisfy(
            occurrence -> {
              assertThat(occurrence.getRecurrentEvent()).isSameAs(recurrent);
              assertThat(occurrence.getOrganization()).isSameAs(church);
              assertThat(occurrence.getSpeakers()).containsExactly(speaker);
            });
  }

  @Test
  void handleFailsWhenTheOrganizationDoesNotExist() {
    when(findOrganizationByIdQueryHandler.handle(new FindOrganizationByIdQuery(organizationId)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(command(List.of(speakerId), List.of(), null)))
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
            () -> handler.handle(command(List.of(speakerId, missingSpeakerId), List.of(), null)))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining(missingSpeakerId.value().toString())
        .hasMessageNotContaining(speakerId.value().toString());

    verifyNoInteractions(eventRepository, entityManager);
  }
}
