package org.ouracademy.srtomy.invitation.command.create_invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.query.PositionResult;
import org.ouracademy.srtomy.accountability.query.find_position_by_id.FindPositionByIdQueryHandler;
import org.ouracademy.srtomy.accountability.query.get_person_accountabilities.GetPersonAccountabilitiesQueryHandler;
import org.ouracademy.srtomy.accountability.query.get_person_accountabilities.PersonAccountabilityResult;
import org.ouracademy.srtomy.iam.domain.IamRoleId;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQuery;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQueryHandler;
import org.ouracademy.srtomy.invitation.InvitationFixtures;
import org.ouracademy.srtomy.invitation.command.InvitationRules;
import org.ouracademy.srtomy.invitation.domain.Invitation;
import org.ouracademy.srtomy.invitation.domain.InvitationStatus;
import org.ouracademy.srtomy.invitation.domain.InvitationTokens;
import org.ouracademy.srtomy.invitation.repository.InvitationRepository;
import org.ouracademy.srtomy.notification.command.send_email.SendEmailCommand;
import org.ouracademy.srtomy.notification.command.send_email.SendEmailCommandHandler;
import org.ouracademy.srtomy.notification.command.send_email.SendEmailResult;
import org.ouracademy.srtomy.organization.domain.MemberVisibility;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.FindOrganizationResult;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.srtomy.organization.query.find_organization_by_id.FindOrganizationByIdQuery;
import org.ouracademy.srtomy.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.domain.Person;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;
import org.ouracademy.srtomy.user.domain.UserId;
import org.ouracademy.srtomy.user.query.UserResult;
import org.ouracademy.srtomy.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.ouracademy.srtomy.user.query.is_person_linked.IsPersonLinkedQueryHandler;
import org.ouracademy.srtomy.user.query.is_person_linked.IsPersonLinkedResult;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

class CreateInvitationCommandHandlerImplTests {

  InvitationRepository invitationRepository = mock(InvitationRepository.class);
  FindUserBySubjectQueryHandler findUser = mock(FindUserBySubjectQueryHandler.class);
  FindOrganizationByIdQueryHandler findOrganization = mock(FindOrganizationByIdQueryHandler.class);
  FindChurchOfOrganizationQueryHandler findChurch =
      mock(FindChurchOfOrganizationQueryHandler.class);
  FindPositionByIdQueryHandler findPosition = mock(FindPositionByIdQueryHandler.class);
  GetPersonAccountabilitiesQueryHandler getPersonAccountabilities =
      mock(GetPersonAccountabilitiesQueryHandler.class);
  AuthorizeAccessGrantQueryHandler authorizeAccessGrant =
      mock(AuthorizeAccessGrantQueryHandler.class);
  IsPersonLinkedQueryHandler isPersonLinked = mock(IsPersonLinkedQueryHandler.class);
  SendEmailCommandHandler sendEmail = mock(SendEmailCommandHandler.class);
  EntityManager entityManager = mock(EntityManager.class);
  CreateInvitationCommandHandlerImpl handler =
      new CreateInvitationCommandHandlerImpl(
          invitationRepository,
          findUser,
          findOrganization,
          findChurch,
          findPosition,
          new InvitationRules(authorizeAccessGrant, getPersonAccountabilities),
          isPersonLinked,
          sendEmail,
          entityManager);

  PartyId church = PartyId.generate();
  PartyId node = PartyId.generate();
  Organization nodeEntity = InvitationFixtures.organization(node, "Sede Norte");

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(handler, "appUrl", "https://app.example.org/");
    when(findUser.handle(any()))
        .thenReturn(
            Optional.of(new UserResult(UserId.generate(), "auth0|me", "pastor@example.org", true)));
    when(findOrganization.handle(new FindOrganizationByIdQuery(node)))
        .thenReturn(Optional.of(organization(node, "Sede Norte")));
    when(findOrganization.handle(new FindOrganizationByIdQuery(church)))
        .thenReturn(Optional.of(organization(church, "Iglesia Central")));
    when(findChurch.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    when(entityManager.getReference(eq(Organization.class), any())).thenReturn(nodeEntity);
    when(entityManager.getReference(eq(org.ouracademy.srtomy.user.domain.AppUser.class), any()))
        .thenReturn(InvitationFixtures.user("auth0|me", "pastor@example.org"));
    when(invitationRepository.save(any(Invitation.class))).thenAnswer(call -> call.getArgument(0));
    when(invitationRepository.findByOrganizationIdAndStatusOrderByCreateDateDesc(any(), any()))
        .thenReturn(List.of());
    when(sendEmail.handle(any())).thenReturn(new SendEmailResult(true));
  }

