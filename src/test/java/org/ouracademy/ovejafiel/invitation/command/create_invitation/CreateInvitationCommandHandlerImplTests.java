package org.ouracademy.ovejafiel.invitation.command.create_invitation;

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
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityType;
import org.ouracademy.ovejafiel.accountability.domain.AccountabilityTypeId;
import org.ouracademy.ovejafiel.accountability.query.PositionResult;
import org.ouracademy.ovejafiel.accountability.query.find_position_by_id.FindPositionByIdQueryHandler;
import org.ouracademy.ovejafiel.accountability.query.get_person_accountabilities.GetPersonAccountabilitiesQueryHandler;
import org.ouracademy.ovejafiel.accountability.query.get_person_accountabilities.PersonAccountabilityResult;
import org.ouracademy.ovejafiel.iam.domain.IamRoleId;
import org.ouracademy.ovejafiel.iam.domain.Permission;
import org.ouracademy.ovejafiel.iam.domain.PredefinedRoles;
import org.ouracademy.ovejafiel.iam.query.authorize_access_grant.AuthorizeAccessGrantQuery;
import org.ouracademy.ovejafiel.iam.query.authorize_access_grant.AuthorizeAccessGrantQueryHandler;
import org.ouracademy.ovejafiel.invitation.InvitationFixtures;
import org.ouracademy.ovejafiel.invitation.command.InvitationRules;
import org.ouracademy.ovejafiel.invitation.domain.Invitation;
import org.ouracademy.ovejafiel.invitation.domain.InvitationStatus;
import org.ouracademy.ovejafiel.invitation.domain.InvitationTokens;
import org.ouracademy.ovejafiel.invitation.repository.InvitationRepository;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailCommand;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailCommandHandler;
import org.ouracademy.ovejafiel.notification.command.send_email.SendEmailResult;
import org.ouracademy.ovejafiel.organization.domain.MemberVisibility;
import org.ouracademy.ovejafiel.organization.domain.Organization;
import org.ouracademy.ovejafiel.organization.query.FindOrganizationResult;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.ovejafiel.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQuery;
import org.ouracademy.ovejafiel.organization.query.find_organization_by_id.FindOrganizationByIdQueryHandler;
import org.ouracademy.ovejafiel.party.domain.PartyId;
import org.ouracademy.ovejafiel.person.command.fill_person_email.FillPersonEmailCommand;
import org.ouracademy.ovejafiel.person.command.fill_person_email.FillPersonEmailCommandHandler;
import org.ouracademy.ovejafiel.person.domain.Person;
import org.ouracademy.ovejafiel.person.query.find_people_by_email.FindPeopleByEmailQuery;
import org.ouracademy.ovejafiel.person.query.find_people_by_email.FindPeopleByEmailQueryHandler;
import org.ouracademy.ovejafiel.person.query.find_people_by_email.FindPeopleByEmailResult;
import org.ouracademy.ovejafiel.shared.domain.DomainValidationException;
import org.ouracademy.ovejafiel.user.domain.UserId;
import org.ouracademy.ovejafiel.user.query.UserResult;
import org.ouracademy.ovejafiel.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.ouracademy.ovejafiel.user.query.is_person_linked.IsPersonLinkedQueryHandler;
import org.ouracademy.ovejafiel.user.query.is_person_linked.IsPersonLinkedResult;
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
  FindPeopleByEmailQueryHandler findPeopleByEmail = mock(FindPeopleByEmailQueryHandler.class);
  FillPersonEmailCommandHandler fillPersonEmail = mock(FillPersonEmailCommandHandler.class);
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
          findPeopleByEmail,
          fillPersonEmail,
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
    when(entityManager.getReference(eq(org.ouracademy.ovejafiel.user.domain.AppUser.class), any()))
        .thenReturn(InvitationFixtures.user("auth0|me", "pastor@example.org"));
    when(invitationRepository.save(any(Invitation.class))).thenAnswer(call -> call.getArgument(0));
    when(invitationRepository.findByOrganizationIdAndStatusOrderByCreateDateDesc(any(), any()))
        .thenReturn(List.of());
    when(sendEmail.handle(any())).thenReturn(new SendEmailResult(true));
    when(findPeopleByEmail.handle(any())).thenReturn(new FindPeopleByEmailResult(List.of()));
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
  void theNameIsOptionalBecauseWhoAcceptsCanSayIt() {
    var command =
        new CreateInvitationCommand("auth0|me", "beto@example.org", node, null, null, null, " ");

    var result = handler.handle(command);

    assertThat(result.invitation().needsName()).isTrue();
    assertThat(result.invitation().personName()).isNull();
  }

  @Test
  void theOnlyPersonOfTheNodeWithThatEmailIsLinked() {
    var personId = PartyId.generate();
    when(findPeopleByEmail.handle(new FindPeopleByEmailQuery("beto@example.org", node)))
        .thenReturn(new FindPeopleByEmailResult(List.of(personId)));
    when(getPersonAccountabilities.handle(any())).thenReturn(List.of(held(node, null)));
    when(isPersonLinked.handle(any())).thenReturn(new IsPersonLinkedResult(false));
    when(entityManager.getReference(eq(Person.class), eq(personId))).thenReturn(new Person());

    handler.handle(
        new CreateInvitationCommand("auth0|me", " Beto@Example.org", node, null, null, null, null));

    var saved = ArgumentCaptor.forClass(Invitation.class);
    verify(invitationRepository).save(saved.capture());
    assertThat(saved.getValue().getPerson()).isNotNull();
    assertThat(saved.getValue().getFirstName()).isNull();
    // That person already has the email, so nothing is filled in.
    verify(fillPersonEmail, never()).handle(any());
  }

  @Test
  void severalPeopleWithThatEmailAskToChooseOne() {
    when(findPeopleByEmail.handle(any()))
        .thenReturn(new FindPeopleByEmailResult(List.of(PartyId.generate(), PartyId.generate())));

    assertThatThrownBy(() -> handler.handle(newPerson("beto@example.org", null)))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("varias personas");
    verify(invitationRepository, never()).save(any());
  }

  @Test
  void aMatchedPersonWhoAlreadyHasAnAccountIsRefused() {
    var personId = PartyId.generate();
    when(findPeopleByEmail.handle(any()))
        .thenReturn(new FindPeopleByEmailResult(List.of(personId)));
    when(getPersonAccountabilities.handle(any())).thenReturn(List.of(held(node, null)));
    when(isPersonLinked.handle(any())).thenReturn(new IsPersonLinkedResult(true));

    assertThatThrownBy(() -> handler.handle(newPerson("beto@example.org", null)))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("ya usa");
  }

  @Test
  void aNamedPersonWithoutEmailGetsTheInvitedOne() {
    var personId = PartyId.generate();
    var command =
        new CreateInvitationCommand(
            "auth0|me", "beto@example.org", node, null, personId, null, null);
    when(getPersonAccountabilities.handle(any())).thenReturn(List.of(held(node, null)));
    when(isPersonLinked.handle(any())).thenReturn(new IsPersonLinkedResult(false));
    when(entityManager.getReference(eq(Person.class), eq(personId))).thenReturn(new Person());

    handler.handle(command);

    verify(fillPersonEmail).handle(new FillPersonEmailCommand(personId, "beto@example.org"));
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
