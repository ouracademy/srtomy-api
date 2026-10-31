package org.ouracademy.ovejafiel.event.command.edit_event;

import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.attendance.command.delete_attendance_for_events.DeleteAttendanceForEventsCommand;
import org.ouracademy.ovejafiel.attendance.command.delete_attendance_for_events.DeleteAttendanceForEventsCommandHandler;
import org.ouracademy.ovejafiel.attendance.query.find_event_ids_with_attendance.FindEventIdsWithAttendanceQuery;
import org.ouracademy.ovejafiel.attendance.query.find_event_ids_with_attendance.FindEventIdsWithAttendanceQueryHandler;
import org.ouracademy.ovejafiel.event.domain.Event;
import org.ouracademy.ovejafiel.event.domain.EventChangeOption;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventSeries;
import org.ouracademy.ovejafiel.event.domain.Recurrence;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.query.EffectiveRegistration;
import org.ouracademy.ovejafiel.event.repository.EventRegistrationRepository;
import org.ouracademy.ovejafiel.event.repository.EventRepository;
import org.ouracademy.ovejafiel.event.repository.EventSeriesRepository;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailCommand;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailCommandHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.person.query.FindPersonResult;
import org.ouracademy.ovejafiel.person.query.find_people_by_ids.FindPeopleByIdsQuery;
import org.ouracademy.ovejafiel.person.query.find_people_by_ids.FindPeopleByIdsQueryHandler;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The rules are in {@code specs/013-edit-events/spec.md}. In short: a one-off event is edited in
 * place; in a series, {@code THIS_EVENT} edits one occurrence and takes it out of the series, and
 * {@code THIS_AND_THE_FOLLOWING_EVENTS}/{@code ALL_EVENTS} either edit the occurrences in place
 * (nothing about the days changed) or regenerate them (the rule, the zone or the day changed),
 * keeping the occurrences that are past or have sign-ups or attendance. Whoever signed up for an
 * occurrence whose time changed gets an email, through the outbox in this same transaction.
 */
@Service
@RequiredArgsConstructor
class EditEventCommandHandlerImpl implements EditEventCommandHandler {

  private static final ZoneId UTC = ZoneId.of("UTC");
  private static final DateTimeFormatter WHEN =
      DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es"));

  private final EventRepository eventRepository;
  private final EventSeriesRepository eventSeriesRepository;
  private final EventRegistrationRepository eventRegistrationRepository;
  private final EffectiveRegistration effectiveRegistration;
  private final FindPeopleByIdsQueryHandler findPeopleByIdsQueryHandler;
  private final FindEventIdsWithAttendanceQueryHandler findEventIdsWithAttendanceQueryHandler;
  private final DeleteAttendanceForEventsCommandHandler deleteAttendanceForEventsCommandHandler;
  private final SendEmailCommandHandler sendEmailCommandHandler;
  private final EntityManager entityManager;

  @Value("${app.url:http://localhost:3000}")
  private String appUrl = "http://localhost:3000";

  /** What the request asks for, in the form the events store it. */
  private record Edit(
      EditEventCommand command,
      ZonedDateTime start,
      ZonedDateTime end,
      List<Person> speakers,
      List<String> speakerNames) {

    Duration duration() {
      return Duration.between(start, end);
    }
  }

  /** What changed in one occurrence that matters to whoever signed up for it. */
  private record Notice(Event event, boolean time, boolean name, boolean place, boolean speakers) {}

  /** What a run changed: how many occurrences it left alone and what changed in the others. */
  private static final class Outcome {
    int kept;
    final List<Notice> notices = new ArrayList<>();
  }

