package org.ouracademy.srtomy.event;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.NotImplementedException;
import org.ouracademy.srtomy.organization.OrganizationRepository;
import org.ouracademy.srtomy.person.PartyId;
import org.ouracademy.srtomy.person.PersonRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("events")
@AllArgsConstructor
@Tag(name = "events")
public class EventController {

  EventRepository eventRepository;
  SingleEventRepository singleEventRepository;
  PersonRepository personRepository;
  OrganizationRepository organizationRepository;
  EventAttendanceRepository eventAttendanceRepository;

  @GetMapping
  public List<SingleEvent> findAll(
      @RequestParam(required = true) UUID organizationId,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
    return eventRepository.searchEventsOfOrgIdAndBetweenDates(
        new PartyId(organizationId), start.atZone(ZoneId.of("UTC")), end.atZone(ZoneId.of("UTC")));
  }

  @GetMapping("{id}")
  public ResponseEntity<SingleEvent> findById(@PathVariable UUID id) {
    return ResponseEntity.of(singleEventRepository.findById(new EventId(id)));
  }

  @PostMapping
  @ResponseStatus(code = HttpStatus.CREATED)
  @Transactional
  public UUID create(@RequestBody @Valid EventRequest eventRequest) {
    var startTime = OffsetDateTime.parse(eventRequest.startTime);
    var endTime = OffsetDateTime.parse(eventRequest.endTime);

    var start = startTime.atZoneSameInstant(ZoneId.of("UTC"));
    var end = endTime.atZoneSameInstant(ZoneId.of("UTC"));

    // eventRequest.startTime.
    var event =
        eventRequest.isRecurrent()
            ? new RecurrentEvent(
                eventRequest.name, start, end, eventRequest.daysOfWeek, eventRequest.endRecur)
            : new SingleEvent(eventRequest.name, start, end);
    mapToModel(eventRequest, event);
    var id = eventRepository.save(event).getId();

    if (event instanceof RecurrentEvent recurrentEvent) {
      var events = recurrentEvent.expand(startTime, endTime);
      eventRepository.saveAll(events);
    }

    return id.value();
  }

  private void mapToModel(EventRequest eventRequest, Event event) {
    event.organization =
        organizationRepository.getReferenceById(new PartyId(eventRequest.organizationId));
    event.address = eventRequest.address;
    event.speakers =
        eventRequest.speakerIds.stream()
            .map(id -> personRepository.getReferenceById(new PartyId(id)))
            .toList();
    event.description = eventRequest.description;
    event.isACourse = eventRequest.isACourse;
  }

  @GetMapping("{id}/attendance")
  public ResponseEntity<EventAttendanceResponse> getAttendance(@PathVariable UUID id) {
    return eventAttendanceRepository
        .findByEventId(new EventId(id))
        .map(x -> EventAttendanceResponse.builder().personIds(x.getPersonIds()).build())
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.noContent().build());
  }

  @PutMapping("{id}/attendance")
  @ResponseStatus(code = HttpStatus.CREATED)
  @Transactional
  public void registerAttendance(
      @PathVariable UUID id, @RequestBody @Valid AttendanceRequest attendanceRequest) {
    var eventId = new EventId(id);
    var eventAttendance =
        eventAttendanceRepository
            .findByEventId(eventId)
            .orElse(new EventAttendance(eventRepository.getReferenceById(eventId)));

    eventAttendance = eventAttendanceRepository.save(eventAttendance);
    eventAttendance.setPeople(
        attendanceRequest.personIds.stream()
            .map(personId -> personRepository.getReferenceById(new PartyId(personId)))
            .toList());
    eventAttendanceRepository.save(eventAttendance);
  }

  @Data
  public static class EventDeleteRequest {
    EventChangeOption option = EventChangeOption.THIS_EVENT;
  }

  @DeleteMapping("{id}")
  public void delete(@PathVariable UUID id, @RequestBody @Valid EventDeleteRequest request) {
    var event = singleEventRepository.findById(new EventId(id)).orElseThrow();
    switch (request.option) {
      case THIS_EVENT:
        eventRepository.delete(event);
        break;
      case THIS_AND_THE_FOLLOWING_EVENTS:
        var followingEvents =
            singleEventRepository.findByRecurrentEventAndStartAfter(
                event.recurrentEvent, event.start);
        eventRepository.deleteAll(followingEvents);
        eventRepository.delete(event);
        break;
      case ALL_EVENTS:
        eventRepository.deleteAll(singleEventRepository.findByRecurrentEvent(event.recurrentEvent));
        eventRepository.delete(event.recurrentEvent);
        break;
    }
  }

  @PutMapping("{id}")
  public ResponseEntity<Event> edit(
      @PathVariable UUID id, @RequestBody @Valid EditEventRequest request) {

    // TODO:

    throw new NotImplementedException("TODO:");
  }
}
