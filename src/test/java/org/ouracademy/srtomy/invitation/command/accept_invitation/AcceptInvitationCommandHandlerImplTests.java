package org.ouracademy.srtomy.invitation.command.accept_invitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.IS_PART_OF;
import static org.ouracademy.srtomy.accountability.domain.SystemAccountabilityTypes.VISITOR;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ouracademy.srtomy.accountability.command.create_accountability.CreateAccountabilityCommand;
import org.ouracademy.srtomy.accountability.command.create_accountability.CreateAccountabilityCommandHandler;
import org.ouracademy.srtomy.accountability.domain.AccountabilityType;
import org.ouracademy.srtomy.accountability.domain.AccountabilityTypeId;
import org.ouracademy.srtomy.accountability.query.PositionResult;
import org.ouracademy.srtomy.accountability.query.find_organizations_of_member.FindOrganizationsOfMemberQueryHandler;
import org.ouracademy.srtomy.accountability.query.find_organizations_of_member.OrganizationsOfMemberResult;
import org.ouracademy.srtomy.accountability.query.get_positions_of_person.GetPositionsOfPersonQueryHandler;
import org.ouracademy.srtomy.iam.domain.Permission;
import org.ouracademy.srtomy.iam.domain.PredefinedRoles;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQuery;
import org.ouracademy.srtomy.iam.query.authorize.AuthorizeQueryHandler;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQuery;
import org.ouracademy.srtomy.iam.query.authorize_access_grant.AuthorizeAccessGrantQueryHandler;
import org.ouracademy.srtomy.invitation.InvitationFixtures;
import org.ouracademy.srtomy.invitation.domain.Invitation;
import org.ouracademy.srtomy.invitation.domain.InvitationId;
import org.ouracademy.srtomy.invitation.domain.InvitationStatus;
import org.ouracademy.srtomy.invitation.repository.InvitationRepository;
import org.ouracademy.srtomy.organization.domain.Organization;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationQueryHandler;
import org.ouracademy.srtomy.organization.query.find_church_of_organization.FindChurchOfOrganizationResult;
import org.ouracademy.srtomy.party.domain.PartyId;
import org.ouracademy.srtomy.person.command.create_person.CreatePersonCommand;
import org.ouracademy.srtomy.person.command.create_person.CreatePersonCommandHandler;
import org.ouracademy.srtomy.person.command.create_person.CreatePersonResult;
import org.ouracademy.srtomy.person.domain.Person;
import org.ouracademy.srtomy.shared.domain.DomainValidationException;
import org.ouracademy.srtomy.user.command.link_user_to_person.LinkUserToPersonCommand;
import org.ouracademy.srtomy.user.command.link_user_to_person.LinkUserToPersonCommandHandler;
import org.ouracademy.srtomy.user.domain.AppUser;
import org.ouracademy.srtomy.user.domain.UserId;
import org.ouracademy.srtomy.user.query.UserResult;
import org.ouracademy.srtomy.user.query.find_linked_person.FindLinkedPersonQueryHandler;
import org.ouracademy.srtomy.user.query.find_linked_person.FindLinkedPersonResult;
import org.ouracademy.srtomy.user.query.find_user_by_subject.FindUserBySubjectQueryHandler;
import org.springframework.security.access.AccessDeniedException;

class AcceptInvitationCommandHandlerImplTests {

  InvitationRepository invitationRepository = mock(InvitationRepository.class);
  FindUserBySubjectQueryHandler findUser = mock(FindUserBySubjectQueryHandler.class);
  FindChurchOfOrganizationQueryHandler findChurch =
      mock(FindChurchOfOrganizationQueryHandler.class);
  FindLinkedPersonQueryHandler findLinkedPerson = mock(FindLinkedPersonQueryHandler.class);
  LinkUserToPersonCommandHandler linkUser = mock(LinkUserToPersonCommandHandler.class);
  CreatePersonCommandHandler createPerson = mock(CreatePersonCommandHandler.class);
  CreateAccountabilityCommandHandler createAccountability =
      mock(CreateAccountabilityCommandHandler.class);
  FindOrganizationsOfMemberQueryHandler findNodesOfMember =
      mock(FindOrganizationsOfMemberQueryHandler.class);
  GetPositionsOfPersonQueryHandler getPositions = mock(GetPositionsOfPersonQueryHandler.class);
  AuthorizeQueryHandler authorize = mock(AuthorizeQueryHandler.class);
  AuthorizeAccessGrantQueryHandler authorizeAccessGrant =
      mock(AuthorizeAccessGrantQueryHandler.class);
  EntityManager entityManager = mock(EntityManager.class);
  AcceptInvitationCommandHandlerImpl handler =
      new AcceptInvitationCommandHandlerImpl(
          invitationRepository,
          findUser,
          findChurch,
          findLinkedPerson,
          linkUser,
          createPerson,
          createAccountability,
          findNodesOfMember,
          getPositions,
          authorize,
          authorizeAccessGrant,
          entityManager);