  @Override
  @Transactional
  public EditEventResult handle(EditEventCommand command) {
    var event =
        eventRepository
            .findById(command.id())
            .orElseThrow(() -> new NoSuchElementException("Event not found: " + command.id()));
    var edit = validated(command, event);
    var displayZone =
        command.timeZone() != null
            ? command.timeZone()
            : event.getSeries() != null ? event.getSeries().zone() : UTC;

    var outcome = new Outcome();
    var series = event.getSeries();
    if (series == null) {
      editOneOff(event, edit, outcome);
    } else if (command.option() == EventChangeOption.THIS_EVENT) {
      apply(event, edit, edit.start(), edit.end(), outcome);
      event.leaveSeries();
    } else {
      editSeries(event, series, edit, outcome);
    }
    validateSchedule(event, edit);
    var notified = notifyRegistrations(outcome.notices, edit, displayZone);
    return new EditEventResult(event.getId(), outcome.kept, notified);
  }

  private Edit validated(EditEventCommand command, Event event) {
    if (!event.getOrganization().getId().equals(command.organizationId())) {
      throw new DomainValidationException(
          "No se puede cambiar la iglesia, sede o grupo de un evento");
    }
    if (Boolean.TRUE.equals(command.isACourse()) != Boolean.TRUE.equals(event.getIsACourse())) {
      throw new DomainValidationException(
          "No se puede convertir un evento en curso ni un curso en evento");
    }
    requireSameContainer(command, event);
    var start = command.startTime().atZoneSameInstant(UTC);
    var end = command.endTime().atZoneSameInstant(UTC);
    if (end.isBefore(start)) {
      throw new DomainValidationException("La hora de fin no puede ser anterior a la de inicio");
    }
    var people = findPeople(command.speakerIds());
    return new Edit(
        command,
        start,
        end,
        command.speakerIds().stream()
            .map(id -> entityManager.getReference(Person.class, id))
            .toList(),
        people.stream().map(FindPersonResult::name).toList());
  }

  /**
   * A sub-event stays in its mega event (like the node, it cannot change), and neither can repeat.
   * A null container means "as it is", like a null recurrence.
   */
  private void requireSameContainer(EditEventCommand command, Event event) {
    var container = event.getParent() == null ? null : event.getParent().getId();
    if (command.parentEventId() != null && !command.parentEventId().equals(container)) {
      throw new DomainValidationException("No se puede mover un evento a otro evento ni sacarlo");
    }
    if (command.recurrence() != null
        && (event.isChild() || eventRepository.existsByParent(event))) {
      throw new DomainValidationException("Un evento compuesto no puede repetirse");
    }
  }

  /**
   * Composite events (spec 012): a sub-event stays inside the dates of its mega event, and a mega
   * event cannot be edited leaving a sub-event out. How a sub-event is entered is set here too.
   */
  private void validateSchedule(Event event, Edit edit) {
    if (event.isChild()) {
      event.requireInside(event.getParent());
      if (edit.command().registrationMode() != null) {
        event.setRegistrationMode(edit.command().registrationMode());
      }
    }
    eventRepository
        .findByParentOrderByStart(event)
        .forEach(subEvent -> subEvent.requireInside(event));
  }

  // ---- one-off event

  private void editOneOff(Event event, Edit edit, Outcome outcome) {
    apply(event, edit, edit.start(), edit.end(), outcome);
    var recurrence = edit.command().recurrence();
    if (recurrence == null) {
      return;
    }
    // The event becomes the first occurrence of a new series; the others are created.
    var zone = zoneOf(edit, UTC);
    var series =
        new EventSeries(
            edit.command().name(),
            edit.start().withZoneSameInstant(zone).toLocalDateTime(),
            edit.duration(),
            zone,
            recurrence);
    fillSeries(series, edit);
    series.setOrganization(event.getOrganization());
    series.setIsACourse(event.getIsACourse());
    series = eventSeriesRepository.save(series);
    var generated = series.materialize(series.horizonFrom(today(edit, zone)));
    if (generated.isEmpty()) {
      throw new DomainValidationException("La repetición no genera ningún evento");
    }
    event.joinSeries(series);
    eventRepository.saveAll(
        generated.stream().filter(g -> !sameInstant(g.getStart(), event.getStart())).toList());
  }

  // ---- series: THIS_AND_THE_FOLLOWING_EVENTS and ALL_EVENTS

