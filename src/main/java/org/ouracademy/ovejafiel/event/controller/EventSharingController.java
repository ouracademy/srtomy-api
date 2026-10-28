package org.ouracademy.ovejafiel.event.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.ovejafiel.event.command.cancel_event_registration.CancelEventRegistrationCommand;
import org.ouracademy.ovejafiel.event.command.cancel_event_registration.CancelEventRegistrationCommandHandler;
import org.ouracademy.ovejafiel.event.command.cancel_registration_by_code.CancelRegistrationByCodeCommand;
import org.ouracademy.ovejafiel.event.command.cancel_registration_by_code.CancelRegistrationByCodeCommandHandler;
import org.ouracademy.ovejafiel.event.command.register_for_event.RegisterForEventCommand;
import org.ouracademy.ovejafiel.event.command.register_for_event.RegisterForEventCommandHandler;
import org.ouracademy.ovejafiel.event.command.remove_event_registration.RemoveEventRegistrationCommand;
import org.ouracademy.ovejafiel.event.command.remove_event_registration.RemoveEventRegistrationCommandHandler;
import org.ouracademy.ovejafiel.event.command.renew_event_link.RenewEventLinkCommand;
import org.ouracademy.ovejafiel.event.command.renew_event_link.RenewEventLinkCommandHandler;
import org.ouracademy.ovejafiel.event.command.set_event_registration.SetEventRegistrationCommand;
import org.ouracademy.ovejafiel.event.command.set_event_registration.SetEventRegistrationCommandHandler;
import org.ouracademy.ovejafiel.event.command.set_event_sharing.SetEventSharingCommand;
import org.ouracademy.ovejafiel.event.command.set_event_sharing.SetEventSharingCommandHandler;
import org.ouracademy.ovejafiel.event.command.set_registration_open.SetRegistrationOpenCommand;
import org.ouracademy.ovejafiel.event.command.set_registration_open.SetRegistrationOpenCommandHandler;
import org.ouracademy.ovejafiel.event.command.share_event.ShareEventCommand;
import org.ouracademy.ovejafiel.event.command.share_event.ShareEventCommandHandler;
import org.ouracademy.ovejafiel.event.command.unshare_event.UnshareEventCommand;
import org.ouracademy.ovejafiel.event.command.unshare_event.UnshareEventCommandHandler;
import org.ouracademy.ovejafiel.event.controller.dto.EventLinkResponse;
import org.ouracademy.ovejafiel.event.controller.dto.EventRegistrationRequest;
import org.ouracademy.ovejafiel.event.controller.dto.EventRegistrationResponse;
import org.ouracademy.ovejafiel.event.controller.dto.EventRegistrationSetupRequest;
import org.ouracademy.ovejafiel.event.controller.dto.EventRegistrationSetupResponse;
import org.ouracademy.ovejafiel.event.controller.dto.EventSharingRequest;
import org.ouracademy.ovejafiel.event.controller.dto.EventSharingResponse;
import org.ouracademy.ovejafiel.event.controller.dto.MyEventResponse;
import org.ouracademy.ovejafiel.event.controller.dto.RegistrationOpenRequest;
import org.ouracademy.ovejafiel.event.controller.dto.RegistrationOutcomeResponse;
import org.ouracademy.ovejafiel.event.controller.dto.ShareEventRequest;
import org.ouracademy.ovejafiel.event.controller.dto.ShareEventResponse;
import org.ouracademy.ovejafiel.event.controller.dto.SharedEventResponse;
import org.ouracademy.ovejafiel.event.controller.dto.SharedWithMeResponse;
import org.ouracademy.ovejafiel.event.domain.AccessLevel;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventRegistrationId;
import org.ouracademy.ovejafiel.event.domain.EventShareId;
import org.ouracademy.ovejafiel.event.domain.TicketTypeId;
import org.ouracademy.ovejafiel.event.query.authorize_event.AuthorizeEventQuery;
import org.ouracademy.ovejafiel.event.query.authorize_event.AuthorizeEventQueryHandler;
import org.ouracademy.ovejafiel.event.query.get_event_registration.GetEventRegistrationQuery;
import org.ouracademy.ovejafiel.event.query.get_event_registration.GetEventRegistrationQueryHandler;
import org.ouracademy.ovejafiel.event.query.get_event_sharing.GetEventSharingQuery;
import org.ouracademy.ovejafiel.event.query.get_event_sharing.GetEventSharingQueryHandler;
import org.ouracademy.ovejafiel.event.query.get_shared_event.GetSharedEventQuery;
import org.ouracademy.ovejafiel.event.query.get_shared_event.GetSharedEventQueryHandler;
import org.ouracademy.ovejafiel.event.query.list_event_registrations.ListEventRegistrationsQuery;
import org.ouracademy.ovejafiel.event.query.list_event_registrations.ListEventRegistrationsQueryHandler;
import org.ouracademy.ovejafiel.event.query.list_events_shared_with.ListEventsSharedWithQuery;
import org.ouracademy.ovejafiel.event.query.list_events_shared_with.ListEventsSharedWithQueryHandler;
import org.ouracademy.ovejafiel.event.query.list_my_events.ListMyEventsQuery;
import org.ouracademy.ovejafiel.event.query.list_my_events.ListMyEventsQueryHandler;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.outbox.domain.OutboxStatus;
import org.ouracademy.ovejafiel.outbox.query.find_outbox_statuses.FindOutboxStatusesQuery;
import org.ouracademy.ovejafiel.outbox.query.find_outbox_statuses.FindOutboxStatusesQueryHandler;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sharing events and courses like Drive: who can open one through its link, the people it is shared
 * with, and the sign-ups. Managing all that needs {@code events.write} on the event's node. The
 * pages under {@code public/} need no sign-in; a token, when sent, only widens what the visitor
 * sees (the church, the shares).
 */
