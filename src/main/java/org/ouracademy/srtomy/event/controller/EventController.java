package org.ouracademy.srtomy.event.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.srtomy.event.command.create_event.CreateEventCommand;
import org.ouracademy.srtomy.event.command.create_event.CreateEventCommandHandler;
import org.ouracademy.srtomy.event.command.delete_event.DeleteEventCommand;
import org.ouracademy.srtomy.event.command.delete_event.DeleteEventCommandHandler;
import org.ouracademy.srtomy.event.command.edit_event.EditEventCommand;
import org.ouracademy.srtomy.event.command.edit_event.EditEventCommandHandler;
import org.ouracademy.srtomy.event.controller.dto.EditEventRequest;
import org.ouracademy.srtomy.event.controller.dto.EventDeleteRequest;
import org.ouracademy.srtomy.event.controller.dto.EventRequest;
import org.ouracademy.srtomy.event.controller.dto.EventResponse;
import org.ouracademy.srtomy.event.domain.EventId;
import org.ouracademy.srtomy.event.query.find_event_by_id.FindEventByIdQuery;
import org.ouracademy.srtomy.event.query.find_event_by_id.FindEventByIdQueryHandler;
import org.ouracademy.srtomy.event.query.find_event_organization.FindEventOrganizationQuery;
import org.ouracademy.srtomy.event.query.find_event_organization.FindEventOrganizationQueryHandler;
import org.ouracademy.srtomy.event.query.search_events.SearchEventsQuery;
import org.ouracademy.srtomy.event.query.search_events.SearchEventsQueryHandler;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
 * Event endpoints. The {@code events/{id}/attendance} endpoints live in the attendance module's own
 * controller.
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
  FindEventOrganizationQueryHandler findEventOrganizationQueryHandler;
  AuthorizeQueryHandler authorizeQueryHandler;

  @GetMapping
  public List<EventResponse> findAll(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam(required = true) UUID organizationId,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
    authorizeQueryHandler.handle(
        new AuthorizeQuery(jwt.getSubject(), new PartyId(organizationId), Permission.EVENTS_READ));
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
  public ResponseEntity<EventResponse> findById(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    authorizeOnEvent(jwt, id, Permission.EVENTS_READ);
    return ResponseEntity.of(
        findEventByIdQueryHandler
            .handle(new FindEventByIdQuery(new EventId(id)))
            .map(EventResponse::from));
  }

  @PostMapping
  @ResponseStatus(code = HttpStatus.CREATED)
  public UUID create(
      @AuthenticationPrincipal Jwt jwt, @RequestBody @Valid EventRequest eventRequest) {
    authorizeQueryHandler.handle(
        new AuthorizeQuery(
            jwt.getSubject(),
            new PartyId(eventRequest.getOrganizationId()),
            Permission.EVENTS_WRITE));
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

  @DeleteMapping("{id}")
  public void delete(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid EventDeleteRequest request) {
    authorizeOnEvent(jwt, id, Permission.EVENTS_WRITE);
    deleteEventCommandHandler.handle(new DeleteEventCommand(new EventId(id), request.getOption()));
  }

  @PutMapping("{id}")
  public ResponseEntity<UUID> edit(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid EditEventRequest request) {
    authorizeOnEvent(jwt, id, Permission.EVENTS_WRITE);
    var result =
        editEventCommandHandler.handle(new EditEventCommand(new EventId(id), request.getOption()));
    return ResponseEntity.ok(result.id().value());
  }

  /** 404 when the event does not exist, 403 when the user lacks the permission on its node. */
  private void authorizeOnEvent(Jwt jwt, UUID eventId, Permission permission) {
    var organizationId =
        findEventOrganizationQueryHandler
            .handle(new FindEventOrganizationQuery(new EventId(eventId)))
            .orElseThrow(() -> new NoSuchElementException("Event not found: " + eventId))
            .organizationId();
    authorizeQueryHandler.handle(new AuthorizeQuery(jwt.getSubject(), organizationId, permission));
  }
}
