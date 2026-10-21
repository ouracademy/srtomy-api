package org.ouracademy.srtomy.event;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.srtomy.event.command.CreateEventCommand;
import org.ouracademy.srtomy.event.command.CreateEventCommandHandler;
import org.ouracademy.srtomy.event.command.DeleteEventCommand;
import org.ouracademy.srtomy.event.command.DeleteEventCommandHandler;
import org.ouracademy.srtomy.event.command.EditEventCommand;
import org.ouracademy.srtomy.event.command.EditEventCommandHandler;
import org.ouracademy.srtomy.event.controller.dto.EditEventRequest;
import org.ouracademy.srtomy.event.controller.dto.EventDeleteRequest;
import org.ouracademy.srtomy.event.controller.dto.EventRequest;
import org.ouracademy.srtomy.event.controller.dto.EventResponse;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.query.FindEventByIdQuery;
import org.ouracademy.srtomy.event.query.FindEventByIdQueryHandler;
import org.ouracademy.srtomy.event.query.SearchEventsQuery;
import org.ouracademy.srtomy.event.query.SearchEventsQueryHandler;
import org.ouracademy.srtomy.event.repository.EventRepository;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.repository.PersonRepository;
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

/**
 * The event endpoints go through the event command/query handlers. The attendance endpoints still
 * use the repositories directly (including person's PersonRepository) until they move to the
 * attendance module.
 */
@RestController
@RequestMapping("events")
@AllArgsConstructor
@Tag(name = "events")
public class EventController {

  SearchEventsQueryHandler searchEventsQueryHandler;
  FindEventByIdQueryHandler findEventByIdQueryHandler;
  CreateEventCommandHandler createEventCommandHandler;
  DeleteEventCommandHandler deleteEventCommandHandler;
  EditEventCommandHandler editEventCommandHandler;

  // Attendance only; removed when attendance moves to its own module.
  EventRepository eventRepository;
  PersonRepository personRepository;
  EventAttendanceRepository eventAttendanceRepository;

  @GetMapping
  public List<EventResponse> findAll(
      @RequestParam(required = true) UUID organizationId,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
    return searchEventsQueryHandler
        .handle(
            new SearchEventsQuery(
                new PartyId(organizationId),
                start.atZone(ZoneId.of("UTC")),
                end.atZone(ZoneId.of("UTC"))))
        .stream()
        .map(EventResponse::from)
        .toList();
  }

  @GetMapping("{id}")
  public ResponseEntity<EventResponse> findById(@PathVariable UUID id) {
    return ResponseEntity.of(
        findEventByIdQueryHandler
            .handle(new FindEventByIdQuery(new EventId(id)))
            .map(EventResponse::from));
  }

  @PostMapping
  @ResponseStatus(code = HttpStatus.CREATED)
  public UUID create(@RequestBody @Valid EventRequest eventRequest) {
    return createEventCommandHandler
        .handle(
            new CreateEventCommand(
                eventRequest.getName(),
                OffsetDateTime.parse(eventRequest.getStartTime()),
                OffsetDateTime.parse(eventRequest.getEndTime()),
                eventRequest.getAddress(),
                eventRequest.getDescription(),
                eventRequest.getIsACourse(),
                new PartyId(eventRequest.getOrganizationId()),
                eventRequest.getSpeakerIds().stream().map(PartyId::new).toList(),
                eventRequest.getDaysOfWeek(),
                eventRequest.getEndRecur(),
                eventRequest.isRecurrent()))
        .id()
        .value();
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

  @DeleteMapping("{id}")
  public void delete(@PathVariable UUID id, @RequestBody @Valid EventDeleteRequest request) {
    deleteEventCommandHandler.handle(new DeleteEventCommand(new EventId(id), request.getOption()));
  }

  @PutMapping("{id}")
  public ResponseEntity<UUID> edit(
      @PathVariable UUID id, @RequestBody @Valid EditEventRequest request) {
    var result =
        editEventCommandHandler.handle(new EditEventCommand(new EventId(id), request.getOption()));
    return ResponseEntity.ok(result.id().value());
  }
}