  static FindOrganizationResult organization(PartyId id, String name) {
    return new FindOrganizationResult(
        id,
        name,
        null,
        null,
        null,
        Organization.Type.LOCATION,
        null,
        null,
        null,
        MemberVisibility.PUBLIC,
        true,
        "slug",
        null,
        null);
  }

  CreateInvitationCommand newPerson(String email, AccountabilityTypeId positionId) {
    return new CreateInvitationCommand("auth0|me", email, node, positionId, null, "Beto", "Pérez");
  }

  PositionResult position(PartyId owner, Set<IamRoleId> roles, Set<String> permissions) {
    var position =
        new PositionResult(AccountabilityTypeId.generate(), "Ujier", owner, roles, permissions);
    when(findPosition.handle(any())).thenReturn(Optional.of(position));
    when(entityManager.getReference(eq(AccountabilityType.class), eq(position.id())))
        .thenReturn(mock(AccountabilityType.class));
    return position;
  }

  @Test
  void createsAPendingInvitationAndEmailsTheLink() {
    var result = handler.handle(newPerson(" Beto@Example.org ", null));

    var saved = ArgumentCaptor.forClass(Invitation.class);
    verify(invitationRepository).save(saved.capture());
    assertThat(saved.getValue().getEmail()).isEqualTo("beto@example.org");
    assertThat(saved.getValue().getFirstName()).isEqualTo("Beto");
    assertThat(saved.getValue().getExpiresAt())
        .isBetween(LocalDateTime.now().plusDays(13), LocalDateTime.now().plusDays(15));
    assertThat(result.emailSent()).isTrue();
    assertThat(result.invitation().status()).isEqualTo(InvitationStatus.PENDING);

    var email = ArgumentCaptor.forClass(SendEmailCommand.class);
    verify(sendEmail).handle(email.capture());
    assertThat(email.getValue().to()).isEqualTo("beto@example.org");
    assertThat(email.getValue().subject()).contains("Sede Norte");
    assertThat(email.getValue().text())
        .contains("pastor@example.org", "Sede Norte (Iglesia Central)")
        .containsPattern("https://app\\.example\\.org/invitacion/[A-Za-z0-9_-]{43}");
    // Only the hash is stored.
    var token = email.getValue().text().replaceAll("(?s).*/invitacion/([A-Za-z0-9_-]{43}).*", "$1");
    assertThat(saved.getValue().getTokenHash()).isEqualTo(InvitationTokens.hash(token));
  }

  @Test
  void aFailedEmailKeepsTheInvitation() {
    when(sendEmail.handle(any())).thenReturn(new SendEmailResult(false));

    var result = handler.handle(newPerson("beto@example.org", null));

    assertThat(result.emailSent()).isFalse();
    verify(invitationRepository).save(any(Invitation.class));
  }

  @Test
  void invitingAgainRevokesThePendingInvitationOfThatEmailOnly() {
    var same = mock(Invitation.class);
    var other = mock(Invitation.class);
    when(same.isFor("beto@example.org")).thenReturn(true);
    when(other.isFor("beto@example.org")).thenReturn(false);
    when(invitationRepository.findByOrganizationIdAndStatusOrderByCreateDateDesc(
            node, InvitationStatus.PENDING))
        .thenReturn(List.of(same, other));

    handler.handle(newPerson("beto@example.org", null));

    verify(same).revoke();
    verify(other, never()).revoke();
  }

  @Test
  void aPositionWithAccessIsCheckedAgainstTheInvitationPermission() {
    var position = position(church, Set.of(PredefinedRoles.COLLABORATOR), Set.of("events.read"));

    handler.handle(newPerson("beto@example.org", position.id()));

    verify(authorizeAccessGrant)
        .handle(
            new AuthorizeAccessGrantQuery(
                "auth0|me",
                Set.of(PredefinedRoles.COLLABORATOR),
                Set.of("events.read"),
                node,
                Permission.INVITATIONS_SEND));
    var email = ArgumentCaptor.forClass(SendEmailCommand.class);
    verify(sendEmail).handle(email.capture());
    assertThat(email.getValue().text()).contains(", como Ujier");
  }

