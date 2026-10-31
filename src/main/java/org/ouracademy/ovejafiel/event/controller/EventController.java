package org.ouracademy.ovejafiel.event.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.event.command.create_event.CreateEventCommand;
import org.ouracademy.ovejafiel.event.command.create_event.CreateEventCommandHandler;
import org.ouracademy.ovejafiel.event.command.delete_event.DeleteEventCommand;
import org.ouracademy.ovejafiel.event.command.delete_event.DeleteEventCommandHandler;
import org.ouracademy.ovejafiel.event.command.edit_event.EditEventCommand;
import org.ouracademy.ovejafiel.event.command.edit_event.EditEventCommandHandler;
import org.ouracademy.ovejafiel.event.controller.dto.EditEventRequest;
import org.ouracademy.ovejafiel.event.controller.dto.EditEventResponse;
import org.ouracademy.ovejafiel.event.controller.dto.EventDeleteRequest;
import org.ouracademy.ovejafiel.event.controller.dto.EventRequest;
import org.ouracademy.ovejafiel.event.controller.dto.EventResponse;
import org.ouracademy.ovejafiel.event.domain.AccessLevel;
import org.ouracademy.ovejafiel.event.domain.EventChangeOption;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.query.authorize_event.AuthorizeEventQuery;
import org.ouracademy.ovejafiel.event.query.authorize_event.AuthorizeEventQueryHandler;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQuery;
import org.ouracademy.ovejafiel.event.query.find_event_by_id.FindEventByIdQueryHandler;
import org.ouracademy.ovejafiel.event.query.search_events.SearchEventsQuery;
import org.ouracademy.ovejafiel.event.query.search_events.SearchEventsQueryHandler;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
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
  AuthorizeEventQueryHandler authorizeEventQueryHandler;
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
    authorizeOnEvent(jwt, id, AccessLevel.VIEWER);
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
    if (eventRequest.getParentEventId() != null) {
      // Adding to someone's mega event takes edit rights on it, not only on the child's node.
      authorizeOnEvent(jwt, eventRequest.getParentEventId(), AccessLevel.EDITOR);
    }
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
                eventRequest.getRecurrence() == null
                    ? null
                    : eventRequest.getRecurrence().toRecurrence(),
                eventRequest.zone(),
                eventRequest.getParentEventId() == null
                    ? null
                    : new EventId(eventRequest.getParentEventId()),
                eventRequest.getRegistrationMode()))
        .id()
        .value();
  }

  @DeleteMapping("{id}")
  public void delete(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid EventDeleteRequest request) {
    authorizeOnEvent(jwt, id, AccessLevel.MANAGER);
    deleteEventCommandHandler.handle(new DeleteEventCommand(new EventId(id), request.option()));
  }

  @PutMapping("{id}")
  public EditEventResponse edit(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid EditEventRequest request) {
    authorizeOnEvent(jwt, id, AccessLevel.EDITOR);
    var result =
        editEventCommandHandler.handle(
            new EditEventCommand(
                new EventId(id),
                request.getOption() == null ? EventChangeOption.THIS_EVENT : request.getOption(),
                request.getName(),
                OffsetDateTime.parse(request.getStartTime()),
                OffsetDateTime.parse(request.getEndTime()),
                request.getAddress(),
                request.getDescription(),
                request.getIsACourse(),
                new PartyId(request.getOrganizationId()),
                request.getSpeakerIds().stream().map(PartyId::new).toList(),
                request.getRecurrence() == null ? null : request.getRecurrence().toRecurrence(),
                request.getTimeZone() == null ? null : request.zone(),
                request.getParentEventId() == null ? null : new EventId(request.getParentEventId()),
                request.getRegistrationMode(),
                Instant.now()));
    return new EditEventResponse(result.id().value(), result.keptEvents(), result.notifiedPeople());
  }

  /**
   * 404 when the event does not exist, 403 when the user cannot do at least {@code required} with
   * it: the church tree, a share or the general access of the event can give it.
   */
  private void authorizeOnEvent(Jwt jwt, UUID eventId, AccessLevel required) {
    authorizeEventQueryHandler.handle(
        new AuthorizeEventQuery(jwt.getSubject(), new EventId(eventId), required));
  }
}
