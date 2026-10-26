package org.ouracademy.srtomy.invitation.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.srtomy.invitation.command.accept_invitation.AcceptInvitationCommand;
import org.ouracademy.srtomy.invitation.command.accept_invitation.AcceptInvitationCommandHandler;
import org.ouracademy.srtomy.invitation.command.create_invitation.CreateInvitationCommand;
import org.ouracademy.srtomy.invitation.command.create_invitation.CreateInvitationCommandHandler;
import org.ouracademy.srtomy.invitation.command.revoke_invitation.RevokeInvitationCommand;
import org.ouracademy.srtomy.invitation.command.revoke_invitation.RevokeInvitationCommandHandler;
import org.ouracademy.srtomy.invitation.controller.dto.AcceptInvitationRequest;
import org.ouracademy.srtomy.invitation.controller.dto.AcceptInvitationResponse;
import org.ouracademy.srtomy.invitation.controller.dto.CreateInvitationRequest;
import org.ouracademy.srtomy.invitation.controller.dto.CreateInvitationResponse;
import org.ouracademy.srtomy.invitation.controller.dto.InvitationPreviewResponse;
import org.ouracademy.srtomy.invitation.controller.dto.InvitationResponse;
import org.ouracademy.srtomy.invitation.domain.InvitationId;
import org.ouracademy.srtomy.invitation.query.InvitationResult;
import org.ouracademy.srtomy.invitation.query.find_invitation_by_id.FindInvitationByIdQuery;
import org.ouracademy.srtomy.invitation.query.find_invitation_by_id.FindInvitationByIdQueryHandler;
import org.ouracademy.srtomy.invitation.query.find_invitation_by_token.FindInvitationByTokenQuery;
import org.ouracademy.srtomy.invitation.query.find_invitation_by_token.FindInvitationByTokenQueryHandler;
import org.ouracademy.srtomy.invitation.query.list_invitations_for_email.ListInvitationsForEmailQuery;
import org.ouracademy.srtomy.invitation.query.list_invitations_for_email.ListInvitationsForEmailQueryHandler;
import org.ouracademy.srtomy.invitation.query.list_invitations_of_organization.ListInvitationsOfOrganizationQuery;
import org.ouracademy.srtomy.invitation.query.list_invitations_of_organization.ListInvitationsOfOrganizationQueryHandler;
import org.ouracademy.srtomy.outbox.domain.OutboxStatus;
import org.ouracademy.srtomy.outbox.query.find_outbox_statuses.FindOutboxStatusesQuery;
import org.ouracademy.srtomy.outbox.query.find_outbox_statuses.FindOutboxStatusesQueryHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.srtomy.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Invitations to use the app. Sending, listing and revoking need {@code invitations.send} on the
 * node; accepting needs only a signed-in user whose verified email is the invited one. The preview
 * under {@code public/} needs no sign-in: the token in the email link is the credential.
 */
@RestController
@AllArgsConstructor
@Tag(name = "invitations")
public class InvitationController {

  CreateInvitationCommandHandler createInvitationCommandHandler;
  AcceptInvitationCommandHandler acceptInvitationCommandHandler;
  RevokeInvitationCommandHandler revokeInvitationCommandHandler;
  FindInvitationByIdQueryHandler findInvitationByIdQueryHandler;
  FindInvitationByTokenQueryHandler findInvitationByTokenQueryHandler;
  ListInvitationsOfOrganizationQueryHandler listInvitationsOfOrganizationQueryHandler;
  ListInvitationsForEmailQueryHandler listInvitationsForEmailQueryHandler;
  FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;
  FindOutboxStatusesQueryHandler findOutboxStatusesQueryHandler;
  AuthorizeQueryHandler authorizeQueryHandler;