  private void editSeries(Event event, EventSeries series, Edit edit, Outcome outcome) {
    var command = edit.command();
    var allEvents = command.option() == EventChangeOption.ALL_EVENTS;
    var oldZone = series.zone();
    var zone = zoneOf(edit, oldZone);
    var oldDay = event.getStart().withZoneSameInstant(oldZone).toLocalDate();
    var newLocal = edit.start().withZoneSameInstant(zone).toLocalDateTime();
    var keepsRule =
        command.recurrence() == null || command.recurrence().equals(series.getRecurrence());
    var regenerate = !keepsRule || !zone.equals(oldZone) || !newLocal.toLocalDate().equals(oldDay);

    var affected =
        allEvents
            ? eventRepository.findBySeries(series)
            : eventRepository.findBySeriesAndStartGreaterThanEqual(series, event.getStart());
    var others = affected.stream().filter(e -> !e.getId().equals(event.getId())).toList();
    var rule =
        keepsRule
            ? allEvents
                ? series.getRecurrence()
                : series.getRecurrence().remainingFrom(series.getStartLocal(), oldZone, oldDay)
            : command.recurrence();

    if (regenerate) {
      var anchor =
          allEvents
              ? series
                  .getStartLocal()
                  .toLocalDate()
                  .plusDays(ChronoUnit.DAYS.between(oldDay, newLocal.toLocalDate()))
                  .atTime(newLocal.toLocalTime())
              : newLocal;
      regenerate(event, series, others, anchor, zone, rule, oldDay, edit, outcome);
    } else {
      editInPlace(event, series, others, newLocal, rule, oldDay, edit, outcome);
    }
  }

  /** Nothing about the days changed: every occurrence keeps its id, sign-ups and attendance. */
  private void editInPlace(
      Event event,
      EventSeries series,
      List<Event> others,
      LocalDateTime newLocal,
      Recurrence rule,
      LocalDate oldDay,
      Edit edit,
      Outcome outcome) {
    var command = edit.command();
    var zone = series.zone();
    EventSeries target = series;
    if (command.option() == EventChangeOption.ALL_EVENTS) {
      series.rename(command.name());
      series.reschedule(newLocal.toLocalTime(), edit.duration());
      fillSeries(series, edit);
    } else {
      // The earlier occurrences stay in the series that ends the day before.
      target = series.successor(newLocal, edit.duration(), zone, rule);
      target.rename(command.name());
      fillSeries(target, edit);
      target = eventSeriesRepository.save(target);
      series.endOn(oldDay.minusDays(1));
    }
    apply(event, edit, edit.start(), edit.end(), outcome);
    event.joinSeries(target);
    for (var other : others) {
      var start = other.getStart();
      var end = other.getEnd();
      if (!other.hasStartedAt(command.now())) {
        start =
            start
                .withZoneSameInstant(zone)
                .toLocalDate()
                .atTime(newLocal.toLocalTime())
                .atZone(zone)
                .withZoneSameInstant(UTC);
        end = start.plus(edit.duration());
      }
      apply(other, edit, start, end, outcome);
      other.joinSeries(target);
    }
  }

