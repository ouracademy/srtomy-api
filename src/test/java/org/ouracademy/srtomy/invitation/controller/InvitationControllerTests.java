package org.ouracademy.srtomy.invitation.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.srtomy.invitation.command.accept_invitation.AcceptInvitationCommand;
import org.ouracademy.srtomy.invitation.command.accept_invitation.AcceptInvitationCommandHandler;
import org.ouracademy.srtomy.invitation.command.accept_invitation.AcceptInvitationResult;
import org.ouracademy.srtomy.invitation.command.create_invitation.CreateInvitationCommand;
import org.ouracademy.srtomy.invitation.command.create_invitation.CreateInvitationCommandHandler;
import org.ouracademy.srtomy.invitation.command.create_invitation.CreateInvitationResult;
import org.ouracademy.srtomy.invitation.command.revoke_invitation.RevokeInvitationCommand;
import org.ouracademy.srtomy.invitation.command.revoke_invitation.RevokeInvitationCommandHandler;
import org.ouracademy.srtomy.invitation.domain.InvitationId;
import org.ouracademy.srtomy.invitation.domain.InvitationStatus;
import org.ouracademy.srtomy.invitation.query.InvitationResult;
import org.ouracademy.srtomy.invitation.query.find_invitation_by_id.FindInvitationByIdQuery;
import org.ouracademy.srtomy.invitation.query.find_invitation_by_id.FindInvitationByIdQueryHandler;
import org.ouracademy.srtomy.invitation.query.find_invitation_by_token.FindInvitationByTokenQuery;
import org.ouracademy.srtomy.invitation.query.find_invitation_by_token.FindInvitationByTokenQueryHandler;
import org.ouracademy.srtomy.invitation.query.list_invitations_for_email.ListInvitationsForEmailQueryHandler;
import org.ouracademy.srtomy.invitation.query.list_invitations_of_organization.ListInvitationsOfOrganizationQueryHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.shared.SecurityConfig;
import org.ouracademy.srtomy.shared.SrtomyApplication;
import org.ouracademy.srtomy.shared.TestSecurityConfig;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;
import org.ouracademy.srtomy.user.domain.UserId;
import org.ouracademy.srtomy.user.query.UserResult;
import org.ouracademy.srtomy.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@WebMvcTest(InvitationController.class)
@ContextConfiguration(classes = SrtomyApplication.class)
@Import({TestSecurityConfig.class, SecurityConfig.class})
class InvitationControllerTests {

  @Autowired MockMvcTester mvc;
  @Autowired JwtEncoder jwtEncoder;

  @MockitoBean CreateInvitationCommandHandler createInvitationCommandHandler;
  @MockitoBean AcceptInvitationCommandHandler acceptInvitationCommandHandler;
  @MockitoBean RevokeInvitationCommandHandler revokeInvitationCommandHandler;
  @MockitoBean FindInvitationByIdQueryHandler findInvitationByIdQueryHandler;
  @MockitoBean FindInvitationByTokenQueryHandler findInvitationByTokenQueryHandler;
  @MockitoBean ListInvitationsOfOrganizationQueryHandler listInvitationsOfOrganizationQueryHandler;
  @MockitoBean ListInvitationsForEmailQueryHandler listInvitationsForEmailQueryHandler;
  @MockitoBean FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;
  @MockitoBean AuthorizeQueryHandler authorizeQueryHandler;

  PartyId node = PartyId.generate();

  String bearer() {
    return "Bearer " + TestSecurityConfig.validToken(jwtEncoder);
  }

  InvitationResult invitation(InvitationStatus status) {
    return new InvitationResult(
        InvitationId.generate(),
        "beto@example.org",
        node,
        "Sede",
        null,
        null,
        null,
        "Beto Pérez",
        status,
        LocalDateTime.now().plusDays(3),
        "pastor@example.org",
        LocalDateTime.now());
  }