@RestController
@AllArgsConstructor
@Tag(name = "event-sharing")
public class EventSharingController {

  AuthorizeEventQueryHandler authorizeEventQueryHandler;
  GetEventSharingQueryHandler getEventSharingQueryHandler;
  SetEventSharingCommandHandler setEventSharingCommandHandler;
  RenewEventLinkCommandHandler renewEventLinkCommandHandler;
  ShareEventCommandHandler shareEventCommandHandler;
  UnshareEventCommandHandler unshareEventCommandHandler;
  ListEventRegistrationsQueryHandler listEventRegistrationsQueryHandler;
  CancelEventRegistrationCommandHandler cancelEventRegistrationCommandHandler;
  CancelRegistrationByCodeCommandHandler cancelRegistrationByCodeCommandHandler;
  GetEventRegistrationQueryHandler getEventRegistrationQueryHandler;
  SetEventRegistrationCommandHandler setEventRegistrationCommandHandler;
  RemoveEventRegistrationCommandHandler removeEventRegistrationCommandHandler;
  SetRegistrationOpenCommandHandler setRegistrationOpenCommandHandler;
  EventRegistrationAuthorization registrationAuthorization;
  GetSharedEventQueryHandler getSharedEventQueryHandler;
  RegisterForEventCommandHandler registerForEventCommandHandler;
  ListEventsSharedWithQueryHandler listEventsSharedWithQueryHandler;
  ListMyEventsQueryHandler listMyEventsQueryHandler;
  FindOutboxStatusesQueryHandler findOutboxStatusesQueryHandler;

  @GetMapping("events/{id}/sharing")
  public EventSharingResponse sharing(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    manage(jwt, id);
    return getEventSharingQueryHandler
        .handle(new GetEventSharingQuery(new EventId(id)))
        .map(EventSharingResponse::from)
        .orElseThrow(() -> new NoSuchElementException("Event not found"));
  }

  @PutMapping("events/{id}/sharing")
  public EventSharingResponse setSharing(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid EventSharingRequest request) {
    manage(jwt, id);
    setEventSharingCommandHandler.handle(
        new SetEventSharingCommand(new EventId(id), request.generalAccess()));
    return sharing(jwt, id);
  }

  /** Revokes the current link and makes a new one. */
  @PostMapping("events/{id}/sharing/link/renew")
  public EventLinkResponse renewLink(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    manage(jwt, id);
    return new EventLinkResponse(
        renewEventLinkCommandHandler.handle(new RenewEventLinkCommand(new EventId(id))).slug());
  }

  /** Shares the event with someone by email (and tells them); again changes the role. */
  @PostMapping("events/{id}/shares")
  @ResponseStatus(HttpStatus.CREATED)
  public ShareEventResponse share(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid ShareEventRequest request) {
    manage(jwt, id);
    var result =
        shareEventCommandHandler.handle(
            new ShareEventCommand(
                jwt.getSubject(), new EventId(id), request.email(), request.role()));
    // The attempt right after the commit has already run: report how it went.
    var emailStatus =
        findOutboxStatusesQueryHandler
            .handle(new FindOutboxStatusesQuery(List.of(result.emailMessageId())))
            .of(result.emailMessageId());
    return new ShareEventResponse(
        EventSharingResponse.shareFrom(result.share()),
        emailStatus == OutboxStatus.SENT,
        emailStatus == null ? null : emailStatus.name());
  }

  @DeleteMapping("events/{id}/shares/{shareId}")
  public ResponseEntity<Void> unshare(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID shareId) {
    manage(jwt, id);
    unshareEventCommandHandler.handle(
        new UnshareEventCommand(new EventId(id), new EventShareId(shareId)));
    return ResponseEntity.noContent().build();
  }

  /** How the event takes sign-ups: tickets, seats, waitlist. */
  @GetMapping("events/{id}/registration")
  public EventRegistrationSetupResponse registration(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    registrationAuthorization.require(jwt, id, Permission.REGISTRATIONS_READ);
    return currentRegistration(id);
  }

  /** What the registration is now, for whoever has just changed it (so it needs no read access). */
  private EventRegistrationSetupResponse currentRegistration(UUID id) {
    return getEventRegistrationQueryHandler
        .handle(new GetEventRegistrationQuery(new EventId(id)))
        .map(EventRegistrationSetupResponse::from)
        .orElseThrow(() -> new NoSuchElementException("Event not found"));
  }