  /**
   * The rule, the zone or the day changed. The occurrences to come with nobody on them are
   * replaced; the ones past or with sign-ups or attendance stay as they are, as one-off events.
   */
  private void regenerate(
      Event event,
      EventSeries series,
      List<Event> others,
      LocalDateTime anchor,
      ZoneId zone,
      Recurrence rule,
      LocalDate oldDay,
      Edit edit,
      Outcome outcome) {
    var command = edit.command();
    var withPeople = idsWithPeople(others);
    var kept =
        others.stream()
            .filter(e -> e.hasStartedAt(command.now()) || withPeople.contains(e.getId()))
            .toList();
    var replaced = others.stream().filter(e -> !kept.contains(e)).toList();

    var next = new EventSeries(command.name(), anchor, edit.duration(), zone, rule);
    fillSeries(next, edit);
    next.setOrganization(event.getOrganization());
    next.setIsACourse(event.getIsACourse());
    next = eventSeriesRepository.save(next);
    var generated = next.materialize(next.horizonFrom(today(edit, zone)));
    if (generated.isEmpty()) {
      throw new DomainValidationException("La repetición no genera ningún evento");
    }

    if (!replaced.isEmpty()) {
      deleteAttendanceForEventsCommandHandler.handle(
          new DeleteAttendanceForEventsCommand(replaced.stream().map(Event::getId).toList()));
      replaced.forEach(Event::delete);
    }
    kept.forEach(Event::leaveSeries);
    if (command.option() == EventChangeOption.ALL_EVENTS) {
      series.delete();
    } else {
      series.endOn(oldDay.minusDays(1));
    }

    apply(event, edit, edit.start(), edit.end(), outcome);
    event.joinSeries(next);
    // No second occurrence at the time of one that stays, and the past is not rewritten.
    var taken = new HashSet<Instant>();
    taken.add(event.getStart().toInstant());
    kept.forEach(e -> taken.add(e.getStart().toInstant()));
    eventRepository.saveAll(
        generated.stream()
            .filter(g -> g.getStart().toInstant().isAfter(command.now()))
            .filter(g -> !taken.contains(g.getStart().toInstant()))
            .toList());
    outcome.kept = kept.size();
  }

  /** Of the events, those with someone signed up (not cancelled) or attendance taken. */
  private Set<EventId> idsWithPeople(List<Event> events) {
    var ids = events.stream().map(Event::getId).toList();
    var found = new HashSet<EventId>();
    eventRegistrationRepository
        .findByEventIdInAndStatusNot(ids, RegistrationStatus.CANCELLED)
        .forEach(r -> found.add(r.getEvent().getId()));
    found.addAll(
        findEventIdsWithAttendanceQueryHandler
            .handle(new FindEventIdsWithAttendanceQuery(ids))
            .eventIds());
    return found;
  }

  // ---- helpers

  /**
   * Writes the edit into the event, with the given times, and remembers what changed that the
   * people signed up for it should know: the time, the name, the place and the speakers (not the
   * description).
   */
  private void apply(
      Event event, Edit edit, ZonedDateTime start, ZonedDateTime end, Outcome outcome) {
    var command = edit.command();
    var time = !sameInstant(event.getStart(), start) || !sameInstant(event.getEnd(), end);
    var name = !Objects.equals(event.getTitle(), command.name());
    var place = !Objects.equals(blankToNull(event.getAddress()), blankToNull(command.address()));
    var oldSpeakers = event.getSpeakers().stream().map(Person::getId).collect(Collectors.toSet());
    var speakers = !oldSpeakers.equals(new HashSet<>(command.speakerIds()));
    event.edit(
        command.name(), start, end, command.address(), command.description(), edit.speakers());
    if (time || name || place || speakers) {
      outcome.notices.add(new Notice(event, time, name, place, speakers));
    }
  }

  private static String blankToNull(String text) {
    return text == null || text.isBlank() ? null : text.trim();
  }

  private void fillSeries(EventSeries series, Edit edit) {
    series.rename(edit.command().name());
    series.setAddress(edit.command().address());
    series.setDescription(edit.command().description());
    series.setSpeakers(new ArrayList<>(edit.speakers()));
  }

  private static boolean sameInstant(ZonedDateTime a, ZonedDateTime b) {
    return a.toInstant().equals(b.toInstant());
  }

  private static ZoneId zoneOf(Edit edit, ZoneId fallback) {
    return edit.command().timeZone() == null ? fallback : edit.command().timeZone();
  }

  private static LocalDate today(Edit edit, ZoneId zone) {
    return LocalDate.ofInstant(edit.command().now(), zone);
  }

  private List<FindPersonResult> findPeople(List<PartyId> speakerIds) {
    var found = findPeopleByIdsQueryHandler.handle(new FindPeopleByIdsQuery(speakerIds));
    var missing = new HashSet<>(speakerIds);
    found.stream().map(FindPersonResult::id).forEach(missing::remove);
    if (!missing.isEmpty()) {
      throw new NoSuchElementException(
          "Speakers not found: " + missing.stream().map(PartyId::value).toList());
    }
    return found;
  }

