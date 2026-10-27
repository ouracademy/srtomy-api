package org.ouracademy.ovejafiel.event.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.ouracademy.ovejafiel.event.command.cancel_event_registration.CancelEventRegistrationCommand;
import org.ouracademy.ovejafiel.event.command.cancel_event_registration.CancelEventRegistrationCommandHandler;
import org.ouracademy.ovejafiel.event.command.register_for_event.RegisterForEventCommand;
import org.ouracademy.ovejafiel.event.command.register_for_event.RegisterForEventCommandHandler;
import org.ouracademy.ovejafiel.event.command.register_for_event.RegisterForEventResult;
import org.ouracademy.ovejafiel.event.command.renew_event_link.RenewEventLinkCommand;
import org.ouracademy.ovejafiel.event.command.renew_event_link.RenewEventLinkCommandHandler;
import org.ouracademy.ovejafiel.event.command.renew_event_link.RenewEventLinkResult;
import org.ouracademy.ovejafiel.event.command.set_event_sharing.SetEventSharingCommand;
import org.ouracademy.ovejafiel.event.command.set_event_sharing.SetEventSharingCommandHandler;
import org.ouracademy.ovejafiel.event.command.set_event_sharing.SetEventSharingResult;
import org.ouracademy.ovejafiel.event.command.share_event.ShareEventCommand;
import org.ouracademy.ovejafiel.event.command.share_event.ShareEventCommandHandler;
import org.ouracademy.ovejafiel.event.command.share_event.ShareEventResult;
import org.ouracademy.ovejafiel.event.command.unshare_event.UnshareEventCommand;
import org.ouracademy.ovejafiel.event.command.unshare_event.UnshareEventCommandHandler;
import org.ouracademy.ovejafiel.event.domain.AccessLevel;
import org.ouracademy.ovejafiel.event.domain.EventId;
import org.ouracademy.ovejafiel.event.domain.EventShareId;
import org.ouracademy.ovejafiel.event.domain.GeneralAccess;
import org.ouracademy.ovejafiel.event.domain.LinkRole;
import org.ouracademy.ovejafiel.event.domain.ShareRole;
import org.ouracademy.ovejafiel.event.query.EventSharingResult;
import org.ouracademy.ovejafiel.event.query.authorize_event.AuthorizeEventQuery;
import org.ouracademy.ovejafiel.event.query.authorize_event.AuthorizeEventQueryHandler;
import org.ouracademy.ovejafiel.event.query.authorize_event.AuthorizeEventResult;
import org.ouracademy.ovejafiel.event.query.get_event_sharing.GetEventSharingQueryHandler;
import org.ouracademy.ovejafiel.event.query.get_shared_event.GetSharedEventQuery;
import org.ouracademy.ovejafiel.event.query.get_shared_event.GetSharedEventQueryHandler;
import org.ouracademy.ovejafiel.event.query.get_shared_event.GetSharedEventResult;
import org.ouracademy.ovejafiel.event.query.list_event_registrations.ListEventRegistrationsQueryHandler;
import org.ouracademy.ovejafiel.event.query.list_events_shared_with.ListEventsSharedWithQueryHandler;
import org.ouracademy.ovejafiel.event.query.list_events_shared_with.SharedWithMeResult;
import org.ouracademy.ovejafiel.event.query.list_my_events.ListMyEventsQueryHandler;
import org.ouracademy.ovejafiel.shared.OvejafielApplication;
import org.ouracademy.ovejafiel.shared.SecurityConfig;
import org.ouracademy.ovejafiel.shared.TestSecurityConfig;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
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
  @MockitoBean GetSharedEventQueryHandler getSharedEventQueryHandler;
  @MockitoBean RegisterForEventCommandHandler registerForEventCommandHandler;
  @MockitoBean ListEventsSharedWithQueryHandler listEventsSharedWithQueryHandler;
  @MockitoBean ListMyEventsQueryHandler listMyEventsQueryHandler;

  UUID id = UUID.randomUUID();

  String bearer() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  EventSharingResult sharing() {
    return new EventSharingResult(
        GeneralAccess.CHURCH,
        LinkRole.VIEWER,
        "abc",
        false,
        List.of(
            new EventSharingResult.Share(EventShareId.generate(), "ana@x.org", ShareRole.EDITOR)),
        2);
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
        canRegister,
        false);
  }

  @Test
  void managingTheSharingNeedsToManageTheEvent() {
    given(authorizeEventQueryHandler.handle(any())).willThrow(new AccessDeniedException("no"));

    for (var request :
        List.of(
            mvc.get().uri("/v1/events/" + id + "/sharing"),
            mvc.get().uri("/v1/events/" + id + "/registrations"),
            mvc.post().uri("/v1/events/" + id + "/sharing/link/renew"))) {
      assertThat(request.header("Authorization", bearer())).hasStatus(403);
    }
    verifyNoInteractions(
        getEventSharingQueryHandler,
        listEventRegistrationsQueryHandler,
        renewEventLinkCommandHandler);
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
  void changesTheGeneralAccessAndTheLinkRole() {
    given(authorizeEventQueryHandler.handle(any()))
        .willReturn(new AuthorizeEventResult(AccessLevel.MANAGER));
    given(setEventSharingCommandHandler.handle(any())).willReturn(new SetEventSharingResult());
    given(getEventSharingQueryHandler.handle(any())).willReturn(Optional.of(sharing()));

    assertThat(
            mvc.put()
                .uri("/v1/events/" + id + "/sharing")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"generalAccess\":\"CHURCH\",\"linkRole\":\"VIEWER\"}"))
        .hasStatusOk();
    verify(setEventSharingCommandHandler)
        .handle(new SetEventSharingCommand(new EventId(id), GeneralAccess.CHURCH, LinkRole.VIEWER));
  }

  @Test
  void aRefusedLinkRoleIsABadRequest() {
    given(authorizeEventQueryHandler.handle(any()))
        .willReturn(new AuthorizeEventResult(AccessLevel.MANAGER));
    given(setEventSharingCommandHandler.handle(any()))
        .willThrow(new DomainValidationException("Solo se puede abrir la inscripción de un curso"));

    assertThat(
            mvc.put()
                .uri("/v1/events/" + id + "/sharing")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"generalAccess\":\"CHURCH\",\"linkRole\":\"REGISTRANT\"}"))
        .hasStatus(400);
    assertThat(
            mvc.put()
                .uri("/v1/events/" + id + "/sharing")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"generalAccess\":\"CHURCH\"}"))
        .hasStatus(400);
  }

  @Test
  void sharesWithAnEmail() {
    given(authorizeEventQueryHandler.handle(any()))
        .willReturn(new AuthorizeEventResult(AccessLevel.MANAGER));
    var share =
        new EventSharingResult.Share(EventShareId.generate(), "ana@x.org", ShareRole.VIEWER);
    given(shareEventCommandHandler.handle(any())).willReturn(new ShareEventResult(share, true));

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
        .willReturn(new RegisterForEventResult(false));

    var body =
        assertThat(
                mvc.post()
                    .uri("/v1/public/events/abc/registrations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"firstName\":\"Lu\",\"lastName\":\"Mora\",\"email\":\"lu@x.org\","
                            + "\"phoneNumber\":\"999\"}"))
            .hasStatus(201)
            .bodyJson();
    body.extractingPath("$.alreadyRegistered").isEqualTo(false);
    verify(registerForEventCommandHandler)
        .handle(new RegisterForEventCommand("abc", null, "Lu", "Mora", "lu@x.org", "999"));
  }

  @Test
  void aSignUpWithATokenKeepsTheUser() {
    given(registerForEventCommandHandler.handle(any()))
        .willReturn(new RegisterForEventResult(true));

    assertThat(
            mvc.post()
                .uri("/v1/public/events/abc/registrations")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Lu\",\"lastName\":\"Mora\",\"email\":\"lu@x.org\"}"))
        .hasStatus(201);
    verify(registerForEventCommandHandler)
        .handle(new RegisterForEventCommand("abc", "test-user", "Lu", "Mora", "lu@x.org", null));
  }

  @Test
  void aBadSignUpIsRejectedBeforeTheHandler() {
    for (var body :
        List.of(
            "{\"firstName\":\"\",\"lastName\":\"Mora\",\"email\":\"lu@x.org\"}",
            "{\"firstName\":\"Lu\",\"lastName\":\"Mora\",\"email\":\"nope\"}",
            "{\"firstName\":\"Lu\",\"lastName\":\"Mora\"}")) {
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
  void aLinkThatDoesNotAllowSigningUpIsForbiddenOrNotFound() {
    given(registerForEventCommandHandler.handle(any()))
        .willThrow(new AccessDeniedException("cerrada"))
        .willThrow(new NoSuchElementException());
    var body = "{\"firstName\":\"Lu\",\"lastName\":\"Mora\",\"email\":\"lu@x.org\"}";

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
  void theOtherPublicRoutesStayClosedWithoutASession() {
    assertThat(mvc.get().uri("/v1/me/shared-events")).hasStatus(401);
    assertThat(mvc.get().uri("/v1/events/" + id + "/registrations")).hasStatus(401);
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