  PartyId church = PartyId.generate();
  PartyId node = PartyId.generate();
  UserId userId = UserId.generate();
  InvitationId invitationId = InvitationId.generate();
  AppUser inviter = InvitationFixtures.user("auth0|pastor", "pastor@example.org");
  Organization nodeEntity = InvitationFixtures.organization(node, "Sede");

  @BeforeEach
  void setUp() {
    when(findUser.handle(any()))
        .thenReturn(Optional.of(new UserResult(userId, "auth0|bob", "bob@example.org", true)));
    when(findChurch.handle(any()))
        .thenReturn(Optional.of(new FindChurchOfOrganizationResult(church)));
    when(findLinkedPerson.handle(any())).thenReturn(Optional.empty());
    when(findNodesOfMember.handle(any()))
        .thenReturn(new OrganizationsOfMemberResult(List.of(node)));
    when(getPositions.handle(any())).thenReturn(List.of());
    when(entityManager.getReference(org.mockito.ArgumentMatchers.eq(AppUser.class), any()))
        .thenReturn(InvitationFixtures.user("auth0|bob", "bob@example.org"));
  }

  Invitation invitation(
      String email, AccountabilityType position, Person person, LocalDateTime expires) {
    var invitation =
        InvitationFixtures.invitation(email, nodeEntity, position, person, inviter, expires);
    when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
    return invitation;
  }

  Invitation pending() {
    return invitation("bob@example.org", null, null, LocalDateTime.now().plusDays(3));
  }

  AccountabilityType position(PartyId id, boolean withAccess) {
    var type = mock(AccountabilityType.class);
    when(type.getId()).thenReturn(AccountabilityTypeId.generate());
    when(type.hasAccess()).thenReturn(withAccess);
    when(type.getRoleIds())
        .thenReturn(withAccess ? Set.of(PredefinedRoles.COLLABORATOR) : Set.of());
    when(type.getPermissions()).thenReturn(Set.of());
    return type;
  }

  AcceptInvitationCommand command() {
    return new AcceptInvitationCommand("auth0|bob", invitationId);
  }

  @Test
  void aNewPersonIsCreatedLinkedAndGivenThePosition() {
    var type = position(church, true);
    var invitation = invitation("Bob@Example.org", type, null, LocalDateTime.now().plusDays(3));
    var personId = PartyId.generate();
    when(createPerson.handle(any())).thenReturn(new CreatePersonResult(personId));

    var result = handler.handle(command());

    assertThat(result).isEqualTo(new AcceptInvitationResult(node, church, personId));
    var created = ArgumentCaptor.forClass(CreatePersonCommand.class);
    verify(createPerson).handle(created.capture());
    assertThat(created.getValue().info().firstName()).isEqualTo("Beto");
    assertThat(created.getValue().info().email()).isEqualTo("Bob@Example.org");
    assertThat(created.getValue().positionIds()).isEmpty();
    assertThat(created.getValue().organizationId()).isEqualTo(node);
    verify(linkUser).handle(new LinkUserToPersonCommand(userId, personId, church));
    verify(createAccountability)
        .handle(new CreateAccountabilityCommand(personId, node, type.getId()));
    assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
  }

  @Test
  void theInvitersAccessIsCheckedAgainWhenTheyInvitedToAPositionWithAccess() {
    var type = position(church, true);
    invitation("bob@example.org", type, null, LocalDateTime.now().plusDays(3));
    when(createPerson.handle(any())).thenReturn(new CreatePersonResult(PartyId.generate()));

    handler.handle(command());

    verify(authorizeAccessGrant)
        .handle(
            new AuthorizeAccessGrantQuery(
                "auth0|pastor",
                Set.of(PredefinedRoles.COLLABORATOR),
                Set.of(),
                node,
                Permission.INVITATIONS_SEND));
  }

