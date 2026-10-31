package org.ouracademy.ovejafiel.event.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.ovejafiel.event.command.cancel_event_registration.CancelEventRegistrationCommand;
import org.ouracademy.ovejafiel.event.command.cancel_event_registration.CancelEventRegistrationCommandHandler;
import org.ouracademy.ovejafiel.event.command.cancel_registration_by_code.CancelRegistrationByCodeCommand;
import org.ouracademy.ovejafiel.event.command.cancel_registration_by_code.CancelRegistrationByCodeCommandHandler;
import org.ouracademy.ovejafiel.event.command.register_for_event.RegisterForEventCommand;
import org.ouracademy.ovejafiel.event.command.register_for_event.RegisterForEventCommandHandler;
import org.ouracademy.ovejafiel.event.command.register_for_event.RegisterForEventResult;
import org.ouracademy.ovejafiel.event.command.remove_event_registration.RemoveEventRegistrationCommand;
import org.ouracademy.ovejafiel.event.command.remove_event_registration.RemoveEventRegistrationCommandHandler;
import org.ouracademy.ovejafiel.event.command.renew_event_link.RenewEventLinkCommand;
import org.ouracademy.ovejafiel.event.command.renew_event_link.RenewEventLinkCommandHandler;
import org.ouracademy.ovejafiel.event.command.renew_event_link.RenewEventLinkResult;
import org.ouracademy.ovejafiel.event.command.set_event_registration.SetEventRegistrationCommand;
import org.ouracademy.ovejafiel.event.command.set_event_registration.SetEventRegistrationCommandHandler;
import org.ouracademy.ovejafiel.event.command.set_event_sharing.SetEventSharingCommand;
import org.ouracademy.ovejafiel.event.command.set_event_sharing.SetEventSharingCommandHandler;
import org.ouracademy.ovejafiel.event.command.set_event_sharing.SetEventSharingResult;
import org.ouracademy.ovejafiel.event.command.set_registration_open.SetRegistrationOpenCommand;
import org.ouracademy.ovejafiel.event.command.set_registration_open.SetRegistrationOpenCommandHandler;
import org.ouracademy.ovejafiel.event.command.share_event.ShareEventCommand;
import org.ouracademy.ovejafiel.event.command.share_event.ShareEventCommandHandler;
import org.ouracademy.ovejafiel.event.command.share_event.ShareEventResult;
import org.ouracademy.ovejafiel.event.command.unshare_event.UnshareEventCommand;
import org.ouracademy.ovejafiel.event.command.unshare_event.UnshareEventCommandHandler;
import org.ouracademy.ovejafiel.event.domain.AccessLevel;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventShareId;
import org.ouracademy.ovejafiel.event.domain.GeneralAccess;
import org.ouracademy.ovejafiel.event.domain.RegistrationStatus;
import org.ouracademy.ovejafiel.event.domain.ShareRole;
import org.ouracademy.ovejafiel.event.domain.TicketTypeId;
import org.ouracademy.ovejafiel.event.query.EventSharingResult;
import org.ouracademy.ovejafiel.event.query.authorize_event.AuthorizeEventQuery;
import org.ouracademy.ovejafiel.event.query.authorize_event.AuthorizeEventQueryHandler;
import org.ouracademy.ovejafiel.event.query.authorize_event.AuthorizeEventResult;
import org.ouracademy.ovejafiel.event.query.get_event_registration.GetEventRegistrationQueryHandler;
import org.ouracademy.ovejafiel.event.query.get_event_registration.GetEventRegistrationResult;
import org.ouracademy.ovejafiel.event.query.get_event_sharing.GetEventSharingQueryHandler;
import org.ouracademy.ovejafiel.event.query.get_shared_event.GetSharedEventQuery;
import org.ouracademy.ovejafiel.event.query.get_shared_event.GetSharedEventQueryHandler;
import org.ouracademy.ovejafiel.event.query.get_shared_event.GetSharedEventResult;
import org.ouracademy.ovejafiel.event.query.list_event_registrations.ListEventRegistrationsQueryHandler;
import org.ouracademy.ovejafiel.event.query.list_events_shared_with.ListEventsSharedWithQueryHandler;
import org.ouracademy.ovejafiel.event.query.list_events_shared_with.SharedWithMeResult;
import org.ouracademy.ovejafiel.event.query.list_my_events.ListMyEventsQueryHandler;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.outbox.domain.OutboxMessageId;
import org.ouracademy.ovejafiel.outbox.domain.OutboxStatus;
import org.ouracademy.ovejafiel.outbox.query.find_outbox_statuses.FindOutboxStatusesQueryHandler;
import org.ouracademy.ovejafiel.outbox.query.find_outbox_statuses.FindOutboxStatusesResult;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.SecurityConfig;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(EventSharingController.class)
@ContextConfiguration(classes = OvejafielApplication.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class EventSharingControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean AuthorizeEventQueryHandler authorizeEventQueryHandler;
  @MockitoBean GetEventSharingQueryHandler getEventSharingQueryHandler;
  @MockitoBean SetEventSharingCommandHandler setEventSharingCommandHandler;
  @MockitoBean RenewEventLinkCommandHandler renewEventLinkCommandHandler;
  @MockitoBean ShareEventCommandHandler shareEventCommandHandler;
  @MockitoBean UnshareEventCommandHandler unshareEventCommandHandler;
  @MockitoBean ListEventRegistrationsQueryHandler listEventRegistrationsQueryHandler;
  @MockitoBean CancelEventRegistrationCommandHandler cancelEventRegistrationCommandHandler;
  @MockitoBean CancelRegistrationByCodeCommandHandler cancelRegistrationByCodeCommandHandler;
  @MockitoBean GetEventRegistrationQueryHandler getEventRegistrationQueryHandler;
  @MockitoBean SetEventRegistrationCommandHandler setEventRegistrationCommandHandler;
  @MockitoBean RemoveEventRegistrationCommandHandler removeEventRegistrationCommandHandler;
  @MockitoBean SetRegistrationOpenCommandHandler setRegistrationOpenCommandHandler;
  @MockitoBean EventRegistrationAuthorization registrationAuthorization;
  @MockitoBean GetSharedEventQueryHandler getSharedEventQueryHandler;
  @MockitoBean RegisterForEventCommandHandler registerForEventCommandHandler;
  @MockitoBean ListEventsSharedWithQueryHandler listEventsSharedWithQueryHandler;
  @MockitoBean ListMyEventsQueryHandler listMyEventsQueryHandler;
  @MockitoBean FindOutboxStatusesQueryHandler findOutboxStatusesQueryHandler;

  UUID id = UUID.randomUUID();
  static final TicketTypeId TICKET = TicketTypeId.generate();

  String bearer() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  EventSharingResult sharing() {
    return new EventSharingResult(
        GeneralAccess.CHURCH,
        "abc",
        false,
        List.of(
            new EventSharingResult.Share(EventShareId.generate(), "ana@x.org", ShareRole.EDITOR)),
        2);
  }

  String signUpBody() {
    return "{\"ticketTypeId\":\""
        + TICKET.value()
        + "\",\"firstName\":\"Lu\",\"lastName\":\"Mora\",\"email\":\"lu@x.org\"}";
  }

  GetSharedEventResult page(boolean canRegister) {
    return new GetSharedEventResult(
        "Curso",
        "desc",
        "Av. 1",
        ZonedDateTime.parse("2030-01-05T10:00:00Z"),
        ZonedDateTime.parse("2030-01-05T12:00:00Z"),
        true,
        "Jóvenes",
        null,
        canRegister
            ? new GetSharedEventResult.Registration(
                true,
                false,
                false,
                List.of(new GetSharedEventResult.Ticket(TICKET, "General", 2000, "PEN", true, 5)))
            : null,
        canRegister,
        null,
        List.of(),
        null);
  }

  @Test
  void managingTheSharingNeedsToManageTheEvent() {
    given(authorizeEventQueryHandler.handle(any())).willThrow(new AccessDeniedException("no"));

    for (var request :
        List.of(
            mvc.get().uri("/v1/events/" + id + "/sharing"),
            mvc.post().uri("/v1/events/" + id + "/sharing/link/renew"))) {
      assertThat(request.header("Authorization", bearer())).hasStatus(403);
    }
    verifyNoInteractions(getEventSharingQueryHandler, renewEventLinkCommandHandler);
  }

  @Test
  void showsHowTheEventIsShared() {
    given(authorizeEventQueryHandler.handle(any()))
        .willReturn(new AuthorizeEventResult(AccessLevel.MANAGER));
    given(getEventSharingQueryHandler.handle(any())).willReturn(Optional.of(sharing()));

    var body =
        assertThat(mvc.get().uri("/v1/events/" + id + "/sharing").header("Authorization", bearer()))
            .hasStatusOk()
            .bodyJson();
    body.extractingPath("$.generalAccess").isEqualTo("CHURCH");
    body.extractingPath("$.slug").isEqualTo("abc");
    body.extractingPath("$.shares[0].email").isEqualTo("ana@x.org");
    body.extractingPath("$.registrations").isEqualTo(2);
    verify(authorizeEventQueryHandler)
        .handle(new AuthorizeEventQuery("test-user", new EventId(id), AccessLevel.MANAGER));
  }

  @Test
  void changesTheGeneralAccess() {
    given(authorizeEventQueryHandler.handle(any()))
        .willReturn(new AuthorizeEventResult(AccessLevel.MANAGER));
    given(setEventSharingCommandHandler.handle(any())).willReturn(new SetEventSharingResult());
    given(getEventSharingQueryHandler.handle(any())).willReturn(Optional.of(sharing()));

    assertThat(
            mvc.put()
                .uri("/v1/events/" + id + "/sharing")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"generalAccess\":\"CHURCH\"}"))
        .hasStatusOk();
    verify(setEventSharingCommandHandler)
        .handle(new SetEventSharingCommand(new EventId(id), GeneralAccess.CHURCH));
  }

  @Test
  void aSharingWithoutGeneralAccessIsABadRequest() {
    given(authorizeEventQueryHandler.handle(any()))
        .willReturn(new AuthorizeEventResult(AccessLevel.MANAGER));

    assertThat(
            mvc.put()
                .uri("/v1/events/" + id + "/sharing")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .hasStatus(400);
    verifyNoInteractions(setEventSharingCommandHandler);
  }

  @Test
  void sharesWithAnEmail() {
    given(authorizeEventQueryHandler.handle(any()))
        .willReturn(new AuthorizeEventResult(AccessLevel.MANAGER));
    var share =
        new EventSharingResult.Share(EventShareId.generate(), "ana@x.org", ShareRole.VIEWER);
    var messageId = OutboxMessageId.generate();
    given(shareEventCommandHandler.handle(any()))
        .willReturn(new ShareEventResult(share, messageId));
    given(findOutboxStatusesQueryHandler.handle(any()))
        .willReturn(new FindOutboxStatusesResult(Map.of(messageId, OutboxStatus.SENT)));

    var body =
        assertThat(
                mvc.post()
                    .uri("/v1/events/" + id + "/shares")
                    .header("Authorization", bearer())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"ana@x.org\",\"role\":\"VIEWER\"}"))
            .hasStatus(201)
            .bodyJson();
    body.extractingPath("$.emailSent").isEqualTo(true);
    body.extractingPath("$.emailStatus").isEqualTo("SENT");
    body.extractingPath("$.share.email").isEqualTo("ana@x.org");
    verify(shareEventCommandHandler)
        .handle(new ShareEventCommand("test-user", new EventId(id), "ana@x.org", ShareRole.VIEWER));
    assertThat(
            mvc.post()
                .uri("/v1/events/" + id + "/shares")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nope\",\"role\":\"VIEWER\"}"))
        .hasStatus(400);
  }

  @Test
  void stopsSharingAndCancelsSignUps() {
    given(authorizeEventQueryHandler.handle(any()))
        .willReturn(new AuthorizeEventResult(AccessLevel.MANAGER));
    var other = UUID.randomUUID();

    assertThat(
            mvc.delete()
                .uri("/v1/events/" + id + "/shares/" + other)
                .header("Authorization", bearer()))
        .hasStatus(204);
    assertThat(
            mvc.delete()
                .uri("/v1/events/" + id + "/registrations/" + other)
                .header("Authorization", bearer()))
        .hasStatus(204);
    verify(unshareEventCommandHandler)
        .handle(new UnshareEventCommand(new EventId(id), new EventShareId(other)));
    verify(cancelEventRegistrationCommandHandler).handle(any(CancelEventRegistrationCommand.class));
    verify(registrationAuthorization).require(any(), eq(id), eq(Permission.REGISTRATIONS_REMOVE));
  }

  @Test
  void renewsTheLink() {
    given(authorizeEventQueryHandler.handle(any()))
        .willReturn(new AuthorizeEventResult(AccessLevel.MANAGER));
    given(renewEventLinkCommandHandler.handle(new RenewEventLinkCommand(new EventId(id))))
        .willReturn(new RenewEventLinkResult("new-slug"));

    assertThat(
            mvc.post()
                .uri("/v1/events/" + id + "/sharing/link/renew")
                .header("Authorization", bearer()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$.slug")
        .isEqualTo("new-slug");
  }

  @Test
  void thePublicPageNeedsNoSession() {
    given(getSharedEventQueryHandler.handle(new GetSharedEventQuery("abc", null)))
        .willReturn(Optional.of(page(true)));

    var body = assertThat(mvc.get().uri("/v1/public/events/abc")).hasStatusOk().bodyJson();
    body.extractingPath("$.title").isEqualTo("Curso");
    body.extractingPath("$.canRegister").isEqualTo(true);
    body.extractingPath("$.registration.tickets[0].name").isEqualTo("General");
    body.extractingPath("$.registration.tickets[0].remaining").isEqualTo(5);
  }

  @Test
  void aTokenWidensWhatTheVisitorSees() {
    given(getSharedEventQueryHandler.handle(new GetSharedEventQuery("abc", "test-user")))
        .willReturn(Optional.of(page(false)));

    assertThat(mvc.get().uri("/v1/public/events/abc").header("Authorization", bearer()))
        .hasStatusOk();
  }

  @Test
  void aPageTheVisitorCannotOpenIsNotFound() {
    given(getSharedEventQueryHandler.handle(any())).willReturn(Optional.empty());

    assertThat(mvc.get().uri("/v1/public/events/abc")).hasStatus(404);
  }

  @Test
  void anyoneSignsUpWithoutASession() {
    given(registerForEventCommandHandler.handle(any()))
        .willReturn(
            new RegisterForEventResult(false, RegistrationStatus.CONFIRMED, "cancel", "ticket"));

    var body =
        assertThat(
                mvc.post()
                    .uri("/v1/public/events/abc/registrations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"ticketTypeId\":\""
                            + TICKET.value()
                            + "\",\"firstName\":\"Lu\","
                            + "\"lastName\":\"Mora\",\"email\":\"lu@x.org\","
                            + "\"phoneNumber\":\"999\"}"))
            .hasStatus(201)
            .bodyJson();
    body.extractingPath("$.alreadyRegistered").isEqualTo(false);
    body.extractingPath("$.status").isEqualTo("CONFIRMED");
    body.extractingPath("$.cancelCode").isEqualTo("cancel");
    body.extractingPath("$.ticketCode").isEqualTo("ticket");
    verify(registerForEventCommandHandler)
        .handle(new RegisterForEventCommand("abc", null, TICKET, "Lu", "Mora", "lu@x.org", "999"));
  }

  @Test
  void aSignUpWithATokenKeepsTheUser() {
    given(registerForEventCommandHandler.handle(any()))
        .willReturn(
            new RegisterForEventResult(true, RegistrationStatus.CONFIRMED, "cancel", "ticket"));

    assertThat(
            mvc.post()
                .uri("/v1/public/events/abc/registrations")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(signUpBody()))
        .hasStatus(201);
    verify(registerForEventCommandHandler)
        .handle(
            new RegisterForEventCommand(
                "abc", "test-user", TICKET, "Lu", "Mora", "lu@x.org", null));
  }

  @Test
  void aBadSignUpIsRejectedBeforeTheHandler() {
    for (var body :
        List.of(
            signUpBody().replace("\"Lu\"", "\"\""),
            signUpBody().replace("lu@x.org", "nope"),
            "{\"firstName\":\"Lu\",\"lastName\":\"Mora\",\"email\":\"lu@x.org\"}")) {
      assertThat(
              mvc.post()
                  .uri("/v1/public/events/abc/registrations")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .hasStatus(400);
    }
    verifyNoInteractions(registerForEventCommandHandler);
  }

  @Test
  void aSignUpTheVisitorCannotMakeIsForbiddenOrNotFound() {
    given(registerForEventCommandHandler.handle(any()))
        .willThrow(new AccessDeniedException("cerrada"))
        .willThrow(new NoSuchElementException());
    var body = signUpBody();

    assertThat(
            mvc.post()
                .uri("/v1/public/events/abc/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .hasStatus(403);
    assertThat(
            mvc.post()
                .uri("/v1/public/events/abc/registrations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .hasStatus(404);
  }

  @Test
  void organizersReadHowTheEventTakesSignUps() {
    given(getEventRegistrationQueryHandler.handle(any()))
        .willReturn(
            Optional.of(
                new GetEventRegistrationResult(
                    true,
                    true,
                    false,
                    3,
                    2,
                    List.of(
                        new GetEventRegistrationResult.Ticket(
                            TICKET, "General", 0, "PEN", 20, null, null, 3)))));

    var body =
        assertThat(
                mvc.get()
                    .uri("/v1/events/" + id + "/registration")
                    .header("Authorization", bearer()))
            .hasStatusOk()
            .bodyJson();
    body.extractingPath("$.enabled").isEqualTo(true);
    // The total of seats and the waitlist are gone.
    body.doesNotHavePath("$.capacity");
    body.doesNotHavePath("$.waitlistEnabled");
    body.doesNotHavePath("$.waitlisted");
    body.extractingPath("$.tickets[0].sold").isEqualTo(3);
    body.extractingPath("$.checkedIn").isEqualTo(2);
    verify(registrationAuthorization).require(any(), eq(id), eq(Permission.REGISTRATIONS_READ));
  }

  @Test
  void organizersSetTheRegistrationWithItsTickets() {
    given(getEventRegistrationQueryHandler.handle(any()))
        .willReturn(Optional.of(GetEventRegistrationResult.disabled()));

    assertThat(
            mvc.put()
                .uri("/v1/events/" + id + "/registration")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"requiresAccount\":true,"
                        + "\"tickets\":[{\"name\":\"General\",\"priceCents\":0,\"quantity\":30}]}"))
        .hasStatusOk();

    var command = ArgumentCaptor.forClass(SetEventRegistrationCommand.class);
    verify(setEventRegistrationCommandHandler).handle(command.capture());
    assertThat(command.getValue().eventId()).isEqualTo(new EventId(id));
    assertThat(command.getValue().requiresAccount()).isTrue();
    assertThat(command.getValue().tickets())
        .extracting(SetEventRegistrationCommand.Ticket::name)
        .containsExactly("General");
    verify(registrationAuthorization)
        .require(any(), eq(id), eq(Permission.REGISTRATIONS_CONFIGURE));
  }

  @Test
  void aRegistrationWithoutTicketsIsABadRequest() {

    assertThat(
            mvc.put()
                .uri("/v1/events/" + id + "/registration")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"requiresAccount\":false,\"tickets\":[]}"))
        .hasStatus(400);
    verifyNoInteractions(setEventRegistrationCommandHandler);
  }

  @Test
  void organizersTakeTheRegistrationAway() {

    assertThat(
            mvc.delete()
                .uri("/v1/events/" + id + "/registration")
                .header("Authorization", bearer()))
        .hasStatus(204);
    verify(removeEventRegistrationCommandHandler)
        .handle(new RemoveEventRegistrationCommand(new EventId(id)));
    verify(registrationAuthorization)
        .require(any(), eq(id), eq(Permission.REGISTRATIONS_CONFIGURE));
  }

  @Test
  void organizersCloseAndReopenTheRegistration() {
    given(getEventRegistrationQueryHandler.handle(any()))
        .willReturn(Optional.of(GetEventRegistrationResult.disabled()));

    for (var open : List.of(false, true)) {
      assertThat(
              mvc.put()
                  .uri("/v1/events/" + id + "/registration/open")
                  .header("Authorization", bearer())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"open\":" + open + "}"))
          .hasStatusOk();
      verify(setRegistrationOpenCommandHandler)
          .handle(new SetRegistrationOpenCommand(new EventId(id), open));
    }
    verify(registrationAuthorization, times(2))
        .require(any(), eq(id), eq(Permission.REGISTRATIONS_CLOSE));
  }

  @Test
  void closingTheRegistrationNeedsAnOpenFlag() {
    assertThat(
            mvc.put()
                .uri("/v1/events/" + id + "/registration/open")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .hasStatus(400);
    verifyNoInteractions(setRegistrationOpenCommandHandler);
  }

  @Test
  void eachThingYouDoWithTheRegistrationNeedsItsOwnPermission() {
    doThrow(new AccessDeniedException("no"))
        .when(registrationAuthorization)
        .require(any(), any(), any());

    for (var request :
        List.of(
            mvc.get().uri("/v1/events/" + id + "/registration"),
            mvc.get().uri("/v1/events/" + id + "/registrations"),
            mvc.delete().uri("/v1/events/" + id + "/registration"),
            mvc.delete().uri("/v1/events/" + id + "/registrations/" + UUID.randomUUID()))) {
      assertThat(request.header("Authorization", bearer())).hasStatus(403);
    }
    assertThat(
            mvc.put()
                .uri("/v1/events/" + id + "/registration/open")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"open\":false}"))
        .hasStatus(403);
    verifyNoInteractions(
        getEventRegistrationQueryHandler,
        listEventRegistrationsQueryHandler,
        setEventRegistrationCommandHandler,
        removeEventRegistrationCommandHandler,
        cancelEventRegistrationCommandHandler,
        setRegistrationOpenCommandHandler);
  }

  @Test
  void anyoneCancelsWithTheCodeTheyKept() {
    assertThat(mvc.delete().uri("/v1/public/registrations/the-code")).hasStatus(204);

    verify(cancelRegistrationByCodeCommandHandler)
        .handle(new CancelRegistrationByCodeCommand("the-code"));
  }

  @Test
  void anUnknownCancelCodeIsNotFound() {
    given(cancelRegistrationByCodeCommandHandler.handle(any()))
        .willThrow(new NoSuchElementException());

    assertThat(mvc.delete().uri("/v1/public/registrations/nope")).hasStatus(404);
  }

  @Test
  void theOtherPublicRoutesStayClosedWithoutASession() {
    assertThat(mvc.get().uri("/v1/me/shared-events")).hasStatus(401);
    assertThat(mvc.get().uri("/v1/events/" + id + "/registrations")).hasStatus(401);
    assertThat(mvc.get().uri("/v1/events/" + id + "/registration")).hasStatus(401);
    assertThat(
            mvc.post()
                .uri("/v1/events/" + id + "/shares")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@x.org\",\"role\":\"VIEWER\"}"))
        .hasStatus(401);
  }

  @Test
  void listsTheEventsSharedWithTheUser() {
    given(listEventsSharedWithQueryHandler.handle(any()))
        .willReturn(
            List.of(
                new SharedWithMeResult(
                    "Retiro",
                    ZonedDateTime.parse("2030-01-05T10:00:00Z"),
                    false,
                    "Jóvenes",
                    ShareRole.EDITOR,
                    "abc")));

    var body =
        assertThat(mvc.get().uri("/v1/me/shared-events").header("Authorization", bearer()))
            .hasStatusOk()
            .bodyJson();
    body.extractingPath("$[0].title").isEqualTo("Retiro");
    body.extractingPath("$[0].slug").isEqualTo("abc");
    body.extractingPath("$[0].role").isEqualTo("EDITOR");
  }
}