  /** Invites someone to the node; inviting the same email again resends the email. */
  @PostMapping("organizations/{id}/invitations")
  @ResponseStatus(HttpStatus.CREATED)
  public CreateInvitationResponse create(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody @Valid CreateInvitationRequest request) {
    var organizationId = new PartyId(id);
    authorize(jwt, organizationId);
    var result =
        createInvitationCommandHandler.handle(
            new CreateInvitationCommand(
                jwt.getSubject(),
                request.email(),
                organizationId,
                request.positionId() == null
                    ? null
                    : new AccountabilityTypeId(request.positionId()),
                request.personId() == null ? null : new PartyId(request.personId()),
                request.firstName(),
                request.lastName()));
    // The attempt right after the commit has already run: report how it went.
    var emailStatus =
        findOutboxStatusesQueryHandler
            .handle(new FindOutboxStatusesQuery(List.of(result.emailMessageId())))
            .of(result.emailMessageId());
    return new CreateInvitationResponse(
        InvitationResponse.from(result.invitation().withEmailStatus(emailStatus)),
        emailStatus == OutboxStatus.SENT);
  }

  /** The pending invitations of the node (the expired ones are flagged by their status). */
  @GetMapping("organizations/{id}/invitations")
  public List<InvitationResponse> ofOrganization(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    var organizationId = new PartyId(id);
    authorize(jwt, organizationId);
    return listInvitationsOfOrganizationQueryHandler
        .handle(new ListInvitationsOfOrganizationQuery(organizationId))
        .stream()
        .map(InvitationResponse::from)
        .toList();
  }

  @DeleteMapping("invitations/{id}")
  public ResponseEntity<Void> revoke(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
    var invitation = find(new InvitationId(id));
    authorize(jwt, invitation.organizationId());
    revokeInvitationCommandHandler.handle(new RevokeInvitationCommand(invitation.id()));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("public/invitations/{token}")
  public InvitationPreviewResponse preview(@PathVariable String token) {
    return findInvitationByTokenQueryHandler
        .handle(new FindInvitationByTokenQuery(token))
        .map(InvitationPreviewResponse::from)
        .orElseThrow(InvitationController::notFound);
  }

  @PostMapping("invitations/{token}/accept")
  public AcceptInvitationResponse accept(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable String token,
      @RequestBody(required = false) @Valid AcceptInvitationRequest request) {
    var invitation =
        findInvitationByTokenQueryHandler
            .handle(new FindInvitationByTokenQuery(token))
            .orElseThrow(InvitationController::notFound);
    return accept(jwt, invitation.id(), request);
  }

  /** The invitations waiting for the signed-in user's verified email. */
  @GetMapping("me/invitations")
  public List<InvitationResponse> mine(@AuthenticationPrincipal Jwt jwt) {
    return findUserBySubjectQueryHandler
        .handle(new FindUserBySubjectQuery(jwt.getSubject()))
        .filter(user -> user.emailVerified())
        .map(
            user ->
                listInvitationsForEmailQueryHandler
                    .handle(new ListInvitationsForEmailQuery(user.email()))
                    .stream()
                    .map(InvitationResponse::from)
                    .toList())
        .orElse(List.of());
  }

  @PostMapping("me/invitations/{id}/accept")
  public AcceptInvitationResponse acceptMine(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID id,
      @RequestBody(required = false) @Valid AcceptInvitationRequest request) {
    return accept(jwt, new InvitationId(id), request);
  }

  private AcceptInvitationResponse accept(
      Jwt jwt, InvitationId id, AcceptInvitationRequest request) {
    return AcceptInvitationResponse.from(
        acceptInvitationCommandHandler.handle(
            new AcceptInvitationCommand(
                jwt.getSubject(),
                id,
                request == null ? null : request.firstName(),
                request == null ? null : request.lastName())));
  }

  private static NoSuchElementException notFound() {
    return new NoSuchElementException("Invitation not found");
  }

  private InvitationResult find(InvitationId id) {
    return findInvitationByIdQueryHandler
        .handle(new FindInvitationByIdQuery(id))
        .orElseThrow(InvitationController::notFound);
  }

  private void authorize(Jwt jwt, PartyId organizationId) {
    authorizeQueryHandler.handle(
        new AuthorizeQuery(jwt.getSubject(), organizationId, Permission.INVITATIONS_SEND));
  }
}