  // ---- email to whoever signed up

  /** One email per person listing what changed and the dates of their occurrences. */
  private int notifyRegistrations(List<Notice> notices, Edit edit, ZoneId zone) {
    if (notices.isEmpty()) {
      return 0;
    }
    // Whoever holds a place is told about the events the place gives them: a
    // sub-event that inherits the registration of its mega event is told to the
    // people of the mega event.
    var coveredBy = new LinkedHashMap<EventId, List<Notice>>();
    for (var notice : notices) {
      effectiveRegistration
          .governing(notice.event())
          .ifPresent(
              governing ->
                  coveredBy.computeIfAbsent(governing.getId(), k -> new ArrayList<>()).add(notice));
    }
    var registrations =
        eventRegistrationRepository.findByEventIdInAndStatusNot(
            List.copyOf(coveredBy.keySet()), RegistrationStatus.CANCELLED);
    var byPerson = new LinkedHashMap<String, Map<EventId, Notice>>();
    var names = new LinkedHashMap<String, String>();
    for (var registration : registrations) {
      var email = registration.getEmail().trim().toLowerCase();
      var theirNotices = byPerson.computeIfAbsent(email, k -> new LinkedHashMap<>());
      for (var notice : coveredBy.get(registration.getEvent().getId())) {
        theirNotices.putIfAbsent(notice.event().getId(), notice);
      }
      names.putIfAbsent(email, registration.getFirstName());
    }
    byPerson.forEach(
        (email, byEvent) -> {
          var theirs = new ArrayList<>(byEvent.values());
          theirs.sort(Comparator.comparing(n -> n.event().getStart().toInstant()));
          var title = theirs.getFirst().event().getTitle();
          sendEmailCommandHandler.handle(
              new SendEmailCommand(
                  "EVENT_CHANGED",
                  email,
                  "Cambios en «" + title + "»",
                  body(names.get(email), theirs, edit, zone)));
        });
    return byPerson.size();
  }

  private String body(String firstName, List<Notice> notices, Edit edit, ZoneId zone) {
    var command = edit.command();
    var first = notices.getFirst().event();
    var text = new StringBuilder("Hola " + firstName + ",\n\n");
    text.append("Hubo cambios en «")
        .append(first.getTitle())
        .append("», al que te inscribiste:\n\n");
    if (notices.stream().anyMatch(Notice::time)) {
      text.append("• Cambió la fecha u hora (ver abajo).\n");
    }
    if (notices.stream().anyMatch(Notice::name)) {
      text.append("• Nuevo nombre: «").append(command.name()).append("».\n");
    }
    if (notices.stream().anyMatch(Notice::place)) {
      var place = blankToNull(command.address());
      text.append(
          place == null ? "• Ya no hay un lugar definido.\n" : "• Nuevo lugar: " + place + ".\n");
    }
    if (notices.stream().anyMatch(Notice::speakers)) {
      text.append(
          edit.speakerNames().isEmpty()
              ? "• Ya no hay ponentes definidos.\n"
              : "• Ponentes: " + String.join(", ", edit.speakerNames()) + ".\n");
    }
    text.append("\nTu inscripción es para (hora de ").append(zone.getId()).append("):\n\n");
    for (var notice : notices) {
      var event = notice.event();
      text.append("• ")
          .append(WHEN.format(event.getStart().withZoneSameInstant(zone)))
          .append(" a ")
          .append(
              DateTimeFormatter.ofPattern("HH:mm").format(event.getEnd().withZoneSameInstant(zone)))
          .append("\n  ")
          .append(appUrl.replaceAll("/+$", ""))
          .append("/e/")
          .append(event.getShareSlug())
          .append("\n");
    }
    text.append("\nSi ya no puedes asistir, cancela tu inscripción desde ese enlace.\n");
    return text.toString();
  }
}
