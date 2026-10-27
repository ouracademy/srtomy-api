package org.ouracademy.ovejafiel.invitation.command.accept_invitation;

import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.ovejafiel.accountability.domain.SystemAccountabilityTypes.VISITOR;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.command.create_accountability.CreateAccountabilityCommand;
import org.ouracademy.ovejafiel.accountability.command.create_accountability.CreateAccountabilityCommandHandler;
import org.ouracademy.ovejafiel.accountability.query.PositionResult;
import org.ouracademy.ovejafiel.accountability.query.find_organizations_of_member.FindOrganizationsOfMemberQuery;
import org.ouracademy.ovejafiel.accountability.query.find_organizations_of_member.FindOrganizationsOfMemberQueryHandler;
import org.ouracademy.ovejafiel.accountability.query.get_positions_of_person.GetPositionsOfPersonQuery;
import org.ouracademy.ovejafiel.accountability.query.get_positions_of_person.GetPositionsOfPersonQueryHandler;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.ovejafiel.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.ovejafiel.invitation.command.InvitationRules;
import org.ouracademy.ovejafiel.invitation.domain.Invitation;
import org.ouracademy.ovejafiel.invitation.repository.InvitationRepository;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.command.PersonInfo;
import org.ouracademy.ovejafiel.person.command.create_person.CreatePersonCommand;
import org.ouracademy.ovejafiel.person.command.create_person.CreatePersonCommandHandler;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.user.command.link_user_to_person.LinkUserToPersonCommand;
import org.ouracademy.ovejafiel.user.command.link_user_to_person.LinkUserToPersonCommandHandler;
import org.ouracademy.ovejafiel.user.domain.AppUser;
import org.ouracademy.ovejafiel.user.query.UserResult;
import org.ouracademy.ovejafiel.user.query.find_linked_person.FindLinkedPersonQuery;
import org.ouracademy.ovejafiel.user.query.find_linked_person.FindLinkedPersonQueryHandler;
import org.ouracademy.ovejafiel.user.query.find_linked_person.FindLinkedPersonResult;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class AcceptInvitationCommandHandlerImpl implements AcceptInvitationCommandHandler {

  private final InvitationRepository invitationRepository;
  private final FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;
  private final FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  private final FindLinkedPersonQueryHandler findLinkedPersonQueryHandler;
  private final LinkUserToPersonCommandHandler linkUserToPersonCommandHandler;
  private final CreatePersonCommandHandler createPersonCommandHandler;
  private final CreateAccountabilityCommandHandler createAccountabilityCommandHandler;
  private final FindOrganizationsOfMemberQueryHandler findOrganizationsOfMemberQueryHandler;
  private final GetPositionsOfPersonQueryHandler getPositionsOfPersonQueryHandler;
  private final AuthorizeQueryHandler authorizeQueryHandler;
  private final InvitationRules invitationRules;
  private final EntityManager entityManager;

  @Override
  @Transactional
  public AcceptInvitationResult handle(AcceptInvitationCommand command) {
    var invitation =
        invitationRepository
            .findById(command.invitationId())
            .orElseThrow(() -> new NoSuchElementException("Invitation not found"));
    var user =
        findUserBySubjectQueryHandler
            .handle(new FindUserBySubjectQuery(command.subject()))
            .orElseThrow(() -> new AccessDeniedException("Inicia sesión de nuevo"));
    if (!user.emailVerified() || !invitation.isFor(user.email())) {
      throw new AccessDeniedException(
          "Esta invitación es para "
              + invitation.getEmail()
              + ". Verifica ese correo e inicia sesión con él");
    }
    invitation.requireAcceptable(LocalDateTime.now());
    requireInviterStillAllowed(invitation);

    var organizationId = invitation.getOrganization().getId();
    var churchId =
        findChurchOfOrganizationQueryHandler
            .handle(new FindChurchOfOrganizationQuery(organizationId))
            .orElseThrow(() -> new NoSuchElementException("Organization not found"))
            .churchId();

    var personId = linkPerson(invitation, user, command, organizationId, churchId);
    joinNode(personId, organizationId);
    givePosition(invitation, personId, organizationId);

    invitation.accept(entityManager.getReference(AppUser.class, user.id()));
    return new AcceptInvitationResult(organizationId, churchId, personId);
  }

  /** What the inviter was allowed to give when inviting must still be within their reach. */
  private void requireInviterStillAllowed(Invitation invitation) {
    var inviter = invitation.getInvitedBy().getSubject();
    var organizationId = invitation.getOrganization().getId();
    var position = invitation.getPosition();
    try {
      authorizeQueryHandler.handle(
          new AuthorizeQuery(inviter, organizationId, Permission.INVITATIONS_SEND));
      if (position != null) {
        invitationRules.requireMayGive(inviter, organizationId, PositionResult.from(position));
      }
      if (invitation.getPerson() != null) {
        invitationRules.requireMayLink(
            inviter, invitationRules.accountabilitiesOf(invitation.getPerson().getId()));
      }
    } catch (AccessDeniedException e) {
      throw new DomainValidationException(
          "Quien te invitó ya no puede dar este acceso. Pide una invitación nueva");
    }
  }

  private PartyId linkPerson(
      Invitation invitation,
      UserResult user,
      AcceptInvitationCommand command,
      PartyId organizationId,
      PartyId churchId) {
    var linked =
        findLinkedPersonQueryHandler
            .handle(new FindLinkedPersonQuery(user.id(), churchId))
            .map(FindLinkedPersonResult::personId);
    var invited = invitation.getPerson() == null ? null : invitation.getPerson().getId();
    if (linked.isPresent()) {
      if (invited != null && !invited.equals(linked.get())) {
        throw new DomainValidationException(
            "Ya tienes otra ficha en esta iglesia; pide que te inviten con ella");
      }
      return linked.get();
    }
    var personId = invited != null ? invited : createPerson(invitation, command, organizationId);
    linkUserToPersonCommandHandler.handle(
        new LinkUserToPersonCommand(user.id(), personId, churchId));
    return personId;
  }

  /** The name comes from the invitation if it has one, else from whoever accepts. */
  private PartyId createPerson(
      Invitation invitation, AcceptInvitationCommand command, PartyId organizationId) {
    var firstName = firstNonBlank(invitation.getFirstName(), command.firstName());
    var lastName = firstNonBlank(invitation.getLastName(), command.lastName());
    if (firstName == null || lastName == null) {
      throw new DomainValidationException("Indica tu nombre y tu apellido para aceptar");
    }
    return createPersonCommandHandler
        .handle(
            new CreatePersonCommand(
                invitation.getInvitedBy().getSubject(),
                new PersonInfo(firstName, lastName, null, invitation.getEmail(), null, null, null),
                organizationId,
                Set.of()))
        .id();
  }

  private static String firstNonBlank(String preferred, String fallback) {
    if (preferred != null && !preferred.isBlank()) {
      return preferred.trim();
    }
    return fallback == null || fallback.isBlank() ? null : fallback.trim();
  }

  /** A person who already had a record elsewhere in the church also belongs to the node. */
  private void joinNode(PartyId personId, PartyId organizationId) {
    var nodes =
        findOrganizationsOfMemberQueryHandler
            .handle(new FindOrganizationsOfMemberQuery(personId))
            .organizationIds();
    if (!nodes.contains(organizationId)) {
      createAccountabilityCommandHandler.handle(
          new CreateAccountabilityCommand(personId, organizationId, VISITOR));
      createAccountabilityCommandHandler.handle(
          new CreateAccountabilityCommand(personId, organizationId, IS_PART_OF));
    }
  }

  private void givePosition(Invitation invitation, PartyId personId, PartyId organizationId) {
    if (invitation.getPosition() == null) {
      return;
    }
    var positionId = invitation.getPosition().getId();
    var held =
        getPositionsOfPersonQueryHandler
            .handle(new GetPositionsOfPersonQuery(organizationId, personId))
            .stream()
            .map(PositionResult::id)
            .anyMatch(positionId::equals);
    if (!held) {
      createAccountabilityCommandHandler.handle(
          new CreateAccountabilityCommand(personId, organizationId, positionId));
    }
  }
}
