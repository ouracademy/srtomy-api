package org.ouracademy.srtomy.invitation.command.create_invitation;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.query.PositionResult;
import org.ouracademy.srtomy.accountability.query.find_organizations_of_member.FindOrganizationsOfMemberQuery;
import org.ouracademy.srtomy.accountability.query.find_organizations_of_member.FindOrganizationsOfMemberQueryHandler;
import org.ouracademy.srtomy.accountability.query.find_position_by_id.FindPositionByIdQuery;
import org.ouracademy.srtomy.accountability.query.find_position_by_id.FindPositionByIdQueryHandler;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQuery;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQueryHandler;
import org.ouracademy.srtomy.invitation.domain.Invitation;
import org.ouracademy.srtomy.invitation.domain.InvitationStatus;
import org.ouracademy.srtomy.invitation.domain.InvitationTokens;
import org.ouracademy.srtomy.invitation.query.InvitationResult;
import org.ouracademy.srtomy.invitation.repository.InvitationRepository;
import org.ouracademy.srtomy.notification.command.send_email.SendEmailCommand;
import org.ouracademy.srtomy.notification.command.send_email.SendEmailCommandHandler;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.organization.query.find_organization_by_id.FindOrganizationByIdQuery;
import org.ouracademy.srtomy.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.domain.Person;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;
import org.ouracademy.srtomy.user.domain.AppUser;
import org.ouracademy.srtomy.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.srtomy.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.ouracademy.srtomy.user.query.is_person_linked.IsPersonLinkedQuery;
import org.ouracademy.srtomy.user.query.is_person_linked.IsPersonLinkedQueryHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class CreateInvitationCommandHandlerImpl implements CreateInvitationCommandHandler {

  static final int VALID_DAYS = 14;
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private final InvitationRepository invitationRepository;
  private final FindUserBySubjectQueryHandler findUserBySubjectQueryHandler;
  private final FindOrganizationByIdQueryHandler findOrganizationByIdQueryHandler;
  private final FindChurchOfOrganizationQueryHandler findChurchOfOrganizationQueryHandler;
  private final FindPositionByIdQueryHandler findPositionByIdQueryHandler;
  private final FindOrganizationsOfMemberQueryHandler findOrganizationsOfMemberQueryHandler;
  private final AuthorizeAccessGrantQueryHandler authorizeAccessGrantQueryHandler;
  private final IsPersonLinkedQueryHandler isPersonLinkedQueryHandler;
  private final SendEmailCommandHandler sendEmailCommandHandler;
  private final EntityManager entityManager;

  @Value("${app.url:http://localhost:3000}")
  private String appUrl;

  @Override
  @Transactional
  public CreateInvitationResult handle(CreateInvitationCommand command) {
    var inviter =
        findUserBySubjectQueryHandler
            .handle(new FindUserBySubjectQuery(command.subject()))
            .orElseThrow(() -> new AccessDeniedException("Inicia sesión de nuevo"));
    var organization = organizationOf(command.organizationId());
    var churchId = churchOf(command.organizationId());
    var email = normalizedEmail(command.email());

    var position = command.positionId() == null ? null : positionOf(command, churchId);
    requireValidPerson(command);

    // Inviting again replaces the pending invitation, so the old link stops working.
    var tokenOfOld =
        invitationRepository.findByOrganizationIdAndStatusOrderByCreateDateDesc(
            command.organizationId(), InvitationStatus.PENDING);
    tokenOfOld.stream().filter(old -> old.isFor(email)).forEach(Invitation::revoke);

    var token = InvitationTokens.newToken();
    var invitation =
        invitationRepository.save(
            new Invitation(
                email,
                entityManager.getReference(Organization.class, command.organizationId()),
                position == null
                    ? null
                    : entityManager.getReference(AccountabilityType.class, position.id()),
                command.personId() == null
                    ? null
                    : entityManager.getReference(Person.class, command.personId()),
                command.personId() == null ? command.firstName().trim() : null,
                command.personId() == null ? command.lastName().trim() : null,
                InvitationTokens.hash(token),
                LocalDateTime.now().plusDays(VALID_DAYS),
                entityManager.getReference(AppUser.class, inviter.id())));

    var churchName =
        findOrganizationByIdQueryHandler
            .handle(new FindOrganizationByIdQuery(churchId))
            .map(FindOrganizationResult::name)
            .orElse("");
    var sent =
        sendEmailCommandHandler
            .handle(
                new SendEmailCommand(
                    email,
                    "Te invitaron a " + organization.name() + " en Sr. Tommy",
                    body(
                        inviter.email(),
                        organization.name(),
                        churchName,
                        position,
                        invitation,
                        token)))
            .delivered();
    return new CreateInvitationResult(InvitationResult.from(invitation, LocalDateTime.now()), sent);
  }

  private String body(
      String inviterEmail,
      String organizationName,
      String churchName,
      PositionResult position,
      Invitation invitation,
      String token) {
    var who = inviterEmail == null ? "Alguien" : inviterEmail;
    var where =
        organizationName.equals(churchName)
            ? organizationName
            : organizationName + " (" + churchName + ")";
    var as = position == null ? "" : ", como " + position.name();
    return "Hola,\n\n"
        + who
        + " te invitó a usar Sr. Tommy en "
        + where
        + as
        + ".\n\nAcepta la invitación aquí (vence el "
        + invitation.getExpiresAt().format(DATE)
        + "):\n"
        + appUrl.replaceAll("/+$", "")
        + "/invitacion/"
        + token
        + "\n\nInicia sesión o crea tu cuenta con este mismo correo: "
        + invitation.getEmail()
        + ".\n";
  }

  private FindOrganizationResult organizationOf(PartyId organizationId) {
    return findOrganizationByIdQueryHandler
        .handle(new FindOrganizationByIdQuery(organizationId))
        .orElseThrow(() -> new NoSuchElementException("Organization not found"));
  }

  private PartyId churchOf(PartyId organizationId) {
    return findChurchOfOrganizationQueryHandler
        .handle(new FindChurchOfOrganizationQuery(organizationId))
        .orElseThrow(() -> new NoSuchElementException("Organization not found"))
        .churchId();
  }

  private static String normalizedEmail(String email) {
    if (email == null || !email.contains("@")) {
      throw new DomainValidationException("El correo no es válido");
    }
    return email.trim().toLowerCase();
  }

  /** The position must be one of the church; one that gives access is checked like a grant. */
  private PositionResult positionOf(CreateInvitationCommand command, PartyId churchId) {
    var position =
        findPositionByIdQueryHandler
            .handle(new FindPositionByIdQuery(command.positionId()))
            .orElseThrow(() -> new NoSuchElementException("Position not found"));
    if (!position.churchId().equals(churchId)) {
      throw new DomainValidationException("Ese cargo no pertenece a esta iglesia");
    }
    if (!position.roleIds().isEmpty() || !position.permissions().isEmpty()) {
      authorizeAccessGrantQueryHandler.handle(
          new AuthorizeAccessGrantQuery(
              command.subject(),
              position.roleIds(),
              position.permissions(),
              command.organizationId(),
              Permission.INVITATIONS_SEND));
    }
    return position;
  }

  private void requireValidPerson(CreateInvitationCommand command) {
    if (command.personId() == null) {
      if (isBlank(command.firstName()) || isBlank(command.lastName())) {
        throw new DomainValidationException("Indica el nombre y el apellido de la persona");
      }
      return;
    }
    var nodes =
        findOrganizationsOfMemberQueryHandler
            .handle(new FindOrganizationsOfMemberQuery(command.personId()))
            .organizationIds();
    if (!nodes.contains(command.organizationId())) {
      throw new DomainValidationException("La persona no pertenece a este grupo");
    }
    if (isPersonLinkedQueryHandler.handle(new IsPersonLinkedQuery(command.personId())).linked()) {
      throw new DomainValidationException("Esa persona ya usa la aplicación");
    }
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
