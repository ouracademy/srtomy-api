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

  /**
   * Gets how an event is shared. Returns who can open it through its link, the people it is shared
   * with and how many sign-ups it has. Needs manager rights on the event.
   *
   * @param id Id of the event or course.
   */
  @GetMapping("events/{id}/sharing")
  public EventSharingResponse sharing(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    manage(jwt, id);
    return getEventSharingQueryHandler
        .handle(new GetEventSharingQuery(new EventId(id)))
        .map(EventSharingResponse::from)
        .orElseThrow(() -> new NoSuchElementException("Event not found"));
  }

  /**
   * Changes who can open an event through its link: only invited people, anyone in the church or
   * anyone with the link. Needs manager rights on the event.
   *
   * @param id Id of the event or course.
   */
  @PutMapping("events/{id}/sharing")
  public EventSharingResponse setSharing(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid EventSharingRequest request) {
    manage(jwt, id);
    setEventSharingCommandHandler.handle(
        new SetEventSharingCommand(
            new EventId(id),
            request.generalAccess(),
            Boolean.TRUE.equals(request.applyToProgram())));
    return sharing(jwt, id);
  }

  /**
   * Replaces the shared link of an event with a new one. The old link stops working, so everyone
   * who had it needs the new one. Needs manager rights on the event.
   *
   * @param id Id of the event or course.
   */
  @PostMapping("events/{id}/sharing/link/renew")
  public EventLinkResponse renewLink(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    manage(jwt, id);
    return new EventLinkResponse(
        renewEventLinkCommandHandler.handle(new RenewEventLinkCommand(new EventId(id))).slug());
  }

  /**
   * Shares an event with one person by email, as viewer or editor, and emails them. Sharing again
   * with the same email changes the role. Needs manager rights on the event.
   *
   * @param id Id of the event or course.
   */
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

  /**
   * Stops sharing an event with one person. Needs manager rights on the event.
   *
   * @param id Id of the event or course.
   * @param shareId Id of the share, from the sharing of the event.
   */
  @DeleteMapping("events/{id}/shares/{shareId}")
  public ResponseEntity<Void> unshare(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID shareId) {
    manage(jwt, id);
    unshareEventCommandHandler.handle(
        new UnshareEventCommand(new EventId(id), new EventShareId(shareId)));
    return ResponseEntity.noContent().build();
  }

  /**
   * Gets how an event takes sign-ups: its tickets with their seats and prices, whether an account
   * is required and whether it is open. Needs {@code registrations.read}.
   *
   * @param id Id of the event or course.
   */
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

  /**
   * Turns on or changes the sign-ups of an event: its tickets, seats and whether an account is
   * required. The list of tickets replaces the current one. Needs {@code registrations.configure}.
   *
   * @param id Id of the event or course.
   */
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
                            ticket.salesEnd(),
                            ticket.subEventIds() == null
                                ? List.of()
                                : ticket.subEventIds().stream().map(EventId::new).toList()))
                .toList()));
    return currentRegistration(id);
  }

  /**
   * Closes the sign-ups of an event so nobody else can sign up, or opens them again. Existing
   * sign-ups are kept. Needs {@code registrations.close}.
   *
   * @param id Id of the event or course.
   */
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

  /**
   * Turns off the sign-ups of an event altogether, removing its tickets. Needs {@code
   * registrations.configure}.
   *
   * @param id Id of the event or course.
   */
  @DeleteMapping("events/{id}/registration")
  public ResponseEntity<Void> removeRegistration(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    registrationAuthorization.require(jwt, id, Permission.REGISTRATIONS_CONFIGURE);
    removeEventRegistrationCommandHandler.handle(
        new RemoveEventRegistrationCommand(new EventId(id)));
    return ResponseEntity.noContent().build();
  }

  /**
   * Lists the people signed up for an event or course, with their ticket and whether they checked
   * in. Needs {@code registrations.read}.
   *
   * @param id Id of the event or course.
   */
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

  /**
   * Cancels the sign-up of one person, freeing their seat. Needs {@code registrations.remove}.
   *
   * @param id Id of the event or course.
   * @param registrationId Id of the sign-up, from the list of registrations of the event.
   */
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
   * Lists the events and courses of the signed-in user: those where they have a place or were
   * present, most recent first.
   */
  @GetMapping("me/events")
  public List<MyEventResponse> myEvents(@AuthenticationPrincipal Jwt jwt) {
    return listMyEventsQueryHandler.handle(new ListMyEventsQuery(jwt.getSubject())).stream()
        .map(MyEventResponse::from)
        .toList();
  }

  /** Lists the events that other people shared with the signed-in user. */
  @GetMapping("me/shared-events")
  public List<SharedWithMeResponse> sharedWithMe(@AuthenticationPrincipal Jwt jwt) {
    return listEventsSharedWithQueryHandler
        .handle(new ListEventsSharedWithQuery(jwt.getSubject()))
        .stream()
        .map(SharedWithMeResponse::from)
        .toList();
  }

  /**
   * Gets the shared page of an event or course by its slug: what, when, where, its program and its
   * tickets. 404 when the visitor cannot open it. No sign-in needed for events open to anyone with
   * the link.
   *
   * @param slug The slug of the event: the last part of its shared link ({@code /e/{slug}}).
   */
  @GetMapping("public/events/{slug}")
  public SharedEventResponse sharedEvent(
      @AuthenticationPrincipal Jwt jwt, @PathVariable String slug) {
    return getSharedEventQueryHandler
        .handle(new GetSharedEventQuery(slug, subjectOf(jwt)))
        .map(SharedEventResponse::from)
        .orElseThrow(() -> new NoSuchElementException("Event not found"));
  }

  /**
   * Signs someone up for an event or course with one of its tickets. Anyone who can open the page
   * of the event can; the answer carries the ticket and the code to cancel.
   *
   * @param slug The slug of the event: the last part of its shared link ({@code /e/{slug}}).
   */
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

  /**
   * Cancels a sign-up with the code its owner received when signing up. No account needed.
   *
   * @param cancelCode The cancel code given when the person signed up.
   */
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
