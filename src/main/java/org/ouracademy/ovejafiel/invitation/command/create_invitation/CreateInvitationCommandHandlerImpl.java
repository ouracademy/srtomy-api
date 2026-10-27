package org.ouracademy.ovejafiel.invitation.command.create_invitation;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.query.PositionResult;
import org.ouracademy.ovejafiel.accountability.query.find_position_by_id.FindPositionByIdQuery;
import org.ouracademy.ovejafiel.accountability.query.find_position_by_id.FindPositionByIdQueryHandler;
import org.ouracademy.ovejafiel.invitation.command.InvitationRules;
import org.ouracademy.ovejafiel.invitation.domain.Invitation;
import org.ouracademy.ovejafiel.invitation.domain.InvitationStatus;
import org.ouracademy.ovejafiel.invitation.domain.InvitationTokens;
import org.ouracademy.ovejafiel.invitation.query.InvitationResult;
import org.ouracademy.ovejafiel.invitation.repository.InvitationRepository;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailCommand;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailCommandHandler;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.organization.query.FindOrganizationResult;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQuery;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.ovejafiel.outbox.command.cancel_message.CancelMessageCommand;
import org.ouracademy.ovejafiel.outbox.command.cancel_message.CancelMessageCommandHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.command.fill_person_email.FillPersonEmailCommand;
import org.ouracademy.ovejafiel.person.command.fill_person_email.FillPersonEmailCommandHandler;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.person.query.find_people_by_email.FindPeopleByEmailQuery;
import org.ouracademy.ovejafiel.person.query.find_people_by_email.FindPeopleByEmailQueryHandler;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.user.domain.AppUser;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQuery;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.ouracademy.ovejafiel.user.query.is_person_linked.IsPersonLinkedQuery;
import org.ouracademy.ovejafiel.user.query.is_person_linked.IsPersonLinkedQueryHandler;
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
  private final InvitationRules invitationRules;
  private final IsPersonLinkedQueryHandler isPersonLinkedQueryHandler;
  private final FindPeopleByEmailQueryHandler findPeopleByEmailQueryHandler;
  private final FillPersonEmailCommandHandler fillPersonEmailCommandHandler;
  private final SendEmailCommandHandler sendEmailCommandHandler;
  private final CancelMessageCommandHandler cancelMessageCommandHandler;
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
    var personId = resolvePerson(command, email);

    // Inviting again replaces the pending invitation, so the old link stops working.
    var tokenOfOld =
        invitationRepository.findByOrganizationIdAndStatusOrderByCreateDateDesc(
            command.organizationId(), InvitationStatus.PENDING);
    tokenOfOld.stream().filter(old -> old.isFor(email)).forEach(this::replace);

    var token = InvitationTokens.newToken();
    var invitation =
        invitationRepository.save(
            new Invitation(
                email,
                entityManager.getReference(Organization.class, command.organizationId()),
                position == null
                    ? null
                    : entityManager.getReference(AccountabilityType.class, position.id()),
                personId == null ? null : entityManager.getReference(Person.class, personId),
                personId == null ? blankToNull(command.firstName()) : null,
                personId == null ? blankToNull(command.lastName()) : null,
                InvitationTokens.hash(token),
                LocalDateTime.now().plusDays(VALID_DAYS),
                entityManager.getReference(AppUser.class, inviter.id())));

    var churchName =
        findOrganizationByIdQueryHandler
            .handle(new FindOrganizationByIdQuery(churchId))
            .map(FindOrganizationResult::name)
            .orElse("");
    var queued =
        sendEmailCommandHandler
            .handle(
                new SendEmailCommand(
                    "INVITATION",
                    email,
                    "Te invitaron a " + organization.name() + " en Sr. Tommy",
                    body(
                        inviter.email(),
                        organization.name(),
                        churchName,
                        position,
                        invitation,
                        token)))
            .messageId();
    invitation.emailQueued(queued);
    return new CreateInvitationResult(
        InvitationResult.from(invitation, LocalDateTime.now()), queued);
  }

  /** Revokes the invitation and withdraws its email if it has not left: its link is dead now. */
  private void replace(Invitation old) {
    old.revoke();
    if (old.getEmailMessageId() != null) {
      cancelMessageCommandHandler.handle(new CancelMessageCommand(old.getEmailMessageId()));
    }
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
    invitationRules.requireMayGive(command.subject(), command.organizationId(), position);
    return position;
  }

  /**
   * The person the invitation links: the one named, or else the only one of the node with that
   * email, or none (the person is then created when the invitation is accepted). Linking an account
   * to a person hands over the access the person already has, so the caller must be able to give
   * all of it. A person named without an email gets the invited one.
   */
  private PartyId resolvePerson(CreateInvitationCommand command, String email) {
    var node = command.organizationId();
    var named = command.personId() != null;
    PartyId personId = command.personId();
    if (!named) {
      var matches = findPeopleByEmailQueryHandler.handle(new FindPeopleByEmailQuery(email, node));
      if (matches.personIds().size() > 1) {
        throw new DomainValidationException(
            "Hay varias personas con ese correo; elige a quién invitar");
      }
      personId = matches.personIds().isEmpty() ? null : matches.personIds().get(0);
    }
    if (personId == null) {
      return null;
    }
    var accountabilities = invitationRules.accountabilitiesOf(personId);
    if (accountabilities.stream().noneMatch(a -> a.organizationId().equals(node))) {
      throw new DomainValidationException("La persona no pertenece a este grupo");
    }
    if (isPersonLinkedQueryHandler.handle(new IsPersonLinkedQuery(personId)).linked()) {
      throw new DomainValidationException("Esa persona ya usa la aplicación");
    }
    // Linking an account to the person hands over the access the person already has.
    invitationRules.requireMayLink(command.subject(), accountabilities);
    if (named) {
      fillPersonEmailCommandHandler.handle(new FillPersonEmailCommand(personId, email));
    }
    return personId;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