  @PutMapping("events/{id}/registration")
  public EventRegistrationSetupResponse setRegistration(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid EventRegistrationSetupRequest request) {
    registrationAuthorization.require(jwt, id, Permission.REGISTRATIONS_CONFIGURE);
    setEventRegistrationCommandHandler.handle(
        new SetEventRegistrationCommand(
            new EventId(id),
            request.requiresAccount(),
            request.tickets().stream()
                .map(
                    ticket ->
                        new SetEventRegistrationCommand.Ticket(
                            ticket.id() == null ? null : new TicketTypeId(ticket.id()),
                            ticket.name(),
                            ticket.priceCents(),
                            ticket.currency(),
                            ticket.quantity(),
                            ticket.salesStart(),
                            ticket.salesEnd()))
                .toList()));
    return currentRegistration(id);
  }

  /** Closes the registration (nobody else can sign up) or opens it again. */
  @PutMapping("events/{id}/registration/open")
  public EventRegistrationSetupResponse setRegistrationOpen(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid RegistrationOpenRequest request) {
    registrationAuthorization.require(jwt, id, Permission.REGISTRATIONS_CLOSE);
    setRegistrationOpenCommandHandler.handle(
        new SetRegistrationOpenCommand(new EventId(id), request.open()));
    return currentRegistration(id);
  }

  @DeleteMapping("events/{id}/registration")
  public ResponseEntity<Void> removeRegistration(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    registrationAuthorization.require(jwt, id, Permission.REGISTRATIONS_CONFIGURE);
    removeEventRegistrationCommandHandler.handle(
        new RemoveEventRegistrationCommand(new EventId(id)));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("events/{id}/registrations")
  public List<EventRegistrationResponse> registrations(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    registrationAuthorization.require(jwt, id, Permission.REGISTRATIONS_READ);
    return listEventRegistrationsQueryHandler
        .handle(new ListEventRegistrationsQuery(new EventId(id)))
        .stream()
        .map(EventRegistrationResponse::from)
        .toList();
  }

  @DeleteMapping("events/{id}/registrations/{registrationId}")
  public ResponseEntity<Void> cancelRegistration(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID registrationId) {
    registrationAuthorization.require(jwt, id, Permission.REGISTRATIONS_REMOVE);
    cancelEventRegistrationCommandHandler.handle(
        new CancelEventRegistrationCommand(
            new EventId(id), new EventRegistrationId(registrationId)));
    return ResponseEntity.noContent().build();
  }

  /**
   * The events and courses where the signed-in user has a place or was present, most recent first.
   */
  @GetMapping("me/events")
  public List<MyEventResponse> myEvents(@AuthenticationPrincipal Jwt jwt) {
    return listMyEventsQueryHandler.handle(new ListMyEventsQuery(jwt.getSubject())).stream()
        .map(MyEventResponse::from)
        .toList();
  }

  /** The events other people shared with the signed-in user. */
  @GetMapping("me/shared-events")
  public List<SharedWithMeResponse> sharedWithMe(@AuthenticationPrincipal Jwt jwt) {
    return listEventsSharedWithQueryHandler
        .handle(new ListEventsSharedWithQuery(jwt.getSubject()))
        .stream()
        .map(SharedWithMeResponse::from)
        .toList();
  }

  /** The shared page of an event; 404 when the visitor cannot open it. */
  @GetMapping("public/events/{slug}")
  public SharedEventResponse sharedEvent(
      @AuthenticationPrincipal Jwt jwt, @PathVariable String slug) {
    return getSharedEventQueryHandler
        .handle(new GetSharedEventQuery(slug, subjectOf(jwt)))
        .map(SharedEventResponse::from)
        .orElseThrow(() -> new NoSuchElementException("Event not found"));
  }

  /** Signs up for an event or course with a ticket; anyone who can open its page can. */
  @PostMapping("public/events/{slug}/registrations")
  @ResponseStatus(HttpStatus.CREATED)
  public RegistrationOutcomeResponse register(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable String slug,
      @RequestBody @Valid EventRegistrationRequest request) {
    var result =
        registerForEventCommandHandler.handle(
            new RegisterForEventCommand(
                slug,
                subjectOf(jwt),
                new TicketTypeId(request.ticketTypeId()),
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phoneNumber()));
    return RegistrationOutcomeResponse.from(result);
  }

  /** Cancels a sign-up with the code its owner kept; no account needed. */
  @DeleteMapping("public/registrations/{cancelCode}")
  public ResponseEntity<Void> cancelWithCode(@PathVariable String cancelCode) {
    cancelRegistrationByCodeCommandHandler.handle(new CancelRegistrationByCodeCommand(cancelCode));
    return ResponseEntity.noContent().build();
  }

  private void manage(Jwt jwt, UUID eventId) {
    authorizeEventQueryHandler.handle(
        new AuthorizeEventQuery(jwt.getSubject(), new EventId(eventId), AccessLevel.MANAGER));
  }

  private static String subjectOf(Jwt jwt) {
    return jwt == null ? null : jwt.getSubject();
  }
}