  @Test
  void invitingNeedsThePermissionOnTheNodeAndReportsTheEmail() {
    var result = invitation(InvitationStatus.PENDING);
    given(createInvitationCommandHandler.handle(any()))
        .willReturn(new CreateInvitationResult(result, true));

    assertThat(
            mvc.post()
                .uri("/v1/organizations/" + node.value() + "/invitations")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"email\":\"beto@example.org\",\"firstName\":\"Beto\",\"lastName\":\"Pérez\"}"))
        .hasStatus(201)
        .bodyJson()
        .extractingPath("$.emailSent")
        .isEqualTo(true);

    verify(authorizeQueryHandler)
        .handle(new AuthorizeQuery("test-user", node, Permission.INVITATIONS_SEND));
    verify(createInvitationCommandHandler)
        .handle(
            new CreateInvitationCommand(
                "test-user", "beto@example.org", node, null, null, "Beto", "Pérez"));
  }

  @Test
  void withoutThePermissionNothingIsCreatedOrListed() {
    given(authorizeQueryHandler.handle(any())).willThrow(new AccessDeniedException("no"));
    var uri = "/v1/organizations/" + node.value() + "/invitations";

    assertThat(
            mvc.post()
                .uri(uri)
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"beto@example.org\"}"))
        .hasStatus(403);
    assertThat(mvc.get().uri(uri).header("Authorization", bearer())).hasStatus(403);
    verifyNoInteractions(createInvitationCommandHandler, listInvitationsOfOrganizationQueryHandler);
  }

  @Test
  void aBadEmailIsRejectedBeforeAnything() {
    assertThat(
            mvc.post()
                .uri("/v1/organizations/" + node.value() + "/invitations")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nope\"}"))
        .hasStatus(400);
    verifyNoInteractions(createInvitationCommandHandler);
  }

  @Test
  void listsThePendingInvitationsOfTheNode() {
    given(listInvitationsOfOrganizationQueryHandler.handle(any()))
        .willReturn(List.of(invitation(InvitationStatus.PENDING)));

    assertThat(
            mvc.get()
                .uri("/v1/organizations/" + node.value() + "/invitations")
                .header("Authorization", bearer()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[0].status")
        .isEqualTo("PENDING");
  }

  @Test
  void revokingChecksThePermissionOnTheNodeOfTheInvitation() {
    var invitation = invitation(InvitationStatus.PENDING);
    given(findInvitationByIdQueryHandler.handle(new FindInvitationByIdQuery(invitation.id())))
        .willReturn(Optional.of(invitation));

    assertThat(
            mvc.delete()
                .uri("/v1/invitations/" + invitation.id().value())
                .header("Authorization", bearer()))
        .hasStatus(204);

    verify(authorizeQueryHandler)
        .handle(new AuthorizeQuery("test-user", node, Permission.INVITATIONS_SEND));
    verify(revokeInvitationCommandHandler).handle(new RevokeInvitationCommand(invitation.id()));
  }

  @Test
  void revokingAnUnknownInvitationIsNotFound() {
    given(findInvitationByIdQueryHandler.handle(any())).willReturn(Optional.empty());

    assertThat(
            mvc.delete()
                .uri("/v1/invitations/" + InvitationId.generate().value())
                .header("Authorization", bearer()))
        .hasStatus(404);
  }

  @Test
  void thePreviewNeedsNoToken() {
    given(findInvitationByTokenQueryHandler.handle(new FindInvitationByTokenQuery("abc")))
        .willReturn(Optional.of(invitation(InvitationStatus.PENDING)));

    var body = assertThat(mvc.get().uri("/v1/public/invitations/abc")).hasStatusOk().bodyJson();
    body.extractingPath("$.organizationName").isEqualTo("Sede");
    body.extractingPath("$.email").isEqualTo("beto@example.org");
    // Nothing else about the invitation leaks to someone who is not signed in.
    assertThat(mvc.get().uri("/v1/public/invitations/abc"))
        .bodyText()
        .doesNotContain("pastor@example.org", "organizationId");
  }

  @Test
  void anUnknownPreviewTokenIsNotFound() {
    given(findInvitationByTokenQueryHandler.handle(any())).willReturn(Optional.empty());

    assertThat(mvc.get().uri("/v1/public/invitations/nope")).hasStatus(404);
  }

  @Test
  void acceptingByTokenNeedsASession() {
    assertThat(mvc.post().uri("/v1/invitations/abc/accept")).hasStatus(401);
    verifyNoInteractions(acceptInvitationCommandHandler);
  }

  @Test
  void acceptingByTokenHandsTheInvitationToTheSignedInUser() {
    var invitation = invitation(InvitationStatus.PENDING);
    var church = PartyId.generate();
    var person = PartyId.generate();
    given(findInvitationByTokenQueryHandler.handle(new FindInvitationByTokenQuery("abc")))
        .willReturn(Optional.of(invitation));
    given(acceptInvitationCommandHandler.handle(any()))
        .willReturn(new AcceptInvitationResult(node, church, person));

    var body =
        assertThat(mvc.post().uri("/v1/invitations/abc/accept").header("Authorization", bearer()))
            .hasStatusOk()
            .bodyJson();
    body.extractingPath("$.churchId").isEqualTo(church.value().toString());
    body.extractingPath("$.personId").isEqualTo(person.value().toString());
    verify(acceptInvitationCommandHandler)
        .handle(new AcceptInvitationCommand("test-user", invitation.id()));
  }

  @Test
  void aWrongEmailOrAUsedInvitationAreForbiddenOrRejected() {
    var invitation = invitation(InvitationStatus.PENDING);
    given(findInvitationByTokenQueryHandler.handle(any())).willReturn(Optional.of(invitation));
    given(acceptInvitationCommandHandler.handle(any()))
        .willThrow(new AccessDeniedException("otro correo"))
        .willThrow(new DomainValidationException("La invitación venció"));

    assertThat(mvc.post().uri("/v1/invitations/abc/accept").header("Authorization", bearer()))
        .hasStatus(403);
    assertThat(mvc.post().uri("/v1/invitations/abc/accept").header("Authorization", bearer()))
        .hasStatus(400);
  }

  @Test
  void anUnknownTokenToAcceptIsNotFound() {
    given(findInvitationByTokenQueryHandler.handle(any())).willReturn(Optional.empty());
    given(acceptInvitationCommandHandler.handle(any())).willThrow(new NoSuchElementException());

    assertThat(mvc.post().uri("/v1/invitations/zzz/accept").header("Authorization", bearer()))
        .hasStatus(404);
  }

  @Test
  void myInvitationsAreThoseOfMyVerifiedEmail() {
    var user = new UserResult(UserId.generate(), "test-user", "beto@example.org", true);
    given(findUserBySubjectQueryHandler.handle(any())).willReturn(Optional.of(user));
    given(listInvitationsForEmailQueryHandler.handle(any()))
        .willReturn(List.of(invitation(InvitationStatus.PENDING)));

    assertThat(mvc.get().uri("/v1/me/invitations").header("Authorization", bearer()))
        .hasStatusOk()
        .bodyJson()
        .extractingPath("$[0].email")
        .isEqualTo("beto@example.org");
  }

  @Test
  void anUnverifiedEmailHasNoInvitations() {
    var user = new UserResult(UserId.generate(), "test-user", "beto@example.org", false);
    given(findUserBySubjectQueryHandler.handle(any())).willReturn(Optional.of(user));

    assertThat(mvc.get().uri("/v1/me/invitations").header("Authorization", bearer()))
        .hasStatusOk()
        .bodyText()
        .isEqualTo("[]");
    verifyNoInteractions(listInvitationsForEmailQueryHandler);
  }

  @Test
  void acceptingFromMyListUsesTheInvitationId() {
    var id = InvitationId.generate();
    given(acceptInvitationCommandHandler.handle(any()))
        .willReturn(new AcceptInvitationResult(node, PartyId.generate(), PartyId.generate()));

    assertThat(
            mvc.post()
                .uri("/v1/me/invitations/" + id.value() + "/accept")
                .header("Authorization", bearer()))
        .hasStatusOk();
    verify(acceptInvitationCommandHandler).handle(new AcceptInvitationCommand("test-user", id));
  }
}