  @Test
  void aLabelPositionNeedsNoAccessCheck() {
    var position = position(church, Set.of(), Set.of());

    handler.handle(newPerson("beto@example.org", position.id()));

    verifyNoInteractions(authorizeAccessGrant);
  }

  @Test
  void aCallerWhoCannotGiveThePositionIsDenied() {
    var position = position(church, Set.of(PredefinedRoles.ADMIN), Set.of());
    when(authorizeAccessGrant.handle(any())).thenThrow(new AccessDeniedException("no"));

    assertThatThrownBy(() -> handler.handle(newPerson("beto@example.org", position.id())))
        .isInstanceOf(AccessDeniedException.class);
    verify(invitationRepository, never()).save(any());
    verifyNoInteractions(sendEmail);
  }

  @Test
  void aPositionOfAnotherChurchIsRejected() {
    var position = position(PartyId.generate(), Set.of(), Set.of());

    assertThatThrownBy(() -> handler.handle(newPerson("beto@example.org", position.id())))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("iglesia");
    verify(invitationRepository, never()).save(any());
  }

  @Test
  void aNewPersonNeedsAName() {
    var command =
        new CreateInvitationCommand("auth0|me", "beto@example.org", node, null, null, " ", "Pérez");

    assertThatThrownBy(() -> handler.handle(command))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("nombre");
  }

  @Test
  void theEmailMustLookLikeOne() {
    assertThatThrownBy(() -> handler.handle(newPerson("beto", null)))
        .isInstanceOf(DomainValidationException.class);
    assertThatThrownBy(() -> handler.handle(newPerson(null, null)))
        .isInstanceOf(DomainValidationException.class);
  }

  @Test
  void anExistingPersonMustBelongToTheNodeAndHaveNoAccount() {
    var personId = PartyId.generate();
    var command =
        new CreateInvitationCommand(
            "auth0|me", "beto@example.org", node, null, personId, null, null);
    when(entityManager.getReference(eq(Person.class), eq(personId))).thenReturn(new Person());

    when(getPersonAccountabilities.handle(any()))
        .thenReturn(List.of(held(PartyId.generate(), null)));
    assertThatThrownBy(() -> handler.handle(command))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("no pertenece");

    when(getPersonAccountabilities.handle(any())).thenReturn(List.of(held(node, null)));
    when(isPersonLinked.handle(any())).thenReturn(new IsPersonLinkedResult(true));
    assertThatThrownBy(() -> handler.handle(command))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("ya usa");

    when(isPersonLinked.handle(any())).thenReturn(new IsPersonLinkedResult(false));
    var result = handler.handle(command);
    assertThat(result.invitation().status()).isEqualTo(InvitationStatus.PENDING);
  }

  @Test
  void linkingAPersonWhoAlreadyHoldsMoreAccessThanTheCallerIsDenied() {
    var personId = PartyId.generate();
    var command =
        new CreateInvitationCommand(
            "auth0|me", "beto@example.org", node, null, personId, null, null);
    var pastor =
        new PositionResult(
            AccountabilityTypeId.generate(),
            "Pastor",
            church,
            Set.of(PredefinedRoles.ADMIN),
            Set.of());
    when(getPersonAccountabilities.handle(any()))
        .thenReturn(List.of(held(node, null), held(church, pastor)));
    when(isPersonLinked.handle(any())).thenReturn(new IsPersonLinkedResult(false));
    when(authorizeAccessGrant.handle(any())).thenThrow(new AccessDeniedException("no"));

    assertThatThrownBy(() -> handler.handle(command)).isInstanceOf(AccessDeniedException.class);
    verify(authorizeAccessGrant)
        .handle(
            new AuthorizeAccessGrantQuery(
                "auth0|me",
                Set.of(PredefinedRoles.ADMIN),
                Set.of(),
                church,
                Permission.INVITATIONS_SEND));
    verify(invitationRepository, never()).save(any());
  }

  PersonAccountabilityResult held(PartyId organizationId, PositionResult position) {
    return new PersonAccountabilityResult(
        organizationId, AccountabilityTypeId.generate(), position);
  }

  @Test
  void anUnknownCallerMustSignInAgain() {
    when(findUser.handle(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(newPerson("beto@example.org", null)))
        .isInstanceOf(AccessDeniedException.class);
  }
}
