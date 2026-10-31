package org.ouracademy.ovejafiel.event.command.edit_event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.ouracademy.ovejafiel.event.EventFixtures.childOf;
import static org.ouracademy.ovejafiel.event.EventFixtures.eventBetween;
import static org.ouracademy.ovejafiel.event.EventFixtures.eventSeries;
import static org.ouracademy.ovejafiel.event.EventFixtures.registration;
import static org.ouracademy.ovejafiel.event.EventFixtures.ticket;
import static org.ouracademy.ovejafiel.organization.OrganizationFixtures.organization;
import static org.ouracademy.ovejafiel.person.PersonFixtures.person;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ouracademy.ovejafiel.attendance.command.delete_attendance_for_events.DeleteAttendanceForEventsCommand;
import org.ouracademy.ovejafiel.attendance.command.delete_attendance_for_events.DeleteAttendanceForEventsCommandHandler;
import org.ouracademy.ovejafiel.attendance.query.find_event_ids_with_attendance.FindEventIdsWithAttendanceQueryHandler;
import org.ouracademy.ovejafiel.attendance.query.find_event_ids_with_attendance.FindEventIdsWithAttendanceResult;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventChangeOption;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventSeries;
import org.ouracademy.ovejafiel.event.domain.Recurrence;
import org.ouracademy.ovejafiel.event.domain.RegistrationMode;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.query.EffectiveRegistration;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.event.repository.EventSeriesRepository;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailCommand;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailCommandHandler;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.query.find_people_by_ids.FindPeopleByIdsQueryHandler;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EditEventCommandHandlerImplTests {

  static final ZoneId UTC = ZoneId.of("UTC");
  // The first occurrence (Mon 5 Jan) is past; Wed 7, Mon 12 and Wed 14 Jan are to come.
  static final Instant NOW = Instant.parse("2026-01-06T00:00:00Z");

  @Mock EventRepository eventRepository;
  @Mock EventSeriesRepository eventSeriesRepository;
  @Mock EventRegistrationRepository eventRegistrationRepository;
  @Mock FindPeopleByIdsQueryHandler findPeopleByIdsQueryHandler;
  @Mock FindEventIdsWithAttendanceQueryHandler findEventIdsWithAttendanceQueryHandler;
  @Mock DeleteAttendanceForEventsCommandHandler deleteAttendanceForEventsCommandHandler;
  @Mock SendEmailCommandHandler sendEmailCommandHandler;
  @Mock jakarta.persistence.EntityManager entityManager;

  EditEventCommandHandlerImpl handler;
  Organization church;
  EventSeries series;
  List<Event> occurrences;

  @BeforeEach
  void setUp() {
    handler =
        new EditEventCommandHandlerImpl(
            eventRepository,
            eventSeriesRepository,
            eventRegistrationRepository,
            new EffectiveRegistration(),
            findPeopleByIdsQueryHandler,
            findEventIdsWithAttendanceQueryHandler,
            deleteAttendanceForEventsCommandHandler,
            sendEmailCommandHandler,
            entityManager);
    lenient()
        .when(eventSeriesRepository.save(any(EventSeries.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    church = organization(PartyId.generate(), "Iglesia", Organization.Type.CHURCH);
    series = eventSeries(church);
    occurrences =
        List.of(
            occurrence("2026-01-05T15:00:00Z"),
            occurrence("2026-01-07T15:00:00Z"),
            occurrence("2026-01-12T15:00:00Z"),
            occurrence("2026-01-14T15:00:00Z"));
  }

  private Event occurrence(String start) {
    var from = ZonedDateTime.parse(start).withZoneSameInstant(UTC);
    var event = eventBetween(EventId.generate(), "Servicio", church, from, from.plusHours(2));
    ReflectionTestUtils.setField(event, "series", series);
    return event;
  }

  private Event oneOff() {
    var from = ZonedDateTime.parse("2026-02-02T15:00:00Z");
    return eventBetween(EventId.generate(), "Retiro", church, from, from.plusHours(2));
  }

  private void given(Event event) {
    when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
  }

  private void givenTheSeriesEvents() {
    when(eventRepository.findBySeries(series)).thenReturn(occurrences);
  }

  /** A command for {@code event} with its current data; the test changes what it wants. */
  private EditEventCommand command(
      Event event,
      EventChangeOption option,
      String name,
      String start,
      String end,
      Recurrence recurrence) {
    return new EditEventCommand(
        event.getId(),
        option,
        name,
        OffsetDateTime.parse(start),
        OffsetDateTime.parse(end),
        "Nueva dirección",
        "Nueva descripción",
        false,
        church.getId(),
        List.of(),
        recurrence,
        null,
        null,
        null,
        NOW);
  }

  private EditEventCommand sameTimes(Event event, EventChangeOption option, String name) {
    return command(
        event,
        option,
        name,
        event.getStart().toOffsetDateTime().toString(),
        event.getEnd().toOffsetDateTime().toString(),
        null);
  }

  private final java.util.List<org.ouracademy.ovejafiel.event.domain.EventRegistration>
      registrations = new java.util.ArrayList<>();

  /** Who signed up; the repository answers only for the events it is asked about. */
  private void givenSignedUp(Event event, String email) {
    registrations.add(
        registration(event, ticket(event, "General", null), email, RegistrationStatus.CONFIRMED));
    lenient()
        .when(
            eventRegistrationRepository.findByEventIdInAndStatusNot(
                anyCollection(), org.mockito.ArgumentMatchers.eq(RegistrationStatus.CANCELLED)))
        .thenAnswer(
            invocation -> {
              java.util.Collection<EventId> ids = invocation.getArgument(0);
              return registrations.stream()
                  .filter(r -> ids.contains(r.getEvent().getId()))
                  .toList();
            });
  }

  // ---- validation

  @Test
  void aMissingEventThrows() {
    var event = oneOff();
    when(eventRepository.findById(event.getId())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(sameTimes(event, EventChangeOption.THIS_EVENT, "x")))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void theNodeOfAnEventCannotChange() {
    var event = oneOff();
    given(event);
    var other =
        command(
            event,
            EventChangeOption.THIS_EVENT,
            "x",
            "2026-02-02T15:00:00Z",
            "2026-02-02T17:00:00Z",
            null);
    var moved =
        new EditEventCommand(
            other.id(),
            other.option(),
            other.name(),
            other.startTime(),
            other.endTime(),
            null,
            null,
            false,
            PartyId.generate(),
            List.of(),
            null,
            null,
            null,
            null,
            NOW);

    assertThatThrownBy(() -> handler.handle(moved))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("iglesia, sede o grupo");
    assertThat(event.getTitle()).isEqualTo("Retiro");
  }

  @Test
  void anEventCannotBecomeACourse() {
    var event = oneOff();
    given(event);
    var c = sameTimes(event, EventChangeOption.THIS_EVENT, "x");
    var course =
        new EditEventCommand(
            c.id(),
            c.option(),
            c.name(),
            c.startTime(),
            c.endTime(),
            null,
            null,
            true,
            c.organizationId(),
            List.of(),
            null,
            null,
            null,
            null,
            NOW);

    assertThatThrownBy(() -> handler.handle(course))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("curso");
  }

  @Test
  void theEndCannotBeBeforeTheStart() {
    var event = oneOff();
    given(event);

    assertThatThrownBy(
            () ->
                handler.handle(
                    command(
                        event,
                        EventChangeOption.THIS_EVENT,
                        "x",
                        "2026-02-02T15:00:00Z",
                        "2026-02-02T14:00:00Z",
                        null)))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void aSpeakerThatDoesNotExistThrows() {
    var event = oneOff();
    given(event);
    var c = sameTimes(event, EventChangeOption.THIS_EVENT, "x");
    var withSpeaker =
        new EditEventCommand(
            c.id(),
            c.option(),
            c.name(),
            c.startTime(),
            c.endTime(),
            null,
            null,
            false,
            c.organizationId(),
            List.of(PartyId.generate()),
            null,
            null,
            null,
            null,
            NOW);
    when(findPeopleByIdsQueryHandler.handle(any())).thenReturn(List.of());

    assertThatThrownBy(() -> handler.handle(withSpeaker))
        .isInstanceOf(NoSuchElementException.class);
  }

  // ---- one-off event

  @Test
  void editingTheDataOfAOneOffEventWritesItAndAnEmptyDescriptionEmailsNobody() {
    var event = oneOff();
    given(event);
    givenSignedUp(event, "lu@example.com");
    var same = sameTimes(event, EventChangeOption.THIS_EVENT, event.getTitle());
    var onlyDescription =
        new EditEventCommand(
            same.id(),
            same.option(),
            same.name(),
            same.startTime(),
            same.endTime(),
            event.getAddress(),
            "Otra descripción",
            false,
            same.organizationId(),
            List.of(),
            null,
            null,
            null,
            null,
            NOW);

    var result = handler.handle(onlyDescription);

    assertThat(event.getDescription()).isEqualTo("Otra descripción");
    assertThat(result).isEqualTo(new EditEventResult(event.getId(), 0, 0));
    verifyNoInteractions(sendEmailCommandHandler);
  }

  @Test
  void aNewNameOrPlaceEmailsWhoSignedUp() {
    var event = oneOff();
    given(event);
    givenSignedUp(event, "lu@example.com");

    var result =
        handler.handle(sameTimes(event, EventChangeOption.THIS_EVENT, "Retiro de jóvenes"));

    assertThat(event.getTitle()).isEqualTo("Retiro de jóvenes");
    assertThat(event.getAddress()).isEqualTo("Nueva dirección");
    assertThat(result.notifiedPeople()).isEqualTo(1);
    var email = ArgumentCaptor.forClass(SendEmailCommand.class);
    verify(sendEmailCommandHandler).handle(email.capture());
    assertThat(email.getValue().text())
        .contains("Nuevo nombre: «Retiro de jóvenes»", "Nuevo lugar: Nueva dirección")
        .doesNotContain("Cambió la fecha u hora");
  }

  @Test
  void newSpeakersEmailWhoSignedUpWithTheirNames() {
    var event = oneOff();
    given(event);
    givenSignedUp(event, "lu@example.com");
    var speakerId = PartyId.generate();
    when(findPeopleByIdsQueryHandler.handle(any()))
        .thenReturn(
            List.of(
                org.ouracademy.ovejafiel.person.query.FindPersonResult.from(
                    person(speakerId, "Jose", "Mena"))));
    var same = sameTimes(event, EventChangeOption.THIS_EVENT, event.getTitle());
    var withSpeaker =
        new EditEventCommand(
            same.id(),
            same.option(),
            same.name(),
            same.startTime(),
            same.endTime(),
            event.getAddress(),
            event.getDescription(),
            false,
            same.organizationId(),
            List.of(speakerId),
            null,
            null,
            null,
            null,
            NOW);

    handler.handle(withSpeaker);

    var email = ArgumentCaptor.forClass(SendEmailCommand.class);
    verify(sendEmailCommandHandler).handle(email.capture());
    assertThat(email.getValue().text()).contains("Ponentes: Jose Mena");
  }

  @Test
  void nobodyIsEmailedWhenNothingThatMattersChanged() {
    var event = oneOff();
    given(event);
    givenSignedUp(event, "lu@example.com");
    var same = sameTimes(event, EventChangeOption.THIS_EVENT, event.getTitle());
    var untouched =
        new EditEventCommand(
            same.id(),
            same.option(),
            same.name(),
            same.startTime(),
            same.endTime(),
            event.getAddress(),
            event.getDescription(),
            false,
            same.organizationId(),
            List.of(),
            null,
            null,
            null,
            null,
            NOW);

    assertThat(handler.handle(untouched).notifiedPeople()).isZero();
    verifyNoInteractions(sendEmailCommandHandler);
  }

  @Test
  void movingAOneOffEventEmailsWhoSignedUpOnce() {
    var event = oneOff();
    given(event);
    givenSignedUp(event, "Lu@Example.com");

    var result =
        handler.handle(
            command(
                event,
                EventChangeOption.THIS_EVENT,
                "Retiro",
                "2026-02-03T15:00:00Z",
                "2026-02-03T17:00:00Z",
                null));

    assertThat(event.getStart()).isEqualTo(ZonedDateTime.parse("2026-02-03T15:00:00Z[UTC]"));
    assertThat(result.notifiedPeople()).isEqualTo(1);
    var email = ArgumentCaptor.forClass(SendEmailCommand.class);
    verify(sendEmailCommandHandler).handle(email.capture());
    assertThat(email.getValue().to()).isEqualTo("lu@example.com");
    assertThat(email.getValue().type()).isEqualTo("EVENT_CHANGED");
    assertThat(email.getValue().subject()).isEqualTo("Cambios en «Retiro»");
    assertThat(email.getValue().text())
        .contains(
            "Hola Lu",
            "Cambió la fecha u hora",
            "martes 3 de febrero de 2026, 15:00",
            event.getShareSlug())
        .doesNotContain("Nuevo nombre");
  }

  @Test
  void aOneOffEventWithARecurrenceBecomesTheFirstOccurrenceOfANewSeries() {
    var event = oneOff();
    given(event);
    var weekly =
        Recurrence.of(
            Recurrence.Frequency.WEEKLY, 1, null, null, Recurrence.EndType.COUNT, null, 3);

    handler.handle(
        command(
            event,
            EventChangeOption.THIS_EVENT,
            "Retiro",
            "2026-02-02T15:00:00Z",
            "2026-02-02T17:00:00Z",
            weekly));

    assertThat(event.getSeries()).isNotNull();
    assertThat(event.getSeries().getRecurrence()).isEqualTo(weekly);
    @SuppressWarnings("unchecked")
    var created =
        (ArgumentCaptor<List<Event>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
    verify(eventRepository).saveAll(created.capture());
    assertThat(created.getValue())
        .extracting(Event::getStart)
        .containsExactly(
            ZonedDateTime.parse("2026-02-09T15:00:00Z[UTC]"),
            ZonedDateTime.parse("2026-02-16T15:00:00Z[UTC]"));
  }

  // ---- series: THIS_EVENT

  @Test
  void thisEventEditsOneOccurrenceAndTakesItOutOfTheSeries() {
    var target = occurrences.get(1);
    given(target);

    handler.handle(sameTimes(target, EventChangeOption.THIS_EVENT, "Servicio especial"));

    assertThat(target.getTitle()).isEqualTo("Servicio especial");
    assertThat(target.getSeries()).isNull();
    assertThat(occurrences.get(2).getTitle()).isEqualTo("Servicio");
    assertThat(occurrences.get(2).getSeries()).isSameAs(series);
    assertThat(series.getTitle()).isEqualTo("Servicio");
  }

  // ---- series: in place

  @Test
  void allEventsInPlaceChangesTheTimeOfTheFutureOnesAndKeepsThePastOnes() {
    var target = occurrences.get(2);
    given(target);
    givenTheSeriesEvents();
    givenSignedUp(occurrences.get(3), "ana@example.com");

    var result =
        handler.handle(
            command(
                target,
                EventChangeOption.ALL_EVENTS,
                "Culto",
                "2026-01-12T16:00:00Z",
                "2026-01-12T17:30:00Z",
                null));

    assertThat(occurrences).extracting(Event::getTitle).containsOnly("Culto");
    assertThat(occurrences.get(0).getStart())
        .isEqualTo(ZonedDateTime.parse("2026-01-05T15:00:00Z[UTC]"));
    assertThat(occurrences.get(1).getStart())
        .isEqualTo(ZonedDateTime.parse("2026-01-07T16:00:00Z[UTC]"));
    assertThat(occurrences.get(1).duration()).isEqualTo(Duration.ofMinutes(90));
    assertThat(occurrences.get(3).getStart())
        .isEqualTo(ZonedDateTime.parse("2026-01-14T16:00:00Z[UTC]"));
    assertThat(occurrences).allSatisfy(e -> assertThat(e.getSeries()).isSameAs(series));
    assertThat(series.getTitle()).isEqualTo("Culto");
    assertThat(series.getStartLocal().toLocalTime()).isEqualTo(java.time.LocalTime.of(16, 0));
    assertThat(series.duration()).isEqualTo(Duration.ofMinutes(90));
    assertThat(result.notifiedPeople()).isEqualTo(1);
    assertThat(result.keptEvents()).isZero();
    verify(deleteAttendanceForEventsCommandHandler, never()).handle(any());
  }

  @Test
  void thisAndTheFollowingInPlaceSplitsTheSeriesInTwo() {
    var target = occurrences.get(2);
    given(target);
    var following = List.of(occurrences.get(2), occurrences.get(3));
    when(eventRepository.findBySeriesAndStartGreaterThanEqual(series, target.getStart()))
        .thenReturn(following);

    handler.handle(sameTimes(target, EventChangeOption.THIS_AND_THE_FOLLOWING_EVENTS, "Culto"));

    assertThat(occurrences.get(0).getTitle()).isEqualTo("Servicio");
    assertThat(occurrences.get(1).getSeries()).isSameAs(series);
    assertThat(series.getRecurrence().getUntil()).isEqualTo(LocalDate.of(2026, 1, 11));
    var next = target.getSeries();
    assertThat(next).isNotSameAs(series).isNotNull();
    assertThat(occurrences.get(3).getSeries()).isSameAs(next);
    assertThat(next.getTitle()).isEqualTo("Culto");
    assertThat(next.getStartLocal().toLocalDate()).isEqualTo(LocalDate.of(2026, 1, 12));
    assertThat(occurrences.get(3).getTitle()).isEqualTo("Culto");
    verify(eventSeriesRepository).save(next);
  }

  // ---- series: regenerate

  @Test
  void changingTheRuleKeepsWhatIsPastOrHasPeopleAndReplacesTheRest() {
    var target = occurrences.get(1);
    given(target);
    givenTheSeriesEvents();
    var signedUp = occurrences.get(2);
    givenSignedUp(signedUp, "ana@example.com");
    when(findEventIdsWithAttendanceQueryHandler.handle(any()))
        .thenReturn(new FindEventIdsWithAttendanceResult(Set.of()));
    var daily =
        Recurrence.of(Recurrence.Frequency.DAILY, 1, null, null, Recurrence.EndType.COUNT, null, 5);

    var result =
        handler.handle(
            command(
                target,
                EventChangeOption.ALL_EVENTS,
                "Diario",
                "2026-01-07T15:00:00Z",
                "2026-01-07T17:00:00Z",
                daily));

    // Past (5 Jan) and with a sign-up (12 Jan): untouched and out of the series.
    assertThat(result.keptEvents()).isEqualTo(2);
    assertThat(occurrences.get(0).getTitle()).isEqualTo("Servicio");
    assertThat(occurrences.get(0).getSeries()).isNull();
    assertThat(signedUp.getTitle()).isEqualTo("Servicio");
    assertThat(signedUp.getSeries()).isNull();
    assertThat(signedUp.getDeletedTime()).isNull();
    // The one to come with nobody is replaced; the edited one is the first of the new series.
    assertThat(occurrences.get(3).getDeletedTime()).isNotNull();
    verify(deleteAttendanceForEventsCommandHandler)
        .handle(new DeleteAttendanceForEventsCommand(List.of(occurrences.get(3).getId())));
    assertThat(series.getDeletedTime()).isNotNull();
    var next = target.getSeries();
    assertThat(next).isNotSameAs(series);
    assertThat(next.getRecurrence()).isEqualTo(daily);
    assertThat(target.getTitle()).isEqualTo("Diario");
    @SuppressWarnings("unchecked")
    var created =
        (ArgumentCaptor<List<Event>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
    verify(eventRepository).saveAll(created.capture());
    // 5 daily from the first day of the series (5 Jan): 5th is past and kept, 7th is the edited
    // one, so the 6th, 8th and 9th are created.
    assertThat(created.getValue())
        .extracting(e -> e.getStart().toLocalDate())
        .containsExactly(
            LocalDate.of(2026, 1, 6), LocalDate.of(2026, 1, 8), LocalDate.of(2026, 1, 9));
    assertThat(created.getValue()).allSatisfy(e -> assertThat(e.getSeries()).isSameAs(next));
    // Nobody signed up for the edited occurrence, and the ones with people were not touched.
    verifyNoInteractions(sendEmailCommandHandler);
  }

  @Test
  void movingTheDayOfAllEventsShiftsTheSeriesByTheSameDays() {
    var target = occurrences.get(1); // Wed 7 Jan
    given(target);
    givenTheSeriesEvents();
    when(findEventIdsWithAttendanceQueryHandler.handle(any()))
        .thenReturn(new FindEventIdsWithAttendanceResult(Set.of()));

    handler.handle(
        command(
            target,
            EventChangeOption.ALL_EVENTS,
            "Servicio",
            "2026-01-08T15:00:00Z",
            "2026-01-08T17:00:00Z",
            null));

    var next = target.getSeries();
    assertThat(next.getStartLocal().toLocalDate()).isEqualTo(LocalDate.of(2026, 1, 6));
    assertThat(target.getStart().toLocalDate()).isEqualTo(LocalDate.of(2026, 1, 8));
  }

  @Test
  void aRuleThatMakesNoOccurrenceIsRejected() {
    var target = occurrences.get(1);
    given(target);
    givenTheSeriesEvents();
    when(findEventIdsWithAttendanceQueryHandler.handle(any()))
        .thenReturn(new FindEventIdsWithAttendanceResult(Set.of()));
    var never =
        Recurrence.of(
            Recurrence.Frequency.MONTHLY,
            1,
            null,
            Recurrence.MonthlyMode.DAY_OF_MONTH,
            Recurrence.EndType.UNTIL,
            LocalDate.of(2025, 1, 1),
            null);

    assertThatThrownBy(
            () ->
                handler.handle(
                    command(
                        target,
                        EventChangeOption.ALL_EVENTS,
                        "x",
                        "2026-01-07T15:00:00Z",
                        "2026-01-07T17:00:00Z",
                        never)))
        .isInstanceOf(DomainValidationException.class);
  }

  // ---- composite events (spec 012)

  private Event megaEvent() {
    var from = ZonedDateTime.parse("2026-03-01T00:00:00Z");
    return eventBetween(
        EventId.generate(),
        "Mega evento",
        church,
        from,
        ZonedDateTime.parse("2026-03-03T23:00:00Z"));
  }

  private Event subEventOf(Event mega, RegistrationMode mode) {
    var from = ZonedDateTime.parse("2026-03-02T10:00:00Z");
    var child = childOf(mega, EventId.generate(), "Taller", from, from.plusHours(2));
    child.setRegistrationMode(mode);
    return child;
  }

  private EditEventCommand within(
      Event event, String start, String end, EventId parentEventId, RegistrationMode mode) {
    var base = command(event, EventChangeOption.THIS_EVENT, event.getTitle(), start, end, null);
    return new EditEventCommand(
        base.id(),
        base.option(),
        base.name(),
        base.startTime(),
        base.endTime(),
        base.address(),
        base.description(),
        base.isACourse(),
        base.organizationId(),
        base.speakerIds(),
        null,
        null,
        parentEventId,
        mode,
        NOW);
  }

  @Test
  void aSubEventCanMoveInsideItsMegaEvent() {
    var mega = megaEvent();
    var child = subEventOf(mega, RegistrationMode.INHERIT);
    given(child);

    handler.handle(within(child, "2026-03-03T09:00:00Z", "2026-03-03T11:00:00Z", null, null));

    assertThat(child.getStart()).isEqualTo(ZonedDateTime.parse("2026-03-03T09:00:00Z[UTC]"));
  }

  @Test
  void aSubEventCannotLeaveItsMegaEvent() {
    var mega = megaEvent();
    var child = subEventOf(mega, RegistrationMode.INHERIT);
    given(child);

    assertThatThrownBy(
            () ->
                handler.handle(
                    within(child, "2026-03-04T09:00:00Z", "2026-03-04T11:00:00Z", null, null)))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("«Taller»");
  }

  @Test
  void aMegaEventCanGrowButNotLeaveASubEventOut() {
    var mega = megaEvent();
    var child = subEventOf(mega, RegistrationMode.INHERIT);
    given(mega);
    when(eventRepository.findByParentOrderByStart(mega)).thenReturn(List.of(child));

    handler.handle(within(mega, "2026-02-28T00:00:00Z", "2026-03-05T00:00:00Z", null, null));
    assertThat(mega.getEnd()).isEqualTo(ZonedDateTime.parse("2026-03-05T00:00:00Z[UTC]"));

    assertThatThrownBy(
            () ->
                handler.handle(
                    within(mega, "2026-03-03T00:00:00Z", "2026-03-04T00:00:00Z", null, null)))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("«Taller»");
  }

  @Test
  void aMegaEventOrASubEventCannotStartRepeating() {
    var mega = megaEvent();
    var child = subEventOf(mega, RegistrationMode.INHERIT);
    given(mega);
    given(child);
    when(eventRepository.existsByParent(mega)).thenReturn(true);
    var daily =
        Recurrence.of(Recurrence.Frequency.DAILY, 1, null, null, Recurrence.EndType.COUNT, null, 3);

    for (var event : List.of(mega, child)) {
      var plain = within(event, "2026-03-02T10:00:00Z", "2026-03-02T12:00:00Z", null, null);
      var repeating =
          new EditEventCommand(
              plain.id(),
              plain.option(),
              plain.name(),
              plain.startTime(),
              plain.endTime(),
              plain.address(),
              plain.description(),
              plain.isACourse(),
              plain.organizationId(),
              plain.speakerIds(),
              daily,
              null,
              null,
              null,
              NOW);

      assertThatThrownBy(() -> handler.handle(repeating))
          .isInstanceOf(DomainValidationException.class)
          .hasMessageContaining("no puede repetirse");
    }
    verify(eventSeriesRepository, never()).save(any(EventSeries.class));
  }

  @Test
  void aSubEventCannotBeMovedToAnotherEvent() {
    var mega = megaEvent();
    var child = subEventOf(mega, RegistrationMode.INHERIT);
    given(child);

    assertThatThrownBy(
            () ->
                handler.handle(
                    within(
                        child,
                        "2026-03-02T10:00:00Z",
                        "2026-03-02T12:00:00Z",
                        EventId.generate(),
                        null)))
        .isInstanceOf(DomainValidationException.class);
    // Naming its own mega event, or none, is "as it is".
    handler.handle(
        within(child, "2026-03-02T10:00:00Z", "2026-03-02T12:00:00Z", mega.getId(), null));
    handler.handle(within(child, "2026-03-02T10:00:00Z", "2026-03-02T12:00:00Z", null, null));
  }

  @Test
  void theRegistrationModeOfASubEventChangesButAStandaloneEventIgnoresIt() {
    var mega = megaEvent();
    var child = subEventOf(mega, RegistrationMode.INHERIT);
    var standalone = oneOff();
    given(child);
    given(standalone);

    handler.handle(
        within(child, "2026-03-02T10:00:00Z", "2026-03-02T12:00:00Z", null, RegistrationMode.OWN));
    handler.handle(
        within(
            standalone,
            "2026-02-02T15:00:00Z",
            "2026-02-02T17:00:00Z",
            null,
            RegistrationMode.OPEN));

    assertThat(child.getRegistrationMode()).isEqualTo(RegistrationMode.OWN);
    assertThat(standalone.getRegistrationMode()).isEqualTo(RegistrationMode.INHERIT);
  }

  @Test
  void movingASubEventThatInheritsEmailsThePeopleOfTheMegaEvent() {
    var mega = megaEvent();
    var child = subEventOf(mega, RegistrationMode.INHERIT);
    given(child);
    givenSignedUp(mega, "Lu@Example.com");

    var result =
        handler.handle(within(child, "2026-03-03T09:00:00Z", "2026-03-03T11:00:00Z", null, null));

    assertThat(result.notifiedPeople()).isEqualTo(1);
    var ids = ArgumentCaptor.forClass(java.util.Collection.class);
    verify(eventRegistrationRepository)
        .findByEventIdInAndStatusNot(ids.capture(), any(RegistrationStatus.class));
    assertThat(ids.getValue()).containsExactly(mega.getId());
    var email = ArgumentCaptor.forClass(SendEmailCommand.class);
    verify(sendEmailCommandHandler).handle(email.capture());
    assertThat(email.getValue().to()).isEqualTo("lu@example.com");
    assertThat(email.getValue().subject()).isEqualTo("Cambios en «Taller»");
  }

  @Test
  void movingAnOpenSubEventEmailsNobody() {
    var mega = megaEvent();
    var child = subEventOf(mega, RegistrationMode.OPEN);
    given(child);

    var result =
        handler.handle(within(child, "2026-03-03T09:00:00Z", "2026-03-03T11:00:00Z", null, null));

    assertThat(result.notifiedPeople()).isZero();
    verify(sendEmailCommandHandler, never()).handle(any());
  }
}