  @Test
  void anInviterWhoLostTheirAccessInvalidatesTheInvitation() {
    var type = position(church, true);
    var invitation = invitation("bob@example.org", type, null, LocalDateTime.now().plusDays(3));
    when(authorizeAccessGrant.handle(any())).thenThrow(new AccessDeniedException("no"));

    assertThatThrownBy(() -> handler.handle(command()))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("ya no puede");
    verify(linkUser, never()).handle(any());
    assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
  }

  @Test
  void withoutAPositionTheInviterOnlyNeedsToStillBeAbleToInvite() {
    pending();
    when(createPerson.handle(any())).thenReturn(new CreatePersonResult(PartyId.generate()));

    handler.handle(command());

    verify(authorize).handle(new AuthorizeQuery("auth0|pastor", node, Permission.INVITATIONS_SEND));
    verify(createAccountability, never()).handle(any());
  }

  @Test
  void anExistingPersonIsLinkedWithoutCreatingAnother() {
    var person = new Person();
    var personId = PartyId.generate();
    org.springframework.test.util.ReflectionTestUtils.setField(person, "id", personId);
    invitation("bob@example.org", null, person, LocalDateTime.now().plusDays(3));

    var result = handler.handle(command());

    assertThat(result.personId()).isEqualTo(personId);
    verify(createPerson, never()).handle(any());
    verify(linkUser).handle(new LinkUserToPersonCommand(userId, personId, church));
  }

  @Test
  void aUserWhoAlreadyHasAPersonKeepsItAndJoinsTheNode() {
    var existing = PartyId.generate();
    when(findLinkedPerson.handle(any()))
        .thenReturn(Optional.of(new FindLinkedPersonResult(existing)));
    when(findNodesOfMember.handle(any()))
        .thenReturn(new OrganizationsOfMemberResult(List.of(PartyId.generate())));
    pending();

    var result = handler.handle(command());

    assertThat(result.personId()).isEqualTo(existing);
    verify(createPerson, never()).handle(any());
    verify(linkUser, never()).handle(any());
    verify(createAccountability).handle(new CreateAccountabilityCommand(existing, node, VISITOR));
    verify(createAccountability)
        .handle(new CreateAccountabilityCommand(existing, node, IS_PART_OF));
  }

  @Test
  void aUserWithAnotherPersonCannotAcceptAnInvitationForADifferentOne() {
    var person = new Person();
    org.springframework.test.util.ReflectionTestUtils.setField(person, "id", PartyId.generate());
    invitation("bob@example.org", null, person, LocalDateTime.now().plusDays(3));
    when(findLinkedPerson.handle(any()))
        .thenReturn(Optional.of(new FindLinkedPersonResult(PartyId.generate())));

    assertThatThrownBy(() -> handler.handle(command()))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("otra ficha");
  }

  @Test
  void aPositionAlreadyHeldIsNotGivenTwice() {
    var type = position(church, false);
    invitation("bob@example.org", type, null, LocalDateTime.now().plusDays(3));
    var existing = PartyId.generate();
    when(findLinkedPerson.handle(any()))
        .thenReturn(Optional.of(new FindLinkedPersonResult(existing)));
    var held = new PositionResult(type.getId(), "Ujier", church, Set.of(), Set.of());
    when(getPositions.handle(any())).thenReturn(List.of(held));

    handler.handle(command());

    verify(createAccountability, never()).handle(any());
  }

  @Test
  void anotherEmailIsDenied() {
    pending();
    when(findUser.handle(any()))
        .thenReturn(Optional.of(new UserResult(userId, "auth0|eve", "eve@example.org", true)));

    assertThatThrownBy(() -> handler.handle(command()))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining("bob@example.org");
    verify(linkUser, never()).handle(any());
  }

  @Test
  void anUnverifiedEmailIsDenied() {
    pending();
    when(findUser.handle(any()))
        .thenReturn(Optional.of(new UserResult(userId, "auth0|bob", "bob@example.org", false)));

    assertThatThrownBy(() -> handler.handle(command())).isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void anExpiredInvitationIsRejected() {
    invitation("bob@example.org", null, null, LocalDateTime.now().minusMinutes(1));

    assertThatThrownBy(() -> handler.handle(command()))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("venció");
  }

  @Test
  void anUnknownInvitationIsNotFound() {
    when(invitationRepository.findById(invitationId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(command())).isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void anUnknownUserMustSignInAgain() {
    pending();
    when(findUser.handle(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.handle(command())).isInstanceOf(AccessDeniedException.class);
  }
}
